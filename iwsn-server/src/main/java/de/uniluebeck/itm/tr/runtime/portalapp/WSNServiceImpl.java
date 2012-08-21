/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.AuthorizationRequired;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.runtime.wsnapp.UnknownNodeUrnsException;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNNodeMessageReceiver;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.NetworkUtils;
import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.wsn.*;
import eu.wisebed.wiseml.WiseMLHelper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class WSNServiceImpl extends AbstractService implements WSNService {

	/**
	 * The logger for this WSN service.
	 */
	private static final Logger log = LoggerFactory.getLogger(WSNService.class);

	/**
	 * An implementation of {@link WSNNodeMessageReceiver} that listens for messages coming from sensor nodes and
	 * dispatches them according to their content to either listeners or virtual links.
	 */
	private class WSNNodeMessageReceiverInternal implements WSNNodeMessageReceiver {

		private static final byte MESSAGE_TYPE_PLOT = 105;

		private static final byte MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

		private static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

		private static final byte WISELIB_VIRTUAL_LINK_MESSAGE = 11;

		private DatatypeFactory datatypeFactory;

		private WSNNodeMessageReceiverInternal() {
			try {
				datatypeFactory = DatatypeFactory.newInstance();
			} catch (DatatypeConfigurationException e) {
				log.error("" + e, e);
			}
		}

		@Override
		public void receive(final byte[] bytes, final String sourceNodeUrn, final String timestamp) {

			/* this is a message that was received from a sensor node. we now have to check if this is a virtual link
			 * message. in that case we will deliver it to the destination node if there's a virtual link currently.
			 * if the message is a virtual broadcast we'll deliver it to all destinations this node's connected to.
			 * if the message is not a virtual link we'll deliver it to the controller of the experiment as it is. */

			if (!config.getReservedNodes().contains(sourceNodeUrn)) {
				log.warn("Received message from unreserved node \"{}\".", sourceNodeUrn);
				return;
			}

			// check if message is a virtual link message
			boolean isVirtualLinkMessage = bytes.length > 1 && bytes[0] == MESSAGE_TYPE_PLOT &&
					bytes[1] == NODE_OUTPUT_VIRTUAL_LINK;

			if (!isVirtualLinkMessage) {
				deliverNonVirtualLinkMessageToControllers(bytes, sourceNodeUrn, timestamp);
			} else {
				deliverVirtualLinkMessage(bytes, sourceNodeUrn);
			}
		}

		private void deliverVirtualLinkMessage(final byte[] bytes, final String sourceNodeUrn) {

			Map<String, WSN> recipients = getVirtualLinkMessageRecipients(sourceNodeUrn, readDestinationNodeUrn(bytes));

			if (recipients.size() > 0) {

				final byte[] virtualLinkMessage = constructOutboundVirtualLinkMessage(bytes);

				for (Map.Entry<String, WSN> recipient : recipients.entrySet()) {

					String targetNodeUrn = recipient.getKey();
					WSN recipientEndpointProxy = recipient.getValue();

					executorService.execute(new DeliverVirtualLinkMessageRunnable(
							sourceNodeUrn,
							targetNodeUrn,
							recipientEndpointProxy,
							virtualLinkMessage
					)
					);
				}
			}
		}

		private byte[] constructOutboundVirtualLinkMessage(final byte[] bytes) {

			// byte 0: ISense Packet Type
			// byte 1: Node API Command Type
			// byte 2: RSSI
			// byte 3: LQI
			// byte 4: Payload Length
			// byte 5-8: Destination Node URN
			// byte 9-12: Source Node URN
			// byte 13-13+Payload Length: Payload

			// construct message that is actually sent to the destination node URN
			ChannelBuffer header = ChannelBuffers.buffer(3);
			header.writeByte(MESSAGE_TYPE_WISELIB_DOWNSTREAM);
			header.writeByte(WISELIB_VIRTUAL_LINK_MESSAGE);
			header.writeByte(0); // request id according to Node API
			ChannelBuffer payload = ChannelBuffers.wrappedBuffer(bytes, 2, bytes.length - 2);
			ChannelBuffer packet = ChannelBuffers.wrappedBuffer(header, payload);

			byte[] outboundVirtualLinkMessageBinaryData = new byte[packet.readableBytes()];
			packet.getBytes(0, outboundVirtualLinkMessageBinaryData);

			return outboundVirtualLinkMessageBinaryData;
		}

		private Map<String, WSN> getVirtualLinkMessageRecipients(final String sourceNodeUrn,
																 final long destinationNodeMac) {

			// check if message is a broadcast or unicast message
			boolean isBroadcast = destinationNodeMac == 0xFFFF;

			// send virtual link message to all recipients
			Map<String, WSN> recipients = new HashMap<String, WSN>();

			if (isBroadcast) {

				ImmutableMap<String, WSN> map = virtualLinksMap.get(sourceNodeUrn);
				if (map != null) {
					for (Map.Entry<String, WSN> entry : map.entrySet()) {
						recipients.put(entry.getKey(), entry.getValue());
					}
				}

			} else {

				ImmutableMap<String, WSN> map = virtualLinksMap.get(sourceNodeUrn);
				for (String targetNodeUrn : map.keySet()) {

					if (StringUtils.parseHexOrDecLongFromUrn(targetNodeUrn) == destinationNodeMac) {
						recipients.put(targetNodeUrn, map.get(targetNodeUrn));
					}
				}
			}

			return recipients;
		}

		private long readDestinationNodeUrn(final byte[] virtualLinkMessage) {
			ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(virtualLinkMessage);
			return buffer.getLong(5);
		}

		private void deliverNonVirtualLinkMessageToControllers(final byte[] bytes, final String sourceNodeUrn,
															   final String timestamp) {

			XMLGregorianCalendar xmlTimestamp = datatypeFactory.newXMLGregorianCalendar(timestamp);

			Message message = new Message();
			message.setSourceNodeUrn(sourceNodeUrn);
			message.setTimestamp(xmlTimestamp);
			message.setBinaryData(bytes);

			deliveryManager.receive(message);
		}

		@Override
		public void receiveNotification(final WSNAppMessages.Notification notification) {
			deliveryManager.receiveNotification(Lists.newArrayList(notification.getMessage()));
		}
	}

	/**
	 * A runnable that delivers virtual link messages to the intended testbed.
	 */
	private class DeliverVirtualLinkMessageRunnable implements Runnable {

		private final String sourceNodeUrn;

		private final String targetNodeUrn;

		private final WSN recipient;

		private final byte[] message;

		private int tries = 0;

		public DeliverVirtualLinkMessageRunnable(final String sourceNodeUrn,
												 final String targetNodeUrn,
												 final WSN recipient,
												 final byte[] message) {
			this.sourceNodeUrn = sourceNodeUrn;
			this.targetNodeUrn = targetNodeUrn;
			this.recipient = recipient;
			this.message = message;
		}

		@Override
		public void run() {

			if (tries < 3) {

				tries++;

				log.debug("Delivering virtual link message to remote testbed service.");

				try {

					recipient.send(requestIdGenerator.nextLong(), Arrays.asList(targetNodeUrn), message);

				} catch (Exception e) {

					if (tries >= 3) {

						log.warn("Repeatedly couldn't deliver virtual link message. Destroy virtual link.");
						destroyVirtualLink(requestIdGenerator.nextLong(), sourceNodeUrn, targetNodeUrn);

					} else {
						log.warn("Error while delivering virtual link message to remote testbed service. "
								+ "Trying again in 5 seconds."
						);
						executorService.schedule(this, 5, TimeUnit.SECONDS);
					}
				}
			}
		}
	}

	/**
	 * Used to generate secure non-predictable secure request IDs as used request-response matching identifier.
	 */
	private final Random requestIdGenerator = new Random();

	/**
	 * The WSNApp instance associated with this WSN service instance. Does all the testbed internal work around
	 * communicating with the nodes.
	 */
	private WSNApp wsnApp;

	/**
	 * Used for executing all parallel jobs.
	 */
	private ScheduledExecutorService executorService;

	/**
	 * Used to check method preconditions upon invocation of this WSN services public Web Service APIs methods.
	 */
	private WSNPreconditions preconditions;

	/**
	 * Used to manage controllers and send them messages and request statuses.
	 */
	private DeliveryManager deliveryManager;

	/**
	 * The configuration for this service.
	 */
	private WSNServiceConfig config;

	private WSNNodeMessageReceiverInternal nodeMessageReceiver = new WSNNodeMessageReceiverInternal();

	@Inject
	public WSNServiceImpl(
			@Assisted final WSNServiceConfig config,
			@Assisted final DeliveryManager deliveryManager,
			@Assisted final WSNPreconditions preconditions,
			@Assisted final WSNApp wsnApp) {

		checkNotNull(config);
		checkNotNull(deliveryManager);
		checkNotNull(preconditions);
		checkNotNull(wsnApp);

		this.config = config;
		this.deliveryManager = deliveryManager;
		this.preconditions = preconditions;
		this.wsnApp = wsnApp;
	}

	@Override
	protected void doStart() {

		try {

			log.info("Starting WSN service...");

			executorService = Executors.newSingleThreadScheduledExecutor(
					new ThreadFactoryBuilder().setNameFormat("WSNService-Thread %d").build()
			);

			wsnApp.addNodeMessageReceiver(nodeMessageReceiver);

			deliveryManager.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		try {

			log.info("Stopping WSN service...");

			wsnApp.removeNodeMessageReceiver(nodeMessageReceiver);

			deliveryManager.reservationEnded();
			deliveryManager.stopAndWait();

			ExecutorUtils.shutdown(executorService, 5, TimeUnit.SECONDS);

			log.info("Stopped WSN service!");

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	@Override
	public String getVersion() {
		return "3.0";
	}

	@Override
	public void addController(final String controllerEndpointUrl) {

		log.debug("WSNServiceImpl.addController({})", controllerEndpointUrl);

		if (!"NONE".equals(controllerEndpointUrl)) {
			NetworkUtils.checkConnectivity(controllerEndpointUrl);
		}

		deliveryManager.addController(controllerEndpointUrl);
	}

	@Override
	public void removeController(String controllerEndpointUrl) {

		log.debug("WSNServiceImpl.removeController({})", controllerEndpointUrl);

		deliveryManager.removeController(controllerEndpointUrl);
	}

	@Override
	@AuthorizationRequired("WSN_SEND")
	public void send(final long requestId, final List<String> nodeUrns, final byte[] message) {

		preconditions.checkSendArguments(nodeUrns, message);

		log.debug("WSNServiceImpl.send({},{})", nodeUrns, message);

		try {

			wsnApp.send(
					new HashSet<String>(nodeUrns),
					message,
					new WSNApp.Callback() {

						private final long start = System.currentTimeMillis();

						@Override
						public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

							if (log.isDebugEnabled()) {

								final long duration = System.currentTimeMillis() - start;
								final String nodeUrn = requestStatus.getStatus().getNodeId();

								log.debug("Received reply from {} after {} ms.", nodeUrn, duration);
							}

							deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
						}

						@Override
						public void failure(Exception e) {
							deliveryManager.receiveFailureStatusMessages(nodeUrns, requestId, e, -1);
						}
					}
			);

		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}
	}

	@Override
	@AuthorizationRequired("WSN_SET_CHANNEL_PIPELINE")
	public void setChannelPipeline(final long requestId,
								   final List<String> nodeUrn,
								   final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		preconditions.checkSetChannelPipelineArguments(nodeUrn, channelHandlerConfigurations);

		log.debug("WSNServiceImpl.setChannelPipeline({}, {})", nodeUrn, channelHandlerConfigurations);

		final long start = System.currentTimeMillis();

		try {
			wsnApp.setChannelPipeline(new HashSet<String>(nodeUrn), channelHandlerConfigurations,
					new WSNApp.Callback() {

						@Override
						public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
							long end = System.currentTimeMillis();
							log.debug("Received reply after {} ms.", (end - start));
							deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
						}

						@Override
						public void failure(final Exception e) {
							deliveryManager.receiveFailureStatusMessages(nodeUrn, requestId, e, -1);
						}
					}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}
	}

	@Override
	@AuthorizationRequired("WSN_ARE_NODES_ALIVE")
	public void areNodesAlive(final long requestId, final List<String> nodeUrns) {

		preconditions.checkAreNodesAliveArguments(nodeUrns);

		log.debug("WSNServiceImpl.checkAreNodesAlive({})", nodeUrns);

		try {
			wsnApp.areNodesAlive(new HashSet<String>(nodeUrns), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(nodeUrns, requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}
	}

	@Override
	public List<ChannelPipelinesMap> getChannelPipelines(final List<String> strings) {
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	public String getNetwork() {
		log.debug("WSNServiceImpl.getNetwork()");
		return WiseMLHelper.serialize(config.getWiseML());
	}

	@Override
	@AuthorizationRequired("WSN_RESET_NODES")
	public void resetNodes(final long requestId, final List<String> nodeUrns) {

		preconditions.checkResetNodesArguments(nodeUrns);

		log.debug("WSNServiceImpl.resetNodes({})", nodeUrns);

		try {
			wsnApp.resetNodes(new HashSet<String>(nodeUrns), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(nodeUrns, requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}
	}

	/**
	 * Map: (Source Node URN) -> (Map: (Target Node URN) -> (WSN endpoint instance))
	 */
	private ImmutableMap<String, ImmutableMap<String, WSN>> virtualLinksMap = ImmutableMap.of();

	@Override
	@AuthorizationRequired("WSN_SET_VIRTUAL_LINK")
	public void setVirtualLink(final long requestId,
							   final String sourceNodeUrn,
							   final String targetNodeUrn,
							   final String remoteServiceInstance,
							   final List<String> parameters,
							   final List<String> filters) {

		preconditions.checkSetVirtualLinkArguments(
				sourceNodeUrn,
				targetNodeUrn,
				remoteServiceInstance,
				parameters,
				filters
		);

		try {
			wsnApp.setVirtualLink(sourceNodeUrn, targetNodeUrn, new WSNApp.Callback() {

				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));

					if (requestStatus.getStatus().getValue() == 1) {
						addVirtualLink(sourceNodeUrn, targetNodeUrn, remoteServiceInstance);
					}

				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(sourceNodeUrn), requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}
	}

	private void addVirtualLink(String sourceNodeUrn, String targetNodeUrn, String remoteServiceInstance) {

		if (!containsVirtualLink(sourceNodeUrn, targetNodeUrn)) {

			log.debug("+++ Adding virtual link from {} to {}", sourceNodeUrn, targetNodeUrn);

			WSN remoteServiceEndpoint = WisebedServiceHelper.getWSNService(remoteServiceInstance);

			//Create a new immutable map with this sourceNodeUrn and all existing <targetNodeUrn, WSN> mappings
			ImmutableMap.Builder<String, WSN> targetNodeMapBuilder = ImmutableMap.builder();

			//Add potentially existing <targetNodeUrn, WSN> mappings for this source node to the new list
			if (virtualLinksMap.get(sourceNodeUrn) != null) {
				targetNodeMapBuilder.putAll(virtualLinksMap.get(sourceNodeUrn));
			}
			//Add the new <targetNodeUrn, WSN> mapping to this new list
			targetNodeMapBuilder.put(targetNodeUrn, remoteServiceEndpoint);

			ImmutableMap.Builder<String, ImmutableMap<String, WSN>> virtualLinksMapBuilder = ImmutableMap.builder();

			//We now add all existing source nodes to the map except for the current source node
			//It looks a bit strange but we cannot use putAll and then overwrite an existing key
			//because the ImmutableMapBuilder forbids duplicate keys
			for (String existingSourceNode : virtualLinksMap.keySet()) {
				if (!existingSourceNode.equals(sourceNodeUrn)) {
					virtualLinksMapBuilder.put(existingSourceNode, virtualLinksMap.get(existingSourceNode));
				}
			}

			virtualLinksMapBuilder.put(sourceNodeUrn, targetNodeMapBuilder.build());
			virtualLinksMap = virtualLinksMapBuilder.build();

		} else {
			log.debug("+++ Not adding virtual link from {} to {} as it is already established", sourceNodeUrn,
					targetNodeUrn
			);
		}

	}

	private void removeVirtualLink(final String sourceNodeUrn, final String targetNodeUrn) {

		if (containsVirtualLink(sourceNodeUrn, targetNodeUrn)) {

			log.debug("--- Removing virtual link from {} to {}", sourceNodeUrn, targetNodeUrn);

			ImmutableMap.Builder<String, WSN> targetNodeMapBuilder = ImmutableMap.builder();
			for (Map.Entry<String, WSN> oldEntry : virtualLinksMap.get(sourceNodeUrn).entrySet()) {
				if (!targetNodeUrn.equals(oldEntry.getKey())) {
					targetNodeMapBuilder.put(oldEntry.getKey(), oldEntry.getValue());
				}
			}

			ImmutableMap.Builder<String, ImmutableMap<String, WSN>> virtualLinksMapBuilder = ImmutableMap.builder();

			for (String existingSourceNode : virtualLinksMap.keySet()) {
				if (!existingSourceNode.equals(sourceNodeUrn)) {
					virtualLinksMapBuilder.put(existingSourceNode, virtualLinksMap.get(existingSourceNode));
				}
			}

			virtualLinksMapBuilder.put(sourceNodeUrn, targetNodeMapBuilder.build());

			virtualLinksMap = virtualLinksMapBuilder.build();

		}

	}

	private boolean containsVirtualLink(String sourceNodeUrn, String targetNodeUrn) {
		ImmutableMap<String, WSN> map = virtualLinksMap.get(sourceNodeUrn);
		return map != null && map.containsKey(targetNodeUrn);
	}

	@Override
	@AuthorizationRequired("WSN_DESTROY_VIRTUAL_LINK")
	public void destroyVirtualLink(final long requestId,
								   final String sourceNodeUrn,
								   final String targetNodeUrn) {

		preconditions.checkDestroyVirtualLinkArguments(sourceNodeUrn, targetNodeUrn);

		try {
			wsnApp.destroyVirtualLink(sourceNodeUrn, targetNodeUrn, new WSNApp.Callback() {

				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));

					if (requestStatus.getStatus().getValue() == 1) {
						removeVirtualLink(sourceNodeUrn, targetNodeUrn);
					}

				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(sourceNodeUrn), requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}
	}

	@Override
	@AuthorizationRequired("WSN_DISABLE_NODE")
	public void disableNode(final long requestId, final String nodeUrn) {

		preconditions.checkDisableNodeArguments(nodeUrn);

		log.debug("WSNServiceImpl.disableNode");

		try {

			wsnApp.disableNode(nodeUrn, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(nodeUrn), requestId, e, -1);
				}
			}
			);

		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}
	}

	@Override
	@AuthorizationRequired("WSN_DISABLE_PHYSICAL_LINK")
	public void disablePhysicalLink(final long requestId, final String sourceNodeUrn, final String targetNodeUrn) {

		preconditions.checkDisablePhysicalLinkArguments(sourceNodeUrn, targetNodeUrn);

		log.debug("WSNServiceImpl.disablePhysicalLink");

		try {

			wsnApp.disablePhysicalLink(sourceNodeUrn, targetNodeUrn, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(sourceNodeUrn), requestId, e, -1);
				}
			}
			);

		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}
	}

	@Override
	public void disableVirtualization() throws VirtualizationNotSupported_Exception {
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	public void enableVirtualization() throws VirtualizationNotSupported_Exception {
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	@AuthorizationRequired("WSN_ENABLE_NODE")
	public void enableNode(final long requestId, final String nodeUrn) {

		preconditions.checkEnableNodeArguments(nodeUrn);

		log.debug("WSNServiceImpl.enableNode");

		try {

			wsnApp.enableNode(nodeUrn, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(nodeUrn), requestId, e, -1);
				}
			}
			);

		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}
	}

	@Override
	@AuthorizationRequired("WSN_ENABLE_PHYSICAL_LINK")
	public void enablePhysicalLink(final long requestId, final String sourceNodeUrn, final String targetNodeUrn) {

		preconditions.checkEnablePhysicalLinkArguments(sourceNodeUrn, targetNodeUrn);

		log.debug("WSNServiceImpl.enablePhysicalLink");

		try {

			wsnApp.enablePhysicalLink(sourceNodeUrn, targetNodeUrn, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(sourceNodeUrn), requestId, e, -1);
				}
			}
			);

		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}
	}

	@Override
	public void flashPrograms(final long requestId, final List<FlashProgramsConfiguration> configurations) {

		log.debug("WSNServiceImpl.flashPrograms({})", configurations);

		preconditions.checkFlashProgramsArguments(configurations);

		try {

			final Map<String, byte[]> map = newHashMap();

			for (FlashProgramsConfiguration configuration : configurations) {
				for (String nodeUrn : configuration.getNodeUrns()) {
					map.put(nodeUrn, configuration.getProgram());
				}
			}

			wsnApp.flashPrograms(map, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

					if (log.isDebugEnabled()) {

						boolean hasInformation = requestStatus.hasStatus() &&
								requestStatus.getStatus().hasValue() &&
								requestStatus.getStatus().hasNodeId();

						if (hasInformation && requestStatus.getStatus().getValue() >= 0) {
							log.debug(
									"Flashing node {} completed {} percent.",
									requestStatus.getStatus().getNodeId(),
									requestStatus.getStatus().getValue()
							);
						} else if (hasInformation && requestStatus.getStatus().getValue() < 0) {
							log.warn(
									"Failed flashing node {} ({})!",
									requestStatus.getStatus().getNodeId(),
									requestStatus.getStatus().getValue()
							);
						}
					}

					// deliver output to client
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(newArrayList(map.keySet()), requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}
	}
}
