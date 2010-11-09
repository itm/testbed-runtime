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
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.Nullable;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNNodeMessageReceiver;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.ns.wiseml._1.Wiseml;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.ControllerHelper;
import eu.wisebed.testbed.api.wsn.WSNPreconditions;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v211.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.JAXB;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Endpoint;
import java.io.StringWriter;
import java.lang.UnsupportedOperationException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
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
	 * Threads from this ThreadPoolExecutor will be used to deliver messages to controllers by invoking the {@link
	 * eu.wisebed.testbed.api.wsn.v211.Controller#receive(eu.wisebed.testbed.api.wsn.v211.Message)} or {@link
	 * eu.wisebed.testbed.api.wsn.v211.Controller#receiveStatus(eu.wisebed.testbed.api.wsn.v211.RequestStatus)} method. The
	 * ThreadPoolExecutor is instantiated with at least one thread as there usually will be at least one controller and, if
	 * more controllers are attached to the running experiment the maximum thread pool size will be increased. By that, the
	 * number of threads for web-service calls is bounded by the number of controller endpoints as more threads would not,
	 * in theory, increase the throughput to the controllers.
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
	private ControllerHelper controllerHelper;

	private Wiseml wiseML;

	private String urnPrefix;

	private Set<String> reservedNodes;

	@Inject
	public WSNServiceImpl(@Named(WSNServiceModule.URN_PREFIX) String urnPrefix,
						  @Named(WSNServiceModule.WSN_SERVICE_ENDPOINT_URL) URL wsnInstanceEndpointUrl,
						  @Named(WSNServiceModule.CONTROLLER_SERVICE_ENDPOINT_URL) URL controllerEndpointUrl,
						  @Named(WSNServiceModule.WISEML) Wiseml wiseML,
						  @Named(WSNServiceModule.RESERVED_NODES) @Nullable String[] reservedNodes,
						  @Named(WSNServiceModule.MAXIMUM_DELIVERY_QUEUE_SIZE) @Nullable Integer maxmimumDeliveryQueueSize,
						  WSNApp wsnApp) {

		checkNotNull(urnPrefix);
		checkNotNull(wsnInstanceEndpointUrl);
		checkNotNull(controllerEndpointUrl);
		checkNotNull(wiseML);
		checkNotNull(wsnApp);

		this.wsnInstanceEndpointUrl = wsnInstanceEndpointUrl;
		this.wsnApp = wsnApp;
		this.wiseML = wiseML;

		executorService = Executors.newSingleThreadScheduledExecutor(
				new ThreadFactoryBuilder().setNameFormat("WSNService-Thread %d").build()
		);
		controllerHelper = new ControllerHelper(maxmimumDeliveryQueueSize);

		addController(controllerEndpointUrl.toString());

		this.preconditions = new WSNPreconditions();
		this.urnPrefix = urnPrefix;
		this.preconditions.addServedUrnPrefixes(urnPrefix);
		this.reservedNodes = Sets.newHashSet(reservedNodes);
	}

	private WSNNodeMessageReceiverInternal nodeMessageReceiver = new WSNNodeMessageReceiverInternal();

	private class WSNNodeMessageReceiverInternal implements WSNNodeMessageReceiver {

		private static final byte MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

		//private static final byte MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

		//private static final byte NODE_OUTPUT_TEXT = 50;

		//private static final byte NODE_OUTPUT_BYTE = 51;

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
		public void receive(WSNAppMessages.Message wsnMessage) {

			/* this is a message that was received from a sensor node. we now have to check if this is a virtual link
			 * message. in that case we will deliver it to the destination node if there's a virtual link currently.
			 * if the message is a virtual broadcast we'll deliver it to all destinations this node's connected to.
			 * if the message is not a virtual link we'll deliver it to the controller of the experiment as it is. */

            if (!reservedNodes.contains(wsnMessage.getSourceNodeId())) {
                log.warn("Received message from unreserved node \"{}\".", wsnMessage.getSourceNodeId());
                return;
            }

			XMLGregorianCalendar timestamp = datatypeFactory.newXMLGregorianCalendar(wsnMessage.getTimestamp());

			Message message = new Message();
			message.setSourceNodeId(wsnMessage.getSourceNodeId());
			message.setTimestamp(timestamp);

			if (wsnMessage.hasBinaryMessage()) {

				BinaryMessage binaryMessage = new BinaryMessage();
				binaryMessage.setBinaryData(wsnMessage.getBinaryMessage().getBinaryData().toByteArray());
				binaryMessage.setBinaryType((byte) wsnMessage.getBinaryMessage().getBinaryType());

				message.setBinaryMessage(binaryMessage);

			} else if (wsnMessage.hasTextMessage()) {

				TextMessage textMessage = new TextMessage();
				textMessage.setMessageLevel(MessageLevel.valueOf(wsnMessage.getTextMessage().getMessageLevel()
						.toString()
				)
				);
				textMessage.setMsg(wsnMessage.getTextMessage().getMsg());

				message.setTextMessage(textMessage);

			}

			// check if message is a virtual link message
			boolean isVirtualLinkMessage = wsnMessage.getBinaryMessage() != null
					&& wsnMessage.getBinaryMessage().hasBinaryData()
					&& wsnMessage.getBinaryMessage().getBinaryData().toByteArray()[0] == NODE_OUTPUT_VIRTUAL_LINK;

			if (!isVirtualLinkMessage) {

				// deliver to controller in every case, he's a promiscuous listener
				controllerHelper.receive(message);

			} else {

				// message is a virtual link message

				ByteBuffer buffer = ByteBuffer.wrap(wsnMessage.getBinaryMessage().getBinaryData().toByteArray());
				BinaryMessage binaryMessage = new BinaryMessage();
				byte[] bytes = new byte[message.getBinaryMessage().getBinaryData().length + 1];
				bytes[0] = WISELIB_VIRTUAL_LINK_MESSAGE;
				bytes[1] = 0; // request id according to Node API

				// copy payload (i.e. cut away NODE_OUTPUT_VIRTUAL_LINK in the byte zero)
				int index = 2;
				for (int i = 1; i < message.getBinaryMessage().getBinaryData().length; ++i) {
					bytes[index] = message.getBinaryMessage().getBinaryData()[i];
					index++;
				}

				binaryMessage.setBinaryData(bytes);
				binaryMessage.setBinaryType(MESSAGE_TYPE_WISELIB_DOWNSTREAM);

				// check if message is a broadcast or unicast message
				long destinationNode = 0;
				try {
					destinationNode = buffer.getLong(4);
				} catch (Exception e) {
					String msg =
							"probably node akk message popped up in web service this should never happen. ignoring";
					log.warn(msg, e);
				}
				boolean isBroadcast = destinationNode == 0xFFFF;

				// send virtual link message to all recipients
				Map<String, WSN> recipients = new HashMap<String, WSN>();

				if (isBroadcast) {

					ImmutableMap<String, WSN> map = virtualLinksMap.get(wsnMessage.getSourceNodeId());
					if (map == null) {
						log.warn("received virtual link message, but no virtual links defined, ignoring");
						return;
					}
					for (Map.Entry<String, WSN> entry : map.entrySet()) {
						recipients.put(entry.getKey(), entry.getValue());
					}

				} else {

					ImmutableMap<String, WSN> map = virtualLinksMap.get(wsnMessage.getSourceNodeId());
					for (String targetNode : map.keySet()) {

						String[] split = targetNode.split(":");

						if (Long.parseLong(split[split.length - 1]) == destinationNode) {

							recipients.put(targetNode, map.get(targetNode));
							break;
						}
					}
				}

				message.setBinaryMessage(binaryMessage);
				for (Map.Entry<String, WSN> recipient : recipients.entrySet()) {

					executorService.execute(new DeliverVirtualLinkMessageRunnable(
							wsnMessage.getSourceNodeId(), recipient.getKey(), recipient.getValue(), message
					)
					);
				}

			}

		}
	}

	@Override
	public void start() throws Exception {

		log.info("Starting WSN service...");

		wsnInstanceEndpoint = Endpoint.create(this);
		wsnInstanceEndpoint.setExecutor(wsnInstanceWebServiceThreadPool);

		String bindAllInterfacesUrl = UrlUtils.convertHostToZeros(wsnInstanceEndpointUrl.toString());
		log.debug("Endpoint URL: " + wsnInstanceEndpointUrl.toString());
		log.debug("Binding  URL: " + bindAllInterfacesUrl);
		log.debug("Maximum delivery queue size: {}", controllerHelper.getMaximumDeliveryQueueSize());

		wsnInstanceEndpoint.publish(bindAllInterfacesUrl);

		log.info("Started WSN API service wsnInstanceEndpoint on {}", bindAllInterfacesUrl);

		wsnApp.addNodeMessageReceiver(nodeMessageReceiver);
		
		log.info("Started WSN service!");

	}

	@Override
	public void stop() {

		log.info("Stopping WSN service...");

		wsnApp.removeNodeMessageReceiver(nodeMessageReceiver);

		// TODO define clean lifecycle for WSN app, following lifecycle of WSNServiceImpl
        /*try {
			wsnApp.stop();
		} catch (Exception e) {
			log.error("" + e, e);
		}*/

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

		controllerHelper.addController(controllerEndpointUrl);
		//wsnInstanceWebServiceThreadPool.setMaximumPoolSize(controllerHelper.getControllerCount());
	}

	@Override
	public void removeController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {

		controllerHelper.removeController(controllerEndpointUrl);
		//wsnInstanceWebServiceThreadPool.setMaximumPoolSize(controllerHelper.getControllerCount());
	}

	@Override
	public String send(@WebParam(name = "nodeIds", targetNamespace = "") List<String> nodeIds,
					   @WebParam(name = "msg", targetNamespace = "") Message message) {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkSendArguments(nodeIds, message);

		// log.debug("WSNServiceImpl.send({},{})", nodeIds, message);

		final String requestId = secureIdGenerator.getNextId();
		final long start = System.currentTimeMillis();
		// linkLogger.debug("sending virtual Link Message:" + (boolean
		// isVirtualLinkMessage =
		// message.getBinaryMessage() != null &&
		// message.getBinaryMessage().hasinaryData() &&
		// message.getBinaryMessage().getBinaryData()[0] == 52;) );

		//check if only reserved nodes
		preconditions.checkNodesReserved(nodeIds, reservedNodes);

		try {
			wsnApp.send(new HashSet<String>(nodeIds), convert(message), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					long end = System.currentTimeMillis();
					log.debug("Received reply from device after {} ms.", (end - start));
					controllerHelper.receiveStatus(convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					// TODO throw declared type
					throw new RuntimeException(e);
				}
			}
			);
		} catch (UnknownNodeUrnException_Exception e) {
			controllerHelper.receiveUnkownNodeUrnRequestStatus(e, requestId);
		}

		return requestId;

	}

	private RequestStatus convert(WSNAppMessages.RequestStatus requestStatus, String requestId) {
		RequestStatus retRequestStatus = new RequestStatus();
		retRequestStatus.setRequestId(requestId);
		WSNAppMessages.RequestStatus.Status status = requestStatus.getStatus();
		Status retStatus = new Status();
		retStatus.setMsg(status.getMsg());
		retStatus.setNodeId(status.getNodeId());
		retStatus.setValue(status.getValue());
		retRequestStatus.getStatus().add(retStatus);
		return retRequestStatus;
	}

	private WSNAppMessages.Message convert(Message message) {

		WSNAppMessages.Message.Builder builder = WSNAppMessages.Message.newBuilder();

		if (message.getBinaryMessage() != null) {

			WSNAppMessages.Message.BinaryMessage.Builder binaryMessage = WSNAppMessages.Message.BinaryMessage
					.newBuilder().setBinaryType(message.getBinaryMessage().getBinaryType()).setBinaryData(
							ByteString.copyFrom(message.getBinaryMessage().getBinaryData())
					);
			builder.setBinaryMessage(binaryMessage);
		}

		if (message.getTextMessage() != null) {

			WSNAppMessages.Message.TextMessage.Builder textMessage = WSNAppMessages.Message.TextMessage.newBuilder()
					.setMessageLevel(
							WSNAppMessages.Message.MessageLevel.valueOf(message.getTextMessage().getMessageLevel()
									.value()
							)
					).setMsg(message.getTextMessage().getMsg());
			builder.setTextMessage(textMessage);
		}

		builder.setSourceNodeId(message.getSourceNodeId());
		builder.setTimestamp(message.getTimestamp().toString());

		return builder.build();
	}

	@Override
	public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") List<String> nodes) {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkAreNodesAliveArguments(nodes);
		preconditions.checkNodesReserved(nodes, reservedNodes);

		log.debug("WSNServiceImpl.checkAreNodesAlive({})", nodes);

		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.areNodesAlive(new HashSet<String>(nodes), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					controllerHelper.receiveStatus(convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					// TODO throw declared type
					throw new RuntimeException(e);
				}
			}
			);
		} catch (UnknownNodeUrnException_Exception e) {
			controllerHelper.receiveUnkownNodeUrnRequestStatus(e, requestId);
		}

		return requestId;
	}

	@Override
	public String flashPrograms(@WebParam(name = "nodeIds", targetNamespace = "") List<String> nodeIds,
								@WebParam(name = "programIndices", targetNamespace = "") List<Integer> programIndices,
								@WebParam(name = "programs", targetNamespace = "") List<Program> programs) {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkFlashProgramsArguments(nodeIds, programIndices, programs);
		preconditions.checkNodesReserved(nodeIds, reservedNodes);

		log.debug("WSNServiceImpl.flashPrograms");

		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.flashPrograms(convert(nodeIds, programIndices, programs), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					if (requestStatus.hasStatus() && requestStatus.getStatus().hasValue() && requestStatus.getStatus()
							.hasNodeId()) {
						log.debug(
								"Flashing node {} completed {} percent.",
								requestStatus.getStatus().getNodeId(),
								requestStatus.getStatus().getValue()
						);
					}
					controllerHelper.receiveStatus(convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					// TODO throw declared type
					throw new RuntimeException(e);
				}
			}
			);
		} catch (UnknownNodeUrnException_Exception e) {
			controllerHelper.receiveUnkownNodeUrnRequestStatus(e, requestId);
		}

		return requestId;
	}

	private Map<String, WSNAppMessages.Program> convert(List<String> nodeIds, List<Integer> programIndices,
														List<Program> programs) {

		Map<String, WSNAppMessages.Program> programsMap = new HashMap<String, WSNAppMessages.Program>();

		List<WSNAppMessages.Program> convertedPrograms = convert(programs);

		for (int i = 0; i < nodeIds.size(); i++) {
			programsMap.put(nodeIds.get(i), convertedPrograms.get(programIndices.get(i)));
		}

		return programsMap;
	}

	private List<WSNAppMessages.Program> convert(List<Program> programs) {
		List<WSNAppMessages.Program> list = new ArrayList<WSNAppMessages.Program>(programs.size());
		for (Program program : programs) {
			list.add(convert(program));
		}
		return list;
	}

	private WSNAppMessages.Program convert(Program program) {
		return WSNAppMessages.Program.newBuilder().setMetaData(convert(program.getMetaData())).setProgram(
				ByteString.copyFrom(program.getProgram())
		).build();
	}

	private WSNAppMessages.Program.ProgramMetaData convert(ProgramMetaData metaData) {
		return WSNAppMessages.Program.ProgramMetaData.newBuilder().setName(metaData.getName()).setOther(
				metaData.getOther()
		).setPlatform(metaData.getPlatform()).setVersion(metaData.getVersion()).build();
	}

	@Override
	public String getNetwork() {

        log.debug("WSNServiceImpl.getNetwork");

        StringWriter writer = new StringWriter();
        JAXB.marshal(wiseML, writer);

		return writer.toString();
	}

	@Override
	public String resetNodes(@WebParam(name = "nodes", targetNamespace = "") List<String> nodes) {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkResetNodesArguments(nodes);
		preconditions.checkNodesReserved(nodes, reservedNodes);

		log.debug("WSNServiceImpl.resetNodes");

		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.resetNodes(new HashSet<String>(nodes), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					controllerHelper.receiveStatus(convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					// TODO throw declared type
					throw new RuntimeException(e);
				}
			}
			);
		} catch (UnknownNodeUrnException_Exception e) {
			controllerHelper.receiveUnkownNodeUrnRequestStatus(e, requestId);
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

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkSetVirtualLinkArguments(sourceNode, targetNode, remoteServiceInstance, parameters, filters);
		preconditions.checkNodeReserved(sourceNode, reservedNodes);
		preconditions.checkNodeReserved(targetNode, reservedNodes);

		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.setVirtualLink(sourceNode, targetNode, new WSNApp.Callback() {

				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

					controllerHelper.receiveStatus(convert(requestStatus, requestId));

					if (requestStatus.getStatus().getValue() == 1) {
						addVirtualLink(sourceNode, targetNode, remoteServiceInstance);
					}

				}

				@Override
				public void failure(Exception e) {
					// TODO throw declared type
					throw new RuntimeException(e);
				}
			}
			);
		} catch (UnknownNodeUrnException_Exception e) {
			controllerHelper.receiveUnkownNodeUrnRequestStatus(e, requestId);
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
			//because the ImmutableMapBuider forbids duplicate keys
			//TODO This is utterly slow, fix this
			for (String existingSourceNode : virtualLinksMap.keySet()) {
				if (!existingSourceNode.equals(sourceNode)) {
					virtualLinksMapBuilder.put(existingSourceNode, virtualLinksMap.get(existingSourceNode));
				}
			}

			virtualLinksMapBuilder.put(sourceNode, targetNodeMapBuilder.build());
			virtualLinksMap = virtualLinksMapBuilder.build();

		}

	}

	private void removeVirtualLink(final String sourceNode, final String targetNode) {

		if (containsVirtualLink(sourceNode, targetNode)) {

			log.debug("--- Removing virtual link from {} to {}", sourceNode, targetNode);

			ImmutableMap.Builder<String, WSN> targetNodeMapBuilder = ImmutableMap.builder();
			for (Map.Entry<String, WSN> oldEntry : virtualLinksMap.get(sourceNode).entrySet()) {
				if (!targetNode.equals(oldEntry.getKey())) {
					// TODO why is this executing??
					targetNodeMapBuilder.put(oldEntry.getKey(), oldEntry.getValue());
				}
			}

			ImmutableMap.Builder<String, ImmutableMap<String, WSN>> virtualLinksMapBuilder = ImmutableMap.builder();

			//TODO This is utterly slow, fix this
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

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkDestroyVirtualLinkArguments(sourceNode, targetNode);
		preconditions.checkNodeReserved(sourceNode, reservedNodes);
		preconditions.checkNodeReserved(targetNode, reservedNodes);

		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.destroyVirtualLink(sourceNode, targetNode, new WSNApp.Callback() {

				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

					controllerHelper.receiveStatus(convert(requestStatus, requestId));

					if (requestStatus.getStatus().getValue() == 1) {
						removeVirtualLink(sourceNode, targetNode);
					}

				}

				@Override
				public void failure(Exception e) {
					// TODO throw declared type
					throw new RuntimeException(e);
				}
			}
			);
		} catch (UnknownNodeUrnException_Exception e) {
			controllerHelper.receiveUnkownNodeUrnRequestStatus(e, requestId);
		}

		return requestId;
	}


	@Override
	public String defineNetwork(@WebParam(name = "newNetwork", targetNamespace = "") String newNetwork) {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkDefineNetworkArguments(newNetwork);

		log.debug("WSNServiceImpl.defineNetwork");
		throw new UnsupportedOperationException("Method is not yet implemented.");
	}

	@Override
	public String describeCapabilities(@WebParam(name = "capability", targetNamespace = "") String capability)
			throws UnsupportedOperationException_Exception {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkDescribeCapabilitiesArguments(capability);

		log.debug("WSNServiceImpl.describeCapabilities");
		throw new UnsupportedOperationException("Method is not yet implemented.");
	}

	@Override
	public String disableNode(@WebParam(name = "node", targetNamespace = "") String node) {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkDisableNodeArguments(node);
		preconditions.checkNodeReserved(node, reservedNodes);

		log.debug("WSNServiceImpl.disableNode");

		final String requestId = secureIdGenerator.getNextId();

		try {

			wsnApp.disableNode(node, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					controllerHelper.receiveStatus(convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					// TODO throw declared type
					throw new RuntimeException(e);
				}
			});

		} catch (UnknownNodeUrnException_Exception e) {
			controllerHelper.receiveUnkownNodeUrnRequestStatus(e, requestId);
		}

		return requestId;
	}

	@Override
	public String disablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") String nodeA,
									  @WebParam(name = "nodeB", targetNamespace = "") String nodeB) {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkDisablePhysicalLinkArguments(nodeA, nodeB);
		preconditions.checkNodeReserved(nodeA, reservedNodes);
		preconditions.checkNodeReserved(nodeB, reservedNodes);

		log.debug("WSNServiceImpl.disablePhysicalLink");

		final String requestId = secureIdGenerator.getNextId();

		try {

			wsnApp.disablePhysicalLink(nodeA, nodeB, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					controllerHelper.receiveStatus(convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					// TODO throw declared type
					throw new RuntimeException(e);
				}
			});

		} catch (UnknownNodeUrnException_Exception e) {
			controllerHelper.receiveUnkownNodeUrnRequestStatus(e, requestId);
		}

		return requestId;

	}

	@Override
	public String enableNode(@WebParam(name = "node", targetNamespace = "") String node) {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkEnableNodeArguments(node);
		preconditions.checkNodeReserved(node, reservedNodes);

		log.debug("WSNServiceImpl.enableNode");

		final String requestId = secureIdGenerator.getNextId();

		try {

			wsnApp.enableNode(node, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					controllerHelper.receiveStatus(convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					// TODO throw declared type
					throw new RuntimeException(e);
				}
			});

		} catch (UnknownNodeUrnException_Exception e) {
			controllerHelper.receiveUnkownNodeUrnRequestStatus(e, requestId);
		}

		return requestId;

	}

	@Override
	public String enablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") String nodeA,
									 @WebParam(name = "nodeB", targetNamespace = "") String nodeB) {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkEnablePhysicalLinkArguments(nodeA, nodeB);
		preconditions.checkNodeReserved(nodeA, reservedNodes);
		preconditions.checkNodeReserved(nodeB, reservedNodes);

		log.debug("WSNServiceImpl.enablePhysicalLink");

		final String requestId = secureIdGenerator.getNextId();

		try {

			wsnApp.enablePhysicalLink(nodeA, nodeB, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					controllerHelper.receiveStatus(convert(requestStatus, requestId));
				}

				@Override
				public void failure(final Exception e) {
					// TODO throw declared type
					throw new RuntimeException(e);
				}
			});

		} catch (UnknownNodeUrnException_Exception e) {
			controllerHelper.receiveUnkownNodeUrnRequestStatus(e, requestId);
		}

		return requestId;
		
	}

	@Override
	public List<String> getFilters() {
		log.debug("WSNServiceImpl.getFilters");
		throw new UnsupportedOperationException("Method is not yet implemented.");
	}

	@Override
	public List<String> getNeighbourhood(@WebParam(name = "node", targetNamespace = "") String node)
			throws UnknownNodeUrnException_Exception {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkGetNeighbourhoodArguments(node);
		preconditions.checkNodeReserved(node, reservedNodes);

		log.debug("WSNServiceImpl.getNeighbourhood");
		throw new UnsupportedOperationException("Method is not yet implemented.");
	}

	@Override
	public String getPropertyValueOf(@WebParam(name = "node", targetNamespace = "") String node,
									 @WebParam(name = "propertyName", targetNamespace = "") String propertyName)
			throws UnknownNodeUrnException_Exception {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkGetPropertyValueOfArguments(node, propertyName);
		preconditions.checkNodeReserved(node, reservedNodes);

		log.debug("WSNServiceImpl.getPropertyValueOf");
		throw new UnsupportedOperationException("Method is not yet implemented.");
	}

	@Override
	public String setStartTime(@WebParam(name = "time", targetNamespace = "") XMLGregorianCalendar time) {

		// TODO catch precondition exceptions and throw cleanly defined exception to client
		preconditions.checkSetStartTimeArguments(time);

		log.debug("WSNServiceImpl.setStartTime");
		throw new UnsupportedOperationException("Method is not yet implemented.");
	}

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
}
