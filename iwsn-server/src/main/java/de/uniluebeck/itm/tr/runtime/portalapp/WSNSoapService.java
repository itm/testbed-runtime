package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.UrlUtils;
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
		name = "WSN",
		endpointInterface = "eu.wisebed.api.v3.wsn.WSN",
		portName = "WSNPort",
		serviceName = "WSNService",
		targetNamespace = "http://wisebed.eu/api/v3/wsn"
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
	public void areNodesAlive(final long requestId, final List<String> nodeUrns) {
		wsn.areNodesAlive(requestId, nodeUrns);
	}

	@Override
	public void destroyVirtualLink(final long requestId, final String sourceNodeUrn, final String targetNodeUrn) {
		wsn.destroyVirtualLink(requestId, sourceNodeUrn, targetNodeUrn);
	}

	@Override
	public void disableNode(final long requestId, final String nodeUrn) {
		wsn.disableNode(requestId, nodeUrn);
	}

	@Override
	public void disablePhysicalLink(final long requestId, final String sourceNodeUrn, final String targetNodeUrn) {
		wsn.disablePhysicalLink(requestId, sourceNodeUrn, targetNodeUrn);
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
	public void enableNode(final long requestId, final String nodeUrn) {
		wsn.enableNode(requestId, nodeUrn);
	}

	@Override
	public void enablePhysicalLink(final long requestId, final String sourceNodeUrn, final String targetNodeUrn) {
		wsn.enablePhysicalLink(requestId, sourceNodeUrn, targetNodeUrn);
	}

	@Override
	public void flashPrograms(final long requestId, final List<FlashProgramsConfiguration> configurations) {
		wsn.flashPrograms(requestId, configurations);
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
	public void resetNodes(final long requestId, final List<String> nodeUrns) {
		wsn.resetNodes(requestId, nodeUrns);
	}

	@Override
	public void send(final long requestId, final List<String> nodeUrns, final byte[] message) {
		wsn.send(requestId, nodeUrns, message);
	}

	@Override
	public void setChannelPipeline(final long requestId,
								   final List<String> nodeUrns,
								   final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		wsn.setChannelPipeline(requestId, nodeUrns, channelHandlerConfigurations);
	}

	@Override
	public void setVirtualLink(final long requestId,
							   final String sourceNodeUrn,
							   final String targetNodeUrn,
							   final String remoteServiceInstance,
							   final List<String> parameters,
							   final List<String> filters) {

		wsn.setVirtualLink(requestId, sourceNodeUrn, targetNodeUrn, remoteServiceInstance, parameters, filters);
	}
}
