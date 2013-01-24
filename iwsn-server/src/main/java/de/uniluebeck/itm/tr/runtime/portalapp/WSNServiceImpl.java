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
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.wsn.*;
import eu.wisebed.wiseml.WiseMLHelper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter.convert;

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

		@Override
		public void receive(final byte[] bytes, final NodeUrn sourceNodeUrn, final DateTime timestamp) {

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

		private void deliverVirtualLinkMessage(final byte[] bytes, final NodeUrn sourceNodeUrn) {

			Map<NodeUrn, WSN> recipients =
					getVirtualLinkMessageRecipients(sourceNodeUrn, readDestinationNodeUrn(bytes));

			if (recipients.size() > 0) {

				final byte[] virtualLinkMessage = constructOutboundVirtualLinkMessage(bytes);

				for (Map.Entry<NodeUrn, WSN> recipient : recipients.entrySet()) {

					NodeUrn targetNodeUrn = recipient.getKey();
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

		private Map<NodeUrn, WSN> getVirtualLinkMessageRecipients(final NodeUrn sourceNodeUrn,
																  final long destinationNodeMac) {

			// check if message is a broadcast or unicast message
			boolean isBroadcast = destinationNodeMac == 0xFFFF;

			// send virtual link message to all recipients
			Map<NodeUrn, WSN> recipients = new HashMap<NodeUrn, WSN>();

			if (isBroadcast) {

				ImmutableMap<NodeUrn, WSN> map = virtualLinksMap.get(sourceNodeUrn);
				if (map != null) {
					for (Map.Entry<NodeUrn, WSN> entry : map.entrySet()) {
						recipients.put(entry.getKey(), entry.getValue());
					}
				}

			} else {

				ImmutableMap<NodeUrn, WSN> map = virtualLinksMap.get(sourceNodeUrn);
				for (NodeUrn targetNodeUrn : map.keySet()) {

					if (StringUtils.parseHexOrDecLongFromUrn(targetNodeUrn.getSuffix()) == destinationNodeMac) {
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

		private void deliverNonVirtualLinkMessageToControllers(final byte[] bytes,
															   final NodeUrn sourceNodeUrn,
															   final DateTime timestamp) {

			final Message message = new Message();

			message.setSourceNodeUrn(sourceNodeUrn);
			message.setTimestamp(timestamp);
			message.setBinaryData(bytes);

			deliveryManager.receive(message);
		}

		@Override
		public void receiveNotification(final WSNAppMessages.Notification notification) {
			deliveryManager.receiveNotification(convert(notification));
		}

		private Notification convert(final WSNAppMessages.Notification notification) {
			final Notification wsNotification = new Notification();
			wsNotification.setMsg(notification.getMsg());
			wsNotification.setNodeUrn(new NodeUrn(notification.getNodeUrn()));
			wsNotification.setTimestamp(ISODateTimeFormat.dateTimeParser().parseDateTime(notification.getTimestamp()));
			return wsNotification;
		}
	}

	/**
	 * A runnable that delivers virtual link messages to the intended testbed.
	 */
	private class DeliverVirtualLinkMessageRunnable implements Runnable {

		private final NodeUrn sourceNodeUrn;

		private final NodeUrn targetNodeUrn;

		private final WSN recipient;

		private final byte[] message;

		private int tries = 0;

		public DeliverVirtualLinkMessageRunnable(final NodeUrn sourceNodeUrn,
												 final NodeUrn targetNodeUrn,
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

						Link link = new Link();
						link.setSourceNodeUrn(sourceNodeUrn);
						link.setTargetNodeUrn(targetNodeUrn);

						destroyVirtualLinks(requestIdGenerator.nextLong(), newArrayList(link));

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
	public void send(final long requestId, final List<NodeUrn> nodeUrns, final byte[] message) {

		preconditions.checkSendArguments(nodeUrns, message);

		log.debug("WSNServiceImpl.send({},{})", nodeUrns, message);

		try {

			wsnApp.send(
					new HashSet<NodeUrn>(nodeUrns),
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

							deliveryManager.receiveStatus(convert(requestStatus, requestId));
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
								   final List<NodeUrn> nodeUrn,
								   final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		preconditions.checkSetChannelPipelineArguments(nodeUrn, channelHandlerConfigurations);

		log.debug("WSNServiceImpl.setChannelPipeline({}, {})", nodeUrn, channelHandlerConfigurations);

		final long start = System.currentTimeMillis();

		try {
			wsnApp.setChannelPipeline(new HashSet<NodeUrn>(nodeUrn), channelHandlerConfigurations,
					new WSNApp.Callback() {

						@Override
						public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
							long end = System.currentTimeMillis();
							log.debug("Received reply after {} ms.", (end - start));
							deliveryManager.receiveStatus(convert(requestStatus, requestId));
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
	public void setSerialPortParameters(final List<NodeUrn> nodeUrns, final SerialPortParameters parameters) {
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	@AuthorizationRequired("WSN_SET_VIRTUAL_LINK")
	public void setVirtualLinks(final long requestId, final List<VirtualLink> links) {

		log.debug("WSNServiceImpl.setVirtualLinks({}, {})", requestId, links);

		preconditions.checkSetVirtualLinkArguments(links);

		for (VirtualLink link : links) {

			try {

				final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
				final NodeUrn targetNodeUrn = link.getTargetNodeUrn();
				final String remoteWSNServiceEndpointUrl = link.getRemoteWSNServiceEndpointUrl();

				wsnApp.setVirtualLink(sourceNodeUrn, targetNodeUrn, new WSNApp.Callback() {

					@Override
					public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

						deliveryManager.receiveStatus(convert(requestStatus, requestId));

						if (requestStatus.getStatus().getValue() == 1) {
							addVirtualLink(sourceNodeUrn, targetNodeUrn, remoteWSNServiceEndpointUrl);
						}
					}

					@Override
					public void failure(Exception e) {
						deliveryManager.receiveFailureStatusMessages(newArrayList(sourceNodeUrn), requestId, e, -1);
					}
				}
				);
			} catch (UnknownNodeUrnsException e) {
				deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
			}
		}
	}

	@Override
	@AuthorizationRequired("WSN_ARE_NODES_ALIVE")
	public void areNodesAlive(final long requestId, final List<NodeUrn> nodeUrns) {

		preconditions.checkAreNodesAliveArguments(nodeUrns);

		log.debug("WSNServiceImpl.checkAreNodesAlive({})", nodeUrns);

		try {
			wsnApp.areNodesAlive(newHashSet(nodeUrns), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(convert(requestStatus, requestId));
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
	@AuthorizationRequired("WSN_DESTROY_VIRTUAL_LINK")
	public void destroyVirtualLinks(final long requestId, final List<Link> links) {

		log.debug("WSNServiceImpl.destroyVirtualLinks({}, {})", requestId, links);

		preconditions.checkDestroyVirtualLinkArguments(links);

		for (Link link : links) {

			final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
			final NodeUrn targetNodeUrn = link.getTargetNodeUrn();

			try {

				wsnApp.destroyVirtualLink(sourceNodeUrn, targetNodeUrn, new WSNApp.Callback() {

					@Override
					public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

						deliveryManager.receiveStatus(convert(requestStatus, requestId));

						if (requestStatus.getStatus().getValue() == 1) {
							removeVirtualLink(sourceNodeUrn, targetNodeUrn);
						}
					}

					@Override
					public void failure(Exception e) {
						deliveryManager.receiveFailureStatusMessages(
								Lists.newArrayList(sourceNodeUrn),
								requestId,
								e,
								-1
						);
					}
				}
				);
			} catch (UnknownNodeUrnsException e) {
				deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
			}
		}
	}

	@Override
	@AuthorizationRequired("WSN_DISABLE_NODE")
	public void disableNodes(final long requestId, final List<NodeUrn> nodeUrns) {

		log.debug("WSNServiceImpl.disableNodes({}, {})", requestId, nodeUrns);

		preconditions.checkDisableNodeArguments(nodeUrns);

		for (final NodeUrn nodeUrn : nodeUrns) {

			try {

				wsnApp.disableNode(nodeUrn, new WSNApp.Callback() {
					@Override
					public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
						deliveryManager.receiveStatus(convert(requestStatus, requestId));
					}

					@Override
					public void failure(final Exception e) {
						deliveryManager.receiveFailureStatusMessages(newArrayList(nodeUrn), requestId, e, -1);
					}
				}
				);

			} catch (UnknownNodeUrnsException e) {
				deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
			}
		}
	}

	@Override
	@AuthorizationRequired("WSN_DISABLE_PHYSICAL_LINK")
	public void disablePhysicalLinks(final long requestId, final List<Link> links) {

		log.debug("WSNServiceImpl.disablePhysicalLinks({}, {})", requestId, links);

		preconditions.checkDisablePhysicalLinkArguments(links);

		for (Link link : links) {

			final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
			final NodeUrn targetNodeUrn = link.getTargetNodeUrn();

			try {

				wsnApp.disablePhysicalLink(sourceNodeUrn, targetNodeUrn, new WSNApp.Callback() {
					@Override
					public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
						deliveryManager.receiveStatus(convert(requestStatus, requestId));
					}

					@Override
					public void failure(final Exception e) {
						deliveryManager.receiveFailureStatusMessages(newArrayList(sourceNodeUrn), requestId, e, -1);
					}
				}
				);

			} catch (UnknownNodeUrnsException e) {
				deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
			}
		}
	}

	@Override
	public List<ChannelPipelinesMap> getChannelPipelines(final List<NodeUrn> nodeUrns) {
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	public String getNetwork() {
		log.debug("WSNServiceImpl.getNetwork()");
		return WiseMLHelper.serialize(config.getWiseML());
	}

	@Override
	@AuthorizationRequired("WSN_RESET_NODES")
	public void resetNodes(final long requestId, final List<NodeUrn> nodeUrns) {

		preconditions.checkResetNodesArguments(nodeUrns);

		log.debug("WSNServiceImpl.resetNodes({})", nodeUrns);

		try {
			wsnApp.resetNodes(newHashSet(nodeUrns), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(convert(requestStatus, requestId));
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
	private ImmutableMap<NodeUrn, ImmutableMap<NodeUrn, WSN>> virtualLinksMap = ImmutableMap.of();

	private void addVirtualLink(NodeUrn sourceNodeUrn, NodeUrn targetNodeUrn, String remoteServiceInstance) {

		if (!containsVirtualLink(sourceNodeUrn, targetNodeUrn)) {

			log.debug("+++ Adding virtual link from {} to {}", sourceNodeUrn, targetNodeUrn);

			WSN remoteServiceEndpoint = WisebedServiceHelper.getWSNService(remoteServiceInstance);

			//Create a new immutable map with this sourceNodeUrn and all existing <targetNodeUrn, WSN> mappings
			ImmutableMap.Builder<NodeUrn, WSN> targetNodeMapBuilder = ImmutableMap.builder();

			//Add potentially existing <targetNodeUrn, WSN> mappings for this source node to the new list
			if (virtualLinksMap.get(sourceNodeUrn) != null) {
				targetNodeMapBuilder.putAll(virtualLinksMap.get(sourceNodeUrn));
			}
			//Add the new <targetNodeUrn, WSN> mapping to this new list
			targetNodeMapBuilder.put(targetNodeUrn, remoteServiceEndpoint);

			ImmutableMap.Builder<NodeUrn, ImmutableMap<NodeUrn, WSN>> virtualLinksMapBuilder = ImmutableMap.builder();

			//We now add all existing source nodes to the map except for the current source node
			//It looks a bit strange but we cannot use putAll and then overwrite an existing key
			//because the ImmutableMapBuilder forbids duplicate keys
			for (NodeUrn existingSourceNode : virtualLinksMap.keySet()) {
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

	private void removeVirtualLink(final NodeUrn sourceNodeUrn, final NodeUrn targetNodeUrn) {

		if (containsVirtualLink(sourceNodeUrn, targetNodeUrn)) {

			log.debug("--- Removing virtual link from {} to {}", sourceNodeUrn, targetNodeUrn);

			ImmutableMap.Builder<NodeUrn, WSN> targetNodeMapBuilder = ImmutableMap.builder();
			for (Map.Entry<NodeUrn, WSN> oldEntry : virtualLinksMap.get(sourceNodeUrn).entrySet()) {
				if (!targetNodeUrn.equals(oldEntry.getKey())) {
					targetNodeMapBuilder.put(oldEntry.getKey(), oldEntry.getValue());
				}
			}

			ImmutableMap.Builder<NodeUrn, ImmutableMap<NodeUrn, WSN>> virtualLinksMapBuilder = ImmutableMap.builder();

			for (NodeUrn existingSourceNode : virtualLinksMap.keySet()) {
				if (!existingSourceNode.equals(sourceNodeUrn)) {
					virtualLinksMapBuilder.put(existingSourceNode, virtualLinksMap.get(existingSourceNode));
				}
			}

			virtualLinksMapBuilder.put(sourceNodeUrn, targetNodeMapBuilder.build());

			virtualLinksMap = virtualLinksMapBuilder.build();

		}

	}

	private boolean containsVirtualLink(NodeUrn sourceNodeUrn, NodeUrn targetNodeUrn) {
		ImmutableMap<NodeUrn, WSN> map = virtualLinksMap.get(sourceNodeUrn);
		return map != null && map.containsKey(targetNodeUrn);
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
	public void enableNodes(final long requestId, final List<NodeUrn> nodeUrns) {

		log.debug("WSNServiceImpl.enableNodes({}, {})", requestId, nodeUrns);

		preconditions.checkEnableNodeArguments(nodeUrns);

		for (final NodeUrn nodeUrn : nodeUrns) {

			try {

				wsnApp.enableNode(nodeUrn, new WSNApp.Callback() {
					@Override
					public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
						deliveryManager.receiveStatus(convert(requestStatus, requestId));
					}

					@Override
					public void failure(final Exception e) {
						deliveryManager.receiveFailureStatusMessages(newArrayList(nodeUrn), requestId, e, -1);
					}
				}
				);

			} catch (UnknownNodeUrnsException e) {
				deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
			}
		}
	}

	@Override
	@AuthorizationRequired("WSN_ENABLE_PHYSICAL_LINK")
	public void enablePhysicalLinks(final long requestId, final List<Link> links) {

		log.debug("WSNServiceImpl.enablePhysicalLinks({}, {})", requestId, links);

		preconditions.checkEnablePhysicalLinkArguments(links);

		for (Link link : links) {

			try {

				final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
				final NodeUrn targetNodeUrn = link.getTargetNodeUrn();

				wsnApp.enablePhysicalLink(sourceNodeUrn, targetNodeUrn, new WSNApp.Callback() {
					@Override
					public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
						deliveryManager.receiveStatus(convert(requestStatus, requestId));
					}

					@Override
					public void failure(final Exception e) {
						deliveryManager.receiveFailureStatusMessages(newArrayList(sourceNodeUrn), requestId, e, -1);
					}
				}
				);

			} catch (UnknownNodeUrnsException e) {
				deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
			}
		}
	}

	@Override
	public void flashPrograms(final long requestId, final List<FlashProgramsConfiguration> configurations) {

		log.debug("WSNServiceImpl.flashPrograms({})", configurations);

		preconditions.checkFlashProgramsArguments(configurations);

		try {

			final Map<NodeUrn, byte[]> map = newHashMap();

			for (FlashProgramsConfiguration configuration : configurations) {
				for (NodeUrn nodeUrn : configuration.getNodeUrns()) {
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
					deliveryManager.receiveStatus(convert(requestStatus, requestId));
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
