package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.wsn.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@WebService(
		serviceName = "WSNService",
		targetNamespace = "urn:WSNService",
		portName = "WSNPort",
		endpointInterface = "eu.wisebed.api.v3.wsn.WSN"
)
public class WSNSoapService extends AbstractService implements WSN, Service {

	private static final Logger log = LoggerFactory.getLogger(WSNSoapService.class);

	private final WSNService wsn;

	private final WSNServiceConfig config;

	/**
	 * Threads from this ThreadPoolExecutor will be used to deliver messages to controllers by invoking the {@link
	 * eu.wisebed.api.v3.controller.Controller#receive(java.util.List)} or {@link eu.wisebed.api.v3.controller.Controller#receiveStatus(java.util.List)}
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
	protected void doStart() {

		try {

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
			notifyStarted();

		} catch (MalformedURLException e) {
			notifyFailed(e);
		}

	}

	@Override
	protected void doStop() {

		try {

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

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	@Override
	public void addController(final String controllerEndpointUrl) {
		wsn.addController(controllerEndpointUrl);
	}

	@Override
	public String areNodesAlive(final List<String> nodeUrns) {
		return wsn.areNodesAlive(nodeUrns);
	}

	@Override
	public String destroyVirtualLink(final String sourceNodeUrn, final String targetNodeUrn) {
		return wsn.destroyVirtualLink(sourceNodeUrn, targetNodeUrn);
	}

	@Override
	public String disableNode(final String nodeUrn) {
		return wsn.disableNode(nodeUrn);
	}

	@Override
	public String disablePhysicalLink(final String sourceNodeUrn, final String targetNodeUrn) {
		return wsn.disablePhysicalLink(sourceNodeUrn, targetNodeUrn);
	}

	@Override
	public void disableVirtualization() throws VirtualizationNotSupported_Exception {
		wsn.disableVirtualization();
	}

	@Override
	public void enableVirtualization() throws VirtualizationNotSupported_Exception {
		wsn.enableVirtualization();
	}

	@Override
	public String enableNode(final String nodeUrn) {
		return wsn.enableNode(nodeUrn);
	}

	@Override
	public String enablePhysicalLink(final String sourceNodeUrn, final String targetNodeUrn) {
		return wsn.enablePhysicalLink(sourceNodeUrn, targetNodeUrn);
	}

	@Override
	public String flashPrograms(final List<FlashProgramsConfiguration> configurations) {
		return wsn.flashPrograms(configurations);
	}

	@Override
	public List<ChannelPipelinesMap> getChannelPipelines(final List<String> nodeUrns) {
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	public String getNetwork() {
		return wsn.getNetwork();
	}

	@Override
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {
		return wsn.getSupportedChannelHandlers();
	}

	@Override
	public List<String> getSupportedVirtualLinkFilters() {
		return wsn.getSupportedVirtualLinkFilters();
	}

	@Override
	public String getVersion() {
		return wsn.getVersion();
	}

	@Override
	public void removeController(final String controllerEndpointUrl) {
		wsn.removeController(controllerEndpointUrl);
	}

	@Override
	public String resetNodes(final List<String> nodeUrns) {
		return wsn.resetNodes(nodeUrns);
	}

	@Override
	public String send(final List<String> nodeUrns, final Message message) {
		return wsn.send(nodeUrns, message);
	}

	@Override
	public String setChannelPipeline(final List<String> nodeUrns,
									 final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		return wsn.setChannelPipeline(nodeUrns, channelHandlerConfigurations);
	}

	@Override
	public String setVirtualLink(final String sourceNodeUrn,
								 final String targetNodeUrn,
								 final String remoteServiceInstance,
								 final List<String> parameters,
								 final List<String> filters) {

		return wsn.setVirtualLink(sourceNodeUrn, targetNodeUrn, remoteServiceInstance, parameters, filters);
	}
}
