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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.netty.handlerstack.protocolcollection.ProtocolCollection;
import de.uniluebeck.itm.tr.runtime.wsnapp.UnknownNodeUrnsException;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNNodeMessageReceiver;
import de.uniluebeck.itm.tr.util.*;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.ns.wiseml._1.Wiseml;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.WSNPreconditions;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.deliverymanager.DeliveryManager;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Endpoint;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

@WebService(
		serviceName = "WSNService",
		targetNamespace = Constants.NAMESPACE_WSN_SERVICE,
		portName = "WSNPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_WSN_SERVICE
)
public class WSNServiceImpl implements WSNService {

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
		public void receive(final byte[] bytes, final String sourceNodeId, final String timestamp) {

			/* this is a message that was received from a sensor node. we now have to check if this is a virtual link
			 * message. in that case we will deliver it to the destination node if there's a virtual link currently.
			 * if the message is a virtual broadcast we'll deliver it to all destinations this node's connected to.
			 * if the message is not a virtual link we'll deliver it to the controller of the experiment as it is. */

			if (!reservedNodes.contains(sourceNodeId)) {
				log.warn("Received message from unreserved node \"{}\".", sourceNodeId);
				return;
			}

			// check if message is a virtual link message
			boolean isVirtualLinkMessage = bytes.length > 1 && bytes[0] == MESSAGE_TYPE_PLOT &&
					bytes[1] == NODE_OUTPUT_VIRTUAL_LINK;

			if (!isVirtualLinkMessage) {
				deliverNonVirtualLinkMessageToControllers(bytes, sourceNodeId, timestamp);
			} else {
				deliverVirtualLinkMessage(bytes, sourceNodeId, timestamp);
			}
		}

		private void deliverVirtualLinkMessage(final byte[] bytes, final String sourceNodeId, final String timestamp) {

			long destinationNode = readDestinationNodeURN(bytes);
			Map<String, WSN> recipients = determineVirtualLinkMessageRecipients(sourceNodeId, destinationNode);

			if (recipients.size() > 0) {

				Message outboundVirtualLinkMessage =
						constructOutboundVirtualLinkMessage(bytes, sourceNodeId, timestamp);

				for (Map.Entry<String, WSN> recipient : recipients.entrySet()) {

					String targetNode = recipient.getKey();
					WSN recipientEndpointProxy = recipient.getValue();

					executorService.execute(
							new DeliverVirtualLinkMessageRunnable(
									sourceNodeId,
									targetNode,
									recipientEndpointProxy,
									outboundVirtualLinkMessage
							)
					);
				}
			}
		}

		private Message constructOutboundVirtualLinkMessage(final byte[] bytes, final String sourceNodeId,
															final String timestamp) {

			// byte 0: ISense Packet Type
			// byte 1: Node API Command Type
			// byte 2: RSSI
			// byte 3: LQI
			// byte 4: Payload Length
			// byte 5-8: Destination Node URN
			// byte 9-12: Source Node URN
			// byte 13-13+Payload Length: Payload

			Message outboundVirtualLinkMessage = new Message();
			outboundVirtualLinkMessage.setSourceNodeId(sourceNodeId);
			outboundVirtualLinkMessage.setTimestamp(
					datatypeFactory.newXMLGregorianCalendar(timestamp)
			);

			// construct message that is actually sent to the destination node URN
			ChannelBuffer header = ChannelBuffers.buffer(3);
			header.writeByte(MESSAGE_TYPE_WISELIB_DOWNSTREAM);
			header.writeByte(WISELIB_VIRTUAL_LINK_MESSAGE);
			header.writeByte(0); // request id according to Node API

			ChannelBuffer payload = ChannelBuffers.wrappedBuffer(bytes, 2, bytes.length - 2);
			ChannelBuffer packet = ChannelBuffers.wrappedBuffer(header, payload);

			byte[] outboundVirtualLinkMessageBinaryData = new byte[packet.readableBytes()];
			packet.getBytes(0, outboundVirtualLinkMessageBinaryData);

			outboundVirtualLinkMessage.setBinaryData(outboundVirtualLinkMessageBinaryData);

			return outboundVirtualLinkMessage;
		}

		private Map<String, WSN> determineVirtualLinkMessageRecipients(final String sourceNodeURN,
																	   final long destinationNode) {

			// check if message is a broadcast or unicast message
			boolean isBroadcast = destinationNode == 0xFFFF;

			// send virtual link message to all recipients
			Map<String, WSN> recipients = new HashMap<String, WSN>();

			if (isBroadcast) {

				ImmutableMap<String, WSN> map = virtualLinksMap.get(sourceNodeURN);
				if (map != null) {
					for (Map.Entry<String, WSN> entry : map.entrySet()) {
						recipients.put(entry.getKey(), entry.getValue());
					}
				}

			} else {

				ImmutableMap<String, WSN> map = virtualLinksMap.get(sourceNodeURN);
				for (String targetNode : map.keySet()) {

					if (StringUtils.parseHexOrDecLongFromUrn(targetNode) == destinationNode) {
						recipients.put(targetNode, map.get(targetNode));
					}
				}
			}

			return recipients;
		}

		private long readDestinationNodeURN(final byte[] virtualLinkMessage) {
			ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(virtualLinkMessage);
			return buffer.getLong(5);
		}

		private void deliverNonVirtualLinkMessageToControllers(final byte[] bytes, final String sourceNodeId,
															   final String timestamp) {

			XMLGregorianCalendar xmlTimestamp = datatypeFactory.newXMLGregorianCalendar(timestamp);

			Message message = new Message();
			message.setSourceNodeId(sourceNodeId);
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

		private String sourceNode;

		private String targetNode;

		private WSN recipient;

		private Message message;

		private int tries = 0;

		public DeliverVirtualLinkMessageRunnable(final String sourceNode, final String targetNode, final WSN recipient,
												 final Message message) {
			this.sourceNode = sourceNode;
			this.targetNode = targetNode;
			this.recipient = recipient;
			this.message = message;
		}

		@Override
		public void run() {
			if (tries < 3) {

				tries++;

				log.debug("Delivering virtual link message to remote testbed service.");

				try {

					recipient.send(Arrays.asList(targetNode), message);

				} catch (Exception e) {

					if (tries >= 3) {

						log.warn("Repeatedly couldn't deliver virtual link message. Destroy virtual link.");
						destroyVirtualLink(sourceNode, targetNode);

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
	 * Threads from this ThreadPoolExecutor will be used to deliver messages to controllers by invoking the {@link
	 * eu.wisebed.api.controller.Controller#receive(java.util.List)} or {@link eu.wisebed.api.controller.Controller#receiveStatus(java.util.List)}
	 * method. The ThreadPoolExecutor is instantiated with at least one thread as there usually will be at least one
	 * controller and, if more controllers are attached to the running experiment the maximum thread pool size will be
	 * increased. By that, the number of threads for web-service calls is bounded by the number of controller endpoints as
	 * more threads would not, in theory, increase the throughput to the controllers.
	 */
	private final ThreadPoolExecutor wsnInstanceWebServiceThreadPool = new ThreadPoolExecutor(
			1,
			Integer.MAX_VALUE,
			60L, TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>(),
			new ThreadFactoryBuilder().setNameFormat("WSNService-WS-Thread %d").build()
	);

	/**
	 * Used to generate secure non-predictable secure request IDs as used request-response matching identifier.
	 */
	private SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	/**
	 * The WSNApp instance associated with this WSN service instance. Does all the testbed internal work around
	 * communicating with the nodes.
	 */
	private WSNApp wsnApp;

	/**
	 * The endpoint URL of this WSN service instance.
	 */
	private URL wsnInstanceEndpointUrl;

	/**
	 * The endpoint of this WSN instance.
	 */
	private Endpoint wsnInstanceEndpoint;

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
	 * The WiseML document that is delivered when {@link WSNServiceImpl#getNetwork()} is called.
	 */
	private final Wiseml wiseML;

	/**
	 * The set of node URNs that are reserved and thereby associated with this {@link WSN} instance.
	 */
	private final ImmutableSet<String> reservedNodes;

	public WSNServiceImpl(final String urnPrefix, final URL wsnInstanceEndpointUrl, final Wiseml wiseML,
						  final String[] reservedNodes, final DeliveryManager deliveryManager, final WSNApp wsnApp) {

		checkNotNull(urnPrefix);
		checkNotNull(wsnInstanceEndpointUrl);
		checkNotNull(wiseML);
		checkNotNull(wsnApp);

		this.wsnInstanceEndpointUrl = wsnInstanceEndpointUrl;
		this.wsnApp = wsnApp;
		this.wiseML = wiseML;
		this.deliveryManager = deliveryManager;

		executorService = Executors.newSingleThreadScheduledExecutor(
				new ThreadFactoryBuilder().setNameFormat("WSNService-Thread %d").build()
		);

		this.preconditions = new WSNPreconditions(newArrayList(urnPrefix), newArrayList(reservedNodes));
		this.reservedNodes = ImmutableSet.copyOf(reservedNodes);

	}

	private WSNNodeMessageReceiverInternal nodeMessageReceiver = new WSNNodeMessageReceiverInternal();

	@Override
	public void start() throws Exception {

		log.info("Starting WSN service...");

		wsnInstanceEndpoint = Endpoint.create(this);
		wsnInstanceEndpoint.setExecutor(wsnInstanceWebServiceThreadPool);

		String bindAllInterfacesUrl = System.getProperty("disableBindAllInterfacesUrl") != null ?
				wsnInstanceEndpointUrl.toString() :
				UrlUtils.convertHostToZeros(wsnInstanceEndpointUrl.toString());

		log.info("Starting WSN API service on binding URL {} for endpoint URL {}",
				bindAllInterfacesUrl,
				wsnInstanceEndpointUrl.toString()
		);

		wsnInstanceEndpoint.publish(bindAllInterfacesUrl);

		wsnApp.addNodeMessageReceiver(nodeMessageReceiver);

		deliveryManager.start();

		log.info("Started WSN API service wsnInstanceEndpoint on {}", bindAllInterfacesUrl);
	}

	@Override
	public void stop() {

		log.info("Stopping WSN service...");

		wsnApp.removeNodeMessageReceiver(nodeMessageReceiver);
		deliveryManager.experimentEnded();
		deliveryManager.stop();

		if (wsnInstanceEndpoint != null) {
			wsnInstanceEndpoint.stop();
			log.info("Stopped WSN service wsnInstanceEndpoint on {}", wsnInstanceEndpointUrl);
		}

		ExecutorUtils.shutdown(executorService, 5, TimeUnit.SECONDS);

		log.info("Stopped WSN service!");

	}

	@Override
	public String getVersion() {
		return Constants.VERSION;
	}

	@Override
	public void addController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {

		if (!"NONE".equals(controllerEndpointUrl)) {
			NetworkUtils.checkConnectivity(controllerEndpointUrl);
		}
		deliveryManager.addController(controllerEndpointUrl);
	}

	@Override
	public void removeController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {

		deliveryManager.removeController(controllerEndpointUrl);
	}

	@Override
	public String send(@WebParam(name = "nodeIds", targetNamespace = "") final List<String> nodeIds,
					   @WebParam(name = "msg", targetNamespace = "") Message message) {

		preconditions.checkSendArguments(nodeIds, message);

		log.debug("WSNServiceImpl.send({},{})", nodeIds, message);

		final String requestId = secureIdGenerator.getNextId();
		final long start = System.currentTimeMillis();

		try {
			wsnApp.send(new HashSet<String>(nodeIds), message.getBinaryData(), message.getSourceNodeId(),
					message.getTimestamp().toXMLFormat(), new WSNApp.Callback() {

				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					long end = System.currentTimeMillis();
					log.debug("Received reply from device after {} ms.", (end - start));
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(nodeIds, requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		return requestId;

	}

	@Override
	public String setChannelPipeline(@WebParam(name = "nodes", targetNamespace = "")
									 final List<String> nodes,
									 @WebParam(name = "channelHandlerConfigurations", targetNamespace = "")
									 final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		preconditions.checkSetChannelPipelineArguments(nodes, channelHandlerConfigurations);

		log.debug("WSNServiceImpl.setChannelPipeline({}, {})", nodes, channelHandlerConfigurations);

		final String requestId = secureIdGenerator.getNextId();
		final long start = System.currentTimeMillis();

		try {
			wsnApp.setChannelPipeline(new HashSet<String>(nodes), channelHandlerConfigurations, new WSNApp.Callback() {

				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					long end = System.currentTimeMillis();
					log.debug("Received reply after {} ms.", (end - start));
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					deliveryManager.receiveFailureStatusMessages(nodes, requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		return requestId;
	}

	@Override
	public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodeIds) {

		preconditions.checkAreNodesAliveArguments(nodeIds);

		log.debug("WSNServiceImpl.checkAreNodesAlive({})", nodeIds);

		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.areNodesAlive(new HashSet<String>(nodeIds), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(nodeIds, requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		return requestId;
	}

	@Override
	public String flashPrograms(@WebParam(name = "nodeIds", targetNamespace = "") final List<String> nodeIds,
								@WebParam(name = "programIndices", targetNamespace = "")
								final List<Integer> programIndices,
								@WebParam(name = "programs", targetNamespace = "") final List<Program> programs) {

		preconditions.checkFlashProgramsArguments(nodeIds, programIndices, programs);

		log.debug("WSNServiceImpl.flashPrograms");

		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.flashPrograms(TypeConverter.convert(nodeIds, programIndices, programs), new WSNApp.Callback() {
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
					deliveryManager.receiveFailureStatusMessages(nodeIds, requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		return requestId;
	}

	@Override
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {

		HandlerFactoryRegistry handlerFactoryRegistry = new HandlerFactoryRegistry();
		try {
			ProtocolCollection.registerProtocols(handlerFactoryRegistry);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		final List<ChannelHandlerDescription> channelHandlerDescriptions = newArrayList();

		for (HandlerFactoryRegistry.ChannelHandlerDescription handlerDescription : handlerFactoryRegistry
				.getChannelHandlerDescriptions()) {
			channelHandlerDescriptions.add(convert(handlerDescription));
		}

		return channelHandlerDescriptions;
	}

	@Override
	public String getNetwork() {
		log.debug("WSNServiceImpl.getNetwork");
		return WiseMLHelper.serialize(wiseML);
	}

	@Override
	public String resetNodes(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodeIds) {

		preconditions.checkResetNodesArguments(nodeIds);

		log.debug("WSNServiceImpl.resetNodes");

		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.resetNodes(new HashSet<String>(nodeIds), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(nodeIds, requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		return requestId;
	}

	/**
	 * Map: (Source Node URN) -> (Map: (Target Node URN) -> (WSN endpoint instance))
	 */
	private ImmutableMap<String, ImmutableMap<String, WSN>> virtualLinksMap = ImmutableMap.of();

	@Override
	public String setVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") final String sourceNode,
								 @WebParam(name = "targetNode", targetNamespace = "") final String targetNode,
								 @WebParam(name = "remoteServiceInstance", targetNamespace = "")
								 final String remoteServiceInstance,
								 @WebParam(name = "parameters", targetNamespace = "") List<String> parameters,
								 @WebParam(name = "filters", targetNamespace = "") List<String> filters) {

		preconditions.checkSetVirtualLinkArguments(sourceNode, targetNode, remoteServiceInstance, parameters, filters);

		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.setVirtualLink(sourceNode, targetNode, new WSNApp.Callback() {

				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));

					if (requestStatus.getStatus().getValue() == 1) {
						addVirtualLink(sourceNode, targetNode, remoteServiceInstance);
					}

				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(sourceNode), requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		// TODO support filters

		return requestId;
	}

	private void addVirtualLink(String sourceNode, String targetNode, String remoteServiceInstance) {

		if (!containsVirtualLink(sourceNode, targetNode)) {

			log.debug("+++ Adding virtual link from {} to {}", sourceNode, targetNode);

			WSN remoteServiceEndpoint = WSNServiceHelper.getWSNService(remoteServiceInstance);

			//Create a new immutable map with this sourceNode and all existing <targetNode, WSN> mappings
			ImmutableMap.Builder<String, WSN> targetNodeMapBuilder = ImmutableMap.builder();

			//Add potentially existing <targetNode, WSN> mappings for this source node to the new list
			if (virtualLinksMap.get(sourceNode) != null) {
				targetNodeMapBuilder.putAll(virtualLinksMap.get(sourceNode));
			}
			//Add the new <targetNode, WSN> mapping to this new list
			targetNodeMapBuilder.put(targetNode, remoteServiceEndpoint);

			ImmutableMap.Builder<String, ImmutableMap<String, WSN>> virtualLinksMapBuilder = ImmutableMap.builder();

			//We now add all existing source nodes to the map except for the current source node
			//It looks a bit strange but we cannot use putAll and then overwrite an existing key
			//because the ImmutableMapBuilder forbids duplicate keys
			for (String existingSourceNode : virtualLinksMap.keySet()) {
				if (!existingSourceNode.equals(sourceNode)) {
					virtualLinksMapBuilder.put(existingSourceNode, virtualLinksMap.get(existingSourceNode));
				}
			}

			virtualLinksMapBuilder.put(sourceNode, targetNodeMapBuilder.build());
			virtualLinksMap = virtualLinksMapBuilder.build();

		} else {
			log.debug("+++ Not adding virtual link from {} to {} as it is already established", sourceNode, targetNode);
		}

	}

	private void removeVirtualLink(final String sourceNode, final String targetNode) {

		if (containsVirtualLink(sourceNode, targetNode)) {

			log.debug("--- Removing virtual link from {} to {}", sourceNode, targetNode);

			ImmutableMap.Builder<String, WSN> targetNodeMapBuilder = ImmutableMap.builder();
			for (Map.Entry<String, WSN> oldEntry : virtualLinksMap.get(sourceNode).entrySet()) {
				if (!targetNode.equals(oldEntry.getKey())) {
					targetNodeMapBuilder.put(oldEntry.getKey(), oldEntry.getValue());
				}
			}

			ImmutableMap.Builder<String, ImmutableMap<String, WSN>> virtualLinksMapBuilder = ImmutableMap.builder();

			for (String existingSourceNode : virtualLinksMap.keySet()) {
				if (!existingSourceNode.equals(sourceNode)) {
					virtualLinksMapBuilder.put(existingSourceNode, virtualLinksMap.get(existingSourceNode));
				}
			}

			virtualLinksMapBuilder.put(sourceNode, targetNodeMapBuilder.build());

			virtualLinksMap = virtualLinksMapBuilder.build();

		}

	}

	private boolean containsVirtualLink(String sourceNode, String targetNode) {
		ImmutableMap<String, WSN> map = virtualLinksMap.get(sourceNode);
		return map != null && map.containsKey(targetNode);
	}

	@Override
	public String destroyVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") final String sourceNode,
									 @WebParam(name = "targetNode", targetNamespace = "") final String targetNode) {

		preconditions.checkDestroyVirtualLinkArguments(sourceNode, targetNode);

		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.destroyVirtualLink(sourceNode, targetNode, new WSNApp.Callback() {

				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));

					if (requestStatus.getStatus().getValue() == 1) {
						removeVirtualLink(sourceNode, targetNode);
					}

				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(sourceNode), requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		return requestId;
	}

	@Override
	public String disableNode(@WebParam(name = "node", targetNamespace = "") final String node) {

		preconditions.checkDisableNodeArguments(node);

		log.debug("WSNServiceImpl.disableNode");

		final String requestId = secureIdGenerator.getNextId();

		try {

			wsnApp.disableNode(node, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(node), requestId, e, -1);
				}
			}
			);

		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		return requestId;
	}

	@Override
	public String disablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") final String nodeA,
									  @WebParam(name = "nodeB", targetNamespace = "") final String nodeB) {

		preconditions.checkDisablePhysicalLinkArguments(nodeA, nodeB);

		log.debug("WSNServiceImpl.disablePhysicalLink");

		final String requestId = secureIdGenerator.getNextId();

		try {

			wsnApp.disablePhysicalLink(nodeA, nodeB, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(nodeA), requestId, e, -1);
				}
			}
			);

		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		return requestId;

	}

	@Override
	public String enableNode(@WebParam(name = "node", targetNamespace = "") final String node) {

		preconditions.checkEnableNodeArguments(node);

		log.debug("WSNServiceImpl.enableNode");

		final String requestId = secureIdGenerator.getNextId();

		try {

			wsnApp.enableNode(node, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(node), requestId, e, -1);
				}
			}
			);

		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		return requestId;

	}

	@Override
	public String enablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") final String nodeA,
									 @WebParam(name = "nodeB", targetNamespace = "") final String nodeB) {

		preconditions.checkEnablePhysicalLinkArguments(nodeA, nodeB);

		log.debug("WSNServiceImpl.enablePhysicalLink");

		final String requestId = secureIdGenerator.getNextId();

		try {

			wsnApp.enablePhysicalLink(nodeA, nodeB, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					deliveryManager.receiveFailureStatusMessages(Lists.newArrayList(nodeA), requestId, e, -1);
				}
			}
			);

		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		return requestId;

	}

	@Override
	public List<String> getFilters() {
		log.debug("WSNServiceImpl.getFilters");
		throw new java.lang.UnsupportedOperationException("Method is not yet implemented.");
	}

	private ChannelHandlerDescription convert(
			final HandlerFactoryRegistry.ChannelHandlerDescription handlerDescription) {

		ChannelHandlerDescription target = new ChannelHandlerDescription();
		target.setDescription(handlerDescription.getDescription());
		target.setName(handlerDescription.getName());
		for (Map.Entry<String, String> entry : handlerDescription.getConfigurationOptions().entries()) {
			final KeyValuePair keyValuePair = new KeyValuePair();
			keyValuePair.setKey(entry.getKey());
			keyValuePair.setValue(entry.getValue());
			target.getConfigurationOptions().add(keyValuePair);
		}
		return target;
	}

}
