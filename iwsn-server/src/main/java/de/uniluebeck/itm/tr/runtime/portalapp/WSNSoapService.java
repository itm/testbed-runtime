package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.Service;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.WSN;
import org.jboss.netty.util.internal.ExecutorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@WebService(
		serviceName = "WSNService",
		targetNamespace = "urn:WSNService",
		portName = "WSNPort",
		endpointInterface = "eu.wisebed.api.wsn.WSN"
)
public class WSNSoapService implements WSN, Service {

	private static final Logger log = LoggerFactory.getLogger(WSNSoapService.class);

	private final WSNService wsn;

	private final WSNServiceConfig config;

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

	private Endpoint endpoint;

	public WSNSoapService(final WSNService wsn, final WSNServiceConfig config) {
		this.wsn = wsn;
		this.config = config;
	}

	@Override
	public void start() throws Exception {

		log.debug("Starting WSN service SOAP API...");

		endpoint = Endpoint.create(this);
		endpoint.setExecutor(wsnInstanceWebServiceThreadPool);

		String bindAllInterfacesUrl = System.getProperty("disableBindAllInterfacesUrl") != null ?
				config.getWebserviceEndpointUrl().toString() :
				UrlUtils.convertHostToZeros(config.getWebserviceEndpointUrl().toString());

		log.info(
				"Starting WSN service SOAP API on binding URL {} for endpoint URL {}",
				bindAllInterfacesUrl,
				config.getWebserviceEndpointUrl().toString()
		);

		endpoint.publish(bindAllInterfacesUrl);

		log.info("Started WSN service SOAP API on {}", bindAllInterfacesUrl);
	}

	@Override
	public void stop() {

		log.info("Stopping WSN service SOAP API...");

		if (endpoint != null) {
			try {
				endpoint.stop();
			} catch (NullPointerException expectedWellKnownBug) {
				// do nothing
			}
			log.info("Stopped WSN service SOAP API on {}", config.getWebserviceEndpointUrl());
		}

		ExecutorUtils.shutdown(wsnInstanceWebServiceThreadPool, 10, TimeUnit.SECONDS);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "addController",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.AddController"
	)
	@ResponseWrapper(
			localName = "addControllerResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.AddControllerResponse"
	)
	public void addController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") final String controllerEndpointUrl) {
		wsn.addController(controllerEndpointUrl);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "areNodesAlive",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.AreNodesAlive"
	)
	@ResponseWrapper(
			localName = "areNodesAliveResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.AreNodesAliveResponse"
	)
	public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes) {
		return wsn.areNodesAlive(nodes);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "destroyVirtualLink",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.DestroyVirtualLink"
	)
	@ResponseWrapper(
			localName = "destroyVirtualLinkResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.SetVirtualLinkResponse"
	)
	public String destroyVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") final String sourceNode,
									 @WebParam(name = "targetNode", targetNamespace = "") final String targetNode) {
		return wsn.destroyVirtualLink(sourceNode, targetNode);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "disableNode",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.DisableNode"
	)
	@ResponseWrapper(
			localName = "disableNodeResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.DisableNodeResponse"
	)
	public String disableNode(@WebParam(name = "node", targetNamespace = "") final String node) {
		return wsn.disableNode(node);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "disablePhysicalLink",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.DisablePhysicalLink"
	)
	@ResponseWrapper(
			localName = "disablePhysicalLinkResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.DisablePhysicalLinkResponse"
	)
	public String disablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") final String nodeA,
									  @WebParam(name = "nodeB", targetNamespace = "") final String nodeB) {
		return wsn.disablePhysicalLink(nodeA, nodeB);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "enableNode",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.EnableNode"
	)
	@ResponseWrapper(
			localName = "enableNodeResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.EnableNodeResponse"
	)
	public String enableNode(@WebParam(name = "node", targetNamespace = "") final String node) {
		return wsn.enableNode(node);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "enablePhysicalLink",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.EnablePhysicalLink"
	)
	@ResponseWrapper(
			localName = "enablePhysicalLinkResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.EnablePhysicalLinkResponse"
	)
	public String enablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") final String nodeA,
									 @WebParam(name = "nodeB", targetNamespace = "") final String nodeB) {
		return wsn.enablePhysicalLink(nodeA, nodeB);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "flashPrograms",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.FlashPrograms"
	)
	@ResponseWrapper(
			localName = "flashProgramsResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.FlashProgramsResponse"
	)
	public String flashPrograms(@WebParam(name = "nodeIds", targetNamespace = "") final List<String> nodeIds,
								@WebParam(name = "programIndices", targetNamespace = "") final
								List<Integer> programIndices,
								@WebParam(name = "programs", targetNamespace = "") final List<Program> programs) {
		return wsn.flashPrograms(nodeIds, programIndices, programs);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "getFilters",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.GetFilters"
	)
	@ResponseWrapper(
			localName = "getFiltersResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.GetFiltersResponse"
	)
	public List<String> getFilters() {
		return wsn.getFilters();
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "getNetwork",
			targetNamespace = "urn:CommonTypes",
			className = "eu.wisebed.api.common.GetNetwork"
	)
	@ResponseWrapper(
			localName = "getNetworkResponse",
			targetNamespace = "urn:CommonTypes",
			className = "eu.wisebed.api.common.GetNetworkResponse"
	)
	public String getNetwork() {
		return wsn.getNetwork();
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "getSupportedChannelHandlers",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.GetSupportedChannelHandlers"
	)
	@ResponseWrapper(
			localName = "getSupportedChannelHandlersResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.GetSupportedChannelHandlersResponse"
	)
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {
		return wsn.getSupportedChannelHandlers();
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "getVersion",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.GetVersion"
	)
	@ResponseWrapper(
			localName = "getVersionResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.GetVersionResponse"
	)
	public String getVersion() {
		return wsn.getVersion();
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "removeController",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.RemoveController"
	)
	@ResponseWrapper(
			localName = "removeControllerResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.RemoveControllerResponse"
	)
	public void removeController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") final String controllerEndpointUrl) {
		wsn.removeController(controllerEndpointUrl);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "resetNodes",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.ResetNodes"
	)
	@ResponseWrapper(
			localName = "resetNodesResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.ResetNodesResponse"
	)
	public String resetNodes(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes) {
		return wsn.resetNodes(nodes);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "send",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.Send"
	)
	@ResponseWrapper(
			localName = "sendResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.SendResponse"
	)
	public String send(@WebParam(name = "nodeIds", targetNamespace = "") final List<String> nodeIds,
					   @WebParam(name = "message", targetNamespace = "") final Message message) {
		return wsn.send(nodeIds, message);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "setChannelPipeline",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.SetChannelPipeline"
	)
	@ResponseWrapper(
			localName = "setChannelPipelineResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.SetChannelPipelineResponse"
	)
	public String setChannelPipeline(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes,
									 @WebParam(name = "channelHandlerConfigurations", targetNamespace = "") final
									 List<ChannelHandlerConfiguration> channelHandlerConfigurations) {
		return wsn.setChannelPipeline(nodes, channelHandlerConfigurations);
	}

	@Override
	@WebMethod
	@WebResult(
			targetNamespace = ""
	)
	@RequestWrapper(
			localName = "setVirtualLink",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.SetVirtualLink"
	)
	@ResponseWrapper(
			localName = "setVirtualLinkResponse",
			targetNamespace = "urn:WSNService",
			className = "eu.wisebed.api.wsn.SetVirtualLinkResponse"
	)
	public String setVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") final String sourceNode,
								 @WebParam(name = "targetNode", targetNamespace = "") final String targetNode,
								 @WebParam(name = "remoteServiceInstance", targetNamespace = "") final
								 String remoteServiceInstance,
								 @WebParam(name = "parameters", targetNamespace = "") final List<String> parameters,
								 @WebParam(name = "filters", targetNamespace = "") final List<String> filters) {
		return wsn.setVirtualLink(sourceNode, targetNode, remoteServiceInstance, parameters, filters);
	}
}
