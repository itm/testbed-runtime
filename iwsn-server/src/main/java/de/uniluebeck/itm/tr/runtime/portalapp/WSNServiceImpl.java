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

import java.net.URL;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.wiseml.Wiseml;

@WebService(
		serviceName = "WSNService",
		targetNamespace = "urn:WSNService",
		portName = "WSNPort",
		endpointInterface = "eu.wisebed.api.wsn.WSN"
		)
public class WSNServiceImpl implements WSNService {

	/**
	 * The logger for this WSN service.
	 */
	private static final Logger log = LoggerFactory.getLogger(WSNServiceImpl.class);

	/**
	 * Threads from this ThreadPoolExecutor will be used to deliver messages to controllers by
	 * invoking the {@link eu.wisebed.api.controller.Controller#receive(java.util.List)} or
	 * {@link eu.wisebed.api.controller.Controller#receiveStatus(java.util.List)} method. The
	 * ThreadPoolExecutor is instantiated with at least one thread as there usually will be at least
	 * one controller and, if more controllers are attached to the running experiment the maximum
	 * thread pool size will be increased. By that, the number of threads for web-service calls is
	 * bounded by the number of controller endpoints as more threads would not, in theory, increase
	 * the throughput to the controllers.
	 */
	private final ThreadPoolExecutor wsnInstanceWebServiceThreadPool = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>(), new ThreadFactoryBuilder().setNameFormat("WSNService-WS-Thread %d").build());

	/**
	 * The endpoint of this WSN instance.
	 */
	private Endpoint wsnInstanceEndpoint;

	private final WSNService delegate;

	/**
	 * The endpoint URL of this WSN service instance.
	 */
	private final URL wsnInstanceEndpointUrl;



	public WSNServiceImpl(final String urnPrefix, final URL wsnInstanceEndpointUrl, final Wiseml wiseML,
			final String[] reservedNodes, final DeliveryManager deliveryManager, final WSNApp wsnApp) {


		Preconditions.checkNotNull(wsnInstanceEndpointUrl);
		this.wsnInstanceEndpointUrl = wsnInstanceEndpointUrl;

		delegate = new WSNServiceImplInternal(urnPrefix, wiseML, reservedNodes, deliveryManager, wsnApp);

	}

	@Override
	public void start() throws Exception {

		WSNServiceImpl.log.info("Starting WSN service...");

		wsnInstanceEndpoint = Endpoint.create(this);
		wsnInstanceEndpoint.setExecutor(wsnInstanceWebServiceThreadPool);

		final String bindAllInterfacesUrl = System.getProperty("disableBindAllInterfacesUrl") != null ? wsnInstanceEndpointUrl.toString() : UrlUtils
				.convertHostToZeros(wsnInstanceEndpointUrl.toString());

		WSNServiceImpl.log.info("Starting WSN API service on binding URL {} for endpoint URL {}", bindAllInterfacesUrl,
				wsnInstanceEndpointUrl.toString());

		wsnInstanceEndpoint.publish(bindAllInterfacesUrl);

		WSNServiceImpl.log.info("Started WSN API service wsnInstanceEndpoint on {}", bindAllInterfacesUrl);

		delegate.start();
	}

	@Override
	public void stop() {

		WSNServiceImpl.log.info("Stopping WSN service...");

		if (wsnInstanceEndpoint != null) {
			wsnInstanceEndpoint.stop();
			WSNServiceImpl.log.info("Stopped WSN service wsnInstanceEndpoint on {}", wsnInstanceEndpointUrl);
		}

		WSNServiceImpl.log.info("Stopped WSN service!");

	}

	@Override
	public String getVersion() {
		return delegate.getVersion();
	}

	@Override
	public void addController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") final String controllerEndpointUrl) {

		delegate.addController(controllerEndpointUrl);
	}

	@Override
	public void removeController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") final String controllerEndpointUrl) {

		delegate.removeController(controllerEndpointUrl);
	}

	@Override
	public String send(@WebParam(name = "nodeIds", targetNamespace = "") final List<String> nodeIds,
			@WebParam(name = "msg", targetNamespace = "") final Message message) {

		return delegate.send(nodeIds, message);

	}

	@Override
	public String setChannelPipeline(
			@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes,
			@WebParam(name = "channelHandlerConfigurations", targetNamespace = "") final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		return delegate.setChannelPipeline(nodes, channelHandlerConfigurations);
	}

	@Override
	public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodeIds) {

		return delegate.areNodesAlive(nodeIds);
	}

	@Override
	public String flashPrograms(@WebParam(name = "nodeIds", targetNamespace = "") final List<String> nodeIds,
			@WebParam(name = "programIndices", targetNamespace = "") final List<Integer> programIndices,
			@WebParam(name = "programs", targetNamespace = "") final List<Program> programs) {

		return delegate.flashPrograms(nodeIds, programIndices, programs);
	}

	@Override
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {

		return delegate.getSupportedChannelHandlers();
	}

	@Override
	public String getNetwork() {
		return delegate.getNetwork();
	}

	@Override
	public String resetNodes(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodeUrns) {
		return delegate.resetNodes(nodeUrns);
	}


	@Override
	public String setVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") final String sourceNode,
			@WebParam(name = "targetNode", targetNamespace = "") final String targetNode,
			@WebParam(name = "remoteServiceInstance", targetNamespace = "") final String remoteServiceInstance,
			@WebParam(name = "parameters", targetNamespace = "") final List<String> parameters,
			@WebParam(name = "filters", targetNamespace = "") final List<String> filters) {

		return delegate.setVirtualLink(sourceNode, targetNode, remoteServiceInstance, parameters, filters);
	}


	@Override
	public String destroyVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") final String sourceNode,
			@WebParam(name = "targetNode", targetNamespace = "") final String targetNode) {

		return delegate.destroyVirtualLink(sourceNode, targetNode);
	}

	@Override
	public String disableNode(@WebParam(name = "node", targetNamespace = "") final String node) {

		return delegate.disableNode(node);
	}

	@Override
	public String disablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") final String nodeA,
			@WebParam(name = "nodeB", targetNamespace = "") final String nodeB) {

		return delegate.disablePhysicalLink(nodeA, nodeB);

	}

	@Override
	public String enableNode(@WebParam(name = "node", targetNamespace = "") final String node) {

		return delegate.enableNode(node);

	}

	@Override
	public String enablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") final String nodeA,
			@WebParam(name = "nodeB", targetNamespace = "") final String nodeB) {

		return delegate.enablePhysicalLink(nodeA, nodeB);

	}

	@Override
	public List<String> getFilters() {
		return delegate.getFilters();
	}

}
