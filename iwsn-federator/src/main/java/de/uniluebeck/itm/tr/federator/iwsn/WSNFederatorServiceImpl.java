/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.PreconditionsFactory;
import de.uniluebeck.itm.tr.common.WSNPreconditions;
import de.uniluebeck.itm.tr.federator.iwsn.async.*;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerTestbedClientController;
import de.uniluebeck.itm.util.SecureIdGenerator;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.controller.RequestStatus;
import eu.wisebed.api.v3.controller.SingleNodeRequestStatus;
import eu.wisebed.api.v3.wsn.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.util.concurrent.Futures.addCallback;
import static eu.wisebed.wiseml.WiseMLHelper.serialize;


@WebService(
		name = "WSN",
		endpointInterface = "eu.wisebed.api.v3.wsn.WSN",
		portName = "WSNPort",
		serviceName = "WSNService",
		targetNamespace = "http://wisebed.eu/api/v3/wsn"
)
public class WSNFederatorServiceImpl extends AbstractService implements WSNFederatorService {

	private static final Logger log = LoggerFactory.getLogger(WSNFederatorServiceImpl.class);

	private static final String UTF_8 = "UTF-8";

	private final ListeningExecutorService executorService;

	private final ServicePublisher servicePublisher;

	private final FederatorController federatorController;

	private final WSNPreconditions wsnPreconditions;

	private final FederatedEndpoints<WSN> wsnFederatedEndpoints;

	private final Random requestIdGenerator = new Random();

	private final URI endpointUri;

	private ServicePublisherService jaxWsService;

	@Inject
	public WSNFederatorServiceImpl(final ServicePublisher servicePublisher,
								   final IWSNFederatorServiceConfig config,
								   final ListeningExecutorService executorService,
								   final PreconditionsFactory preconditionsFactory,
								   final SecureIdGenerator secureIdGenerator,
								   @Assisted final FederatorController federatorController,
								   @Assisted final FederatedEndpoints<WSN> wsnFederatedEndpoints,
								   @Assisted final Set<NodeUrnPrefix> nodeUrnPrefixes,
								   @Assisted final Set<NodeUrn> nodeUrns) {
		checkArgument(
				config.getFederatorWsnEndpointUriBase() != null && !""
						.equals(config.getFederatorWsnEndpointUriBase().toString()),
				"Configuration parameter " + IWSNFederatorServiceConfig.FEDERATOR_WSN_ENDPOINT_URI_BASE + " must be set"
		);
		assert config.getFederatorWsnEndpointUriBase() != null;

		this.servicePublisher = servicePublisher;
		this.federatorController = federatorController;
		this.wsnPreconditions = preconditionsFactory.createWsnPreconditions(nodeUrnPrefixes, nodeUrns);

		String uriString;
		uriString = config.getFederatorWsnEndpointUriBase().toString();
		uriString += uriString.endsWith("/") ? "" : "/";
		uriString += secureIdGenerator.getNextId();
		this.endpointUri = URI.create(uriString);

		this.wsnFederatedEndpoints = wsnFederatedEndpoints;
		this.executorService = executorService;
	}

	@Override
	protected void doStart() {
		log.trace("WSNFederatorServiceImpl.doStart()");
		try {
			jaxWsService = servicePublisher.createJaxWsService(endpointUri.getPath(), this);
			jaxWsService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("WSNFederatorServiceImpl.doStop()");
		try {
			if (jaxWsService.isRunning()) {
				jaxWsService.stopAndWait();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	/**
	 * Returns the endpoint URL of this WSN federator instance.
	 *
	 * @return the endpoint URL of this WSN federator instance
	 */
	public String getEndpointUrl() {
		return endpointUri.toString();
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "addController", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.AddController")
	@ResponseWrapper(localName = "addControllerResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.AddControllerResponse")
	public void addController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "")
			String controllerEndpointUrl)
			throws AuthorizationFault {
		log.debug("Adding controller endpoint URL {}", controllerEndpointUrl);
		federatorController.addController(createDeliveryManagerController(controllerEndpointUrl));
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "removeController", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.RemoveController")
	@ResponseWrapper(localName = "removeControllerResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.RemoveControllerResponse")
	public void removeController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "")
			String controllerEndpointUrl)
			throws AuthorizationFault {
		log.debug("Removing controller endpoint URL {}", controllerEndpointUrl);
		federatorController.removeController(createDeliveryManagerController(controllerEndpointUrl));
	}

	@WebMethod
	@RequestWrapper(localName = "send", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.Send")
	@ResponseWrapper(localName = "sendResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SendResponse")
	public void send(
			@WebParam(name = "requestId", targetNamespace = "")
			long federatorRequestId,
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns,
			@WebParam(name = "message", targetNamespace = "")
			byte[] messageBytes)
			throws AuthorizationFault, ReservationNotRunningFault_Exception {

		log.trace("WSNFederatorServiceImpl.send({}, {}, {})", federatorRequestId, nodeUrns, messageBytes);
		wsnPreconditions.checkSendArguments(nodeUrns, messageBytes);

		Map<WSN, List<NodeUrn>> map = wsnFederatedEndpoints.getEndpointToNodeUrnMap(nodeUrns);

		log.debug("Invoking send({}, {}) on {}", nodeUrns, messageBytes, map.keySet());
		for (Map.Entry<WSN, List<NodeUrn>> entry : map.entrySet()) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = entry.getKey();
			final List<NodeUrn> nodeIdSubset = entry.getValue();

			final ListenableFuture<Void> listenableFuture = executorService.submit(new SendCallable(
					federatorController,
					endpoint,
					federatedRequestId,
					federatorRequestId,
					nodeIdSubset,
					messageBytes
			)
			);

			addErrorHandling(listenableFuture, federatorRequestId, nodeIdSubset);

		}

	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "areNodesAlive", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.AreNodesAlive")
	@ResponseWrapper(localName = "areNodesAliveResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.AreNodesAliveResponse")
	public void areNodesAlive(
			@WebParam(name = "requestId", targetNamespace = "")
			long federatorRequestId,
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns)
			throws AuthorizationFault, ReservationNotRunningFault_Exception {

		log.trace("WSNFederatorServiceImpl.areNodesAlive({}, {})", federatorRequestId, nodeUrns);
		wsnPreconditions.checkAreNodesAliveArguments(nodeUrns);

		final Map<WSN, List<NodeUrn>> map = wsnFederatedEndpoints.getEndpointToNodeUrnMap(nodeUrns);

		log.debug("Invoking areNodesAlive({}) on {}", nodeUrns, map.keySet());
		for (Map.Entry<WSN, List<NodeUrn>> entry : map.entrySet()) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = entry.getKey();
			final List<NodeUrn> nodeUrnSubset = entry.getValue();

			final ListenableFuture<Void> listenableFuture = executorService.submit(new WSNAreNodesAliveCallable(
					federatorController,
					endpoint,
					federatedRequestId,
					federatorRequestId,
					nodeUrnSubset
			)
			);

			addErrorHandling(listenableFuture, federatorRequestId, nodeUrnSubset);

		}
	}


	@Override
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(localName = "getChannelPipelines", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.GetChannelPipelines")
	@ResponseWrapper(localName = "getChannelPipelinesResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.GetChannelPipelinesResponse")
	public List<ChannelPipelinesMap> getChannelPipelines(
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns)
			throws AuthorizationFault, ReservationNotRunningFault_Exception {
		log.trace("WSNFederatorServiceImpl.getChannelPipelines({})", nodeUrns);
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(localName = "getNetwork", targetNamespace = "http://wisebed.eu/api/v3/common",
			className = "eu.wisebed.api.v3.common.GetNetwork")
	@ResponseWrapper(localName = "getNetworkResponse", targetNamespace = "http://wisebed.eu/api/v3/common",
			className = "eu.wisebed.api.v3.common.GetNetworkResponse")
	public String getNetwork() throws AuthorizationFault {

		log.trace("WSNFederatorServiceImpl.getNetwork()");
		final BiMap<URI, Callable<String>> endpointUrlToCallableMap = HashBiMap.create();
		final Set<URI> endpointUrls = wsnFederatedEndpoints.getEndpointUrls();

		for (final URI endpointUrl : endpointUrls) {
			endpointUrlToCallableMap.put(endpointUrl, new Callable<String>() {
				@Override
				public String call() throws Exception {
					return wsnFederatedEndpoints.getEndpointByEndpointUrl(endpointUrl).getNetwork();
				}
			}
			);
		}

		return serialize(FederatorWiseMLMerger.merge(endpointUrlToCallableMap, executorService));
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "resetNodes", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.ResetNodes")
	@ResponseWrapper(localName = "resetNodesResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.ResetNodesResponse")
	public void resetNodes(
			@WebParam(name = "requestId", targetNamespace = "")
			long federatorRequestId,
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns)
			throws AuthorizationFault, ReservationNotRunningFault_Exception {

		log.trace("WSNFederatorServiceImpl.resetNodes({}, {})", federatorRequestId, nodeUrns);
		wsnPreconditions.checkResetNodesArguments(nodeUrns);

		final Map<WSN, List<NodeUrn>> map = wsnFederatedEndpoints.getEndpointToNodeUrnMap(nodeUrns);

		log.debug("Invoking resetNodes({}) on {}", nodeUrns, map.keySet());
		for (Map.Entry<WSN, List<NodeUrn>> entry : map.entrySet()) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = entry.getKey();
			final List<NodeUrn> nodeIdSubset = entry.getValue();

			final ListenableFuture<Void> listenableFuture = executorService.submit(new ResetNodesCallable(
					federatorController,
					endpoint,
					federatedRequestId,
					federatorRequestId,
					nodeIdSubset
			)
			);

			addErrorHandling(listenableFuture, federatorRequestId, nodeIdSubset);
		}
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "enableVirtualLinks", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnableVirtualLinks")
	@ResponseWrapper(localName = "enableVirtualLinksResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnableVirtualLinksResponse")
	public void enableVirtualLinks(
			@WebParam(name = "requestId", targetNamespace = "")
			long requestId,
			@WebParam(name = "links", targetNamespace = "")
			List<VirtualLink> links)
			throws AuthorizationFault, ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {

		log.trace("WSNFederatorServiceImpl.enableVirtualLinks({}, {})", requestId, links);
		wsnPreconditions.checkSetVirtualLinkArguments(links);

		for (VirtualLink link : links) {

			final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
			final NodeUrn targetNodeUrn = link.getTargetNodeUrn();
			final String remoteWSNServiceEndpointUrl = link.getRemoteWSNServiceEndpointUrl();
			final List<String> parameters = link.getParameters();
			final List<String> filters = link.getFilters();

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = wsnFederatedEndpoints.getEndpointByNodeUrn(sourceNodeUrn);

			log.debug("Invoking setVirtualLink({}, {}, {}, {}, {}) on {}",
					sourceNodeUrn, targetNodeUrn, remoteWSNServiceEndpointUrl, parameters, filters, endpoint
			);

			final ListenableFuture<Void> listenableFuture = executorService.submit(new EnableVirtualLinkCallable(
					federatorController,
					endpoint,
					federatedRequestId,
					requestId,
					sourceNodeUrn,
					targetNodeUrn,
					remoteWSNServiceEndpointUrl,
					parameters,
					filters
			)
			);

			addErrorHandling(listenableFuture, requestId, newArrayList(sourceNodeUrn));


		}
	}


	@Override
	@WebMethod
	@RequestWrapper(localName = "disableVirtualLinks", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisableVirtualLinks")
	@ResponseWrapper(localName = "disableVirtualLinksResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisableVirtualLinksResponse")
	public void disableVirtualLinks(
			@WebParam(name = "requestId", targetNamespace = "")
			long requestId,
			@WebParam(name = "links", targetNamespace = "")
			List<Link> links)
			throws AuthorizationFault, ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {

		log.trace("WSNFederatorServiceImpl.disableVirtualLinks({}, {})", requestId, links);
		wsnPreconditions.checkDestroyVirtualLinkArguments(links);

		for (Link link : links) {

			final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
			final NodeUrn targetNodeUrn = link.getTargetNodeUrn();


			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = wsnFederatedEndpoints.getEndpointByNodeUrn(sourceNodeUrn);

			log.debug(
					"Invoking destroyVirtualLink({}, {}) on {}",
					sourceNodeUrn, targetNodeUrn, endpoint
			);

			final ListenableFuture<Void> listenableFuture = executorService.submit(new DisableVirtualLinkCallable(
					federatorController,
					endpoint,
					federatedRequestId,
					requestId,
					sourceNodeUrn,
					targetNodeUrn
			)
			);

			addErrorHandling(listenableFuture, requestId, newArrayList(sourceNodeUrn));
		}
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "disableNodes", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisableNodes")
	@ResponseWrapper(localName = "disableNodesResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisableNodesResponse")
	public void disableNodes(
			@WebParam(name = "requestId", targetNamespace = "")
			long requestId,
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns)
			throws AuthorizationFault, ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {

		log.trace("WSNFederatorServiceImpl.disableNodes({}, {})", requestId, nodeUrns);
		wsnPreconditions.checkDisableNodeArguments(nodeUrns);

		for (NodeUrn nodeUrn : nodeUrns) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = wsnFederatedEndpoints.getEndpointByNodeUrn(nodeUrn);

			log.debug("Invoking disableNode({}) on {}", nodeUrn, endpoint);

			final ListenableFuture<Void> listenableFuture = executorService.submit(new DisableNodeCallable(
					federatorController,
					endpoint,
					federatedRequestId,
					requestId,
					nodeUrn
			)
			);
			addErrorHandling(listenableFuture, requestId, newArrayList(nodeUrn));
		}
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "disablePhysicalLinks", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisablePhysicalLinks")
	@ResponseWrapper(localName = "disablePhysicalLinksResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisablePhysicalLinksResponse")
	public void disablePhysicalLinks(
			@WebParam(name = "requestId", targetNamespace = "")
			long requestId,
			@WebParam(name = "links", targetNamespace = "")
			List<Link> links)
			throws AuthorizationFault, ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {

		log.trace("WSNFederatorServiceImpl.disablePhysicalLinks({}, {})", requestId, links);
		wsnPreconditions.checkDisablePhysicalLinkArguments(links);


		for (Link link : links) {

			final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
			final NodeUrn targetNodeUrn = link.getTargetNodeUrn();

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = wsnFederatedEndpoints.getEndpointByNodeUrn(sourceNodeUrn);

			log.debug(
					"Invoking disablePhysicalLink({}, {}) on {}",
					sourceNodeUrn, targetNodeUrn, endpoint
			);

			final ListenableFuture<Void> listenableFuture = executorService.submit(new DisablePhysicalLinkCallable(
					federatorController,
					endpoint,
					federatedRequestId,
					requestId,
					sourceNodeUrn,
					targetNodeUrn
			)
			);

			addErrorHandling(listenableFuture, requestId, newArrayList(targetNodeUrn));
		}
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "disableVirtualization", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisableVirtualization")
	@ResponseWrapper(localName = "disableVirtualizationResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisableVirtualizationResponse")
	public void disableVirtualization()
			throws AuthorizationFault, ReservationNotRunningFault_Exception, VirtualizationNotSupportedFault_Exception {
		log.trace("WSNFederatorServiceImpl.disableVirtualization()");
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "enableVirtualization", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnableVirtualization")
	@ResponseWrapper(localName = "enableVirtualizationResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnableVirtualizationResponse")
	public void enableVirtualization()
			throws AuthorizationFault, ReservationNotRunningFault_Exception, VirtualizationNotSupportedFault_Exception {
		log.trace("WSNFederatorServiceImpl.enableVirtualization()");
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "enableNodes", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnableNodes")
	@ResponseWrapper(localName = "enableNodesResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnableNodesResponse")
	public void enableNodes(
			@WebParam(name = "requestId", targetNamespace = "")
			long requestId,
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns)
			throws AuthorizationFault, ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {

		log.trace("WSNFederatorServiceImpl.enableNodes({}, {})", requestId, nodeUrns);
		wsnPreconditions.checkEnableNodeArguments(nodeUrns);

		for (NodeUrn nodeUrn : nodeUrns) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = wsnFederatedEndpoints.getEndpointByNodeUrn(nodeUrn);

			log.debug("Invoking enableNode({}) on {}", new Object[]{nodeUrn, endpoint});

			final ListenableFuture<Void> listenableFuture = executorService.submit(new EnableNodeCallable(
					federatorController,
					endpoint,
					federatedRequestId,
					requestId,
					nodeUrn
			)
			);

			addErrorHandling(listenableFuture, requestId, newArrayList(nodeUrn));
		}
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "enablePhysicalLinks", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnablePhysicalLinks")
	@ResponseWrapper(localName = "enablePhysicalLinksResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnablePhysicalLinksResponse")
	public void enablePhysicalLinks(
			@WebParam(name = "requestId", targetNamespace = "")
			long requestId,
			@WebParam(name = "links", targetNamespace = "")
			List<Link> links)
			throws AuthorizationFault, ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {

		log.trace("WSNFederatorServiceImpl.enablePhysicalLinks({}, {})", requestId, links);
		wsnPreconditions.checkEnablePhysicalLinkArguments(links);

		for (Link link : links) {

			final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
			final NodeUrn targetNodeUrn = link.getTargetNodeUrn();

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = wsnFederatedEndpoints.getEndpointByNodeUrn(sourceNodeUrn);

			log.debug(
					"Invoking enablePhysicalLink({}, {}) on {}",
					sourceNodeUrn, targetNodeUrn, endpoint
			);

			final ListenableFuture<Void> listenableFuture = executorService.submit(new EnablePhysicalLinkCallable(
					federatorController,
					endpoint,
					federatedRequestId,
					requestId,
					sourceNodeUrn,
					targetNodeUrn
			)
			);

			addErrorHandling(listenableFuture, requestId, newArrayList(targetNodeUrn));
		}
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "flashPrograms", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.FlashPrograms")
	@ResponseWrapper(localName = "flashProgramsResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.FlashProgramsResponse")
	public void flashPrograms(
			@WebParam(name = "requestId", targetNamespace = "")
			long federatorRequestId,
			@WebParam(name = "configurations", targetNamespace = "")
			List<FlashProgramsConfiguration> flashProgramsConfigurations)
			throws AuthorizationFault, ReservationNotRunningFault_Exception {

		log.trace("WSNFederatorServiceImpl.flashPrograms({}, {})", federatorRequestId, flashProgramsConfigurations);
		wsnPreconditions.checkFlashProgramsArguments(flashProgramsConfigurations);

		final Multimap<WSN, FlashProgramsConfiguration> federatedConfigurations = HashMultimap.create();

		for (FlashProgramsConfiguration flashProgramsConfiguration : flashProgramsConfigurations) {

			final Map<WSN, List<NodeUrn>> endpointToNodeUrnMap = wsnFederatedEndpoints.getEndpointToNodeUrnMap(
					flashProgramsConfiguration.getNodeUrns()
			);

			for (Map.Entry<WSN, List<NodeUrn>> entry : endpointToNodeUrnMap.entrySet()) {

				final FlashProgramsConfiguration federatedConfiguration = new FlashProgramsConfiguration();
				federatedConfiguration.setProgram(flashProgramsConfiguration.getProgram());
				federatedConfiguration.getNodeUrns().addAll(entry.getValue());

				federatedConfigurations.put(entry.getKey(), federatedConfiguration);
			}
		}


		for (final WSN wsn : federatedConfigurations.keySet()) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final List<FlashProgramsConfiguration> flashProgramsConfigurationsForWsn =
					newArrayList(federatedConfigurations.get(wsn));
			final ListenableFuture<Void> listenableFuture = executorService.submit(new FlashProgramsCallable(
					federatorController,
					wsn,
					federatedRequestId,
					federatorRequestId,
					flashProgramsConfigurationsForWsn
			)
			);

			List<NodeUrn> nodeUrnCollection = new ArrayList<NodeUrn>();
			for (FlashProgramsConfiguration flashProgramsConfiguration : flashProgramsConfigurationsForWsn) {
				nodeUrnCollection.addAll(flashProgramsConfiguration.getNodeUrns());
			}
			addErrorHandling(listenableFuture, federatorRequestId, nodeUrnCollection);

		}
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "setChannelPipeline", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SetChannelPipeline")
	@ResponseWrapper(localName = "setChannelPipelineResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SetChannelPipelineResponse")
	public void setChannelPipeline(
			@WebParam(name = "requestId", targetNamespace = "")
			long federatorRequestId,
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns,
			@WebParam(name = "channelHandlerConfigurations", targetNamespace = "")
			List<ChannelHandlerConfiguration> channelHandlerConfigurations)
			throws AuthorizationFault, ReservationNotRunningFault_Exception {

		log.trace("WSNFederatorServiceImpl.setChannelPipeline({}, {}, {})",
				federatorRequestId, nodeUrns, channelHandlerConfigurations
		);
		final Map<WSN, List<NodeUrn>> endpointToNodesMapping = constructEndpointToNodesMapping(nodeUrns);

		for (WSN wsnEndpoint : endpointToNodesMapping.keySet()) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final List<NodeUrn> nodeUrnsForWsnEndpoint = endpointToNodesMapping.get(wsnEndpoint);
			final ListenableFuture<Void> listenableFuture = executorService.submit(new SetChannelPipelineCallable(
					federatorController,
					wsnEndpoint,
					federatedRequestId,
					federatorRequestId,
					nodeUrnsForWsnEndpoint,
					channelHandlerConfigurations
			)
			);

			addErrorHandling(listenableFuture, federatorRequestId, nodeUrnsForWsnEndpoint);

		}
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "setSerialPortParameters", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SetSerialPortParameters")
	@ResponseWrapper(localName = "setSerialPortParametersResponse", targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SetSerialPortParametersResponse")
	public void setSerialPortParameters(
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns,
			@WebParam(name = "parameters", targetNamespace = "")
			SerialPortParameters parameters)
			throws AuthorizationFault, ReservationNotRunningFault_Exception {
		log.trace("WSNFederatorServiceImpl.setSerialPortParameters({}, {})", nodeUrns, parameters);
		throw new RuntimeException("Not yet implemented!");
	}

	private Map<WSN, List<NodeUrn>> constructEndpointToNodesMapping(final List<NodeUrn> nodeUrns) {

		final Map<WSN, List<NodeUrn>> mapping = newHashMap();

		for (NodeUrn nodeUrn : nodeUrns) {

			final WSN endpoint = wsnFederatedEndpoints.getEndpointByNodeUrn(nodeUrn);

			List<NodeUrn> filteredNodeUrns = mapping.get(endpoint);
			if (filteredNodeUrns == null) {
				filteredNodeUrns = newArrayList();
				mapping.put(endpoint, filteredNodeUrns);
			}
			filteredNodeUrns.add(nodeUrn);
		}

		return mapping;
	}

	/**
	 * Adds a listener to the provided ListenableFuture which will create and forward a status request to the
	 * federation controller if the future cannot complete due to an Exception.
	 *
	 * @param listenableFuture
	 * 		The future which Exceptions are to be caught.
	 * @param requestId
	 * 		The request identifier provided by a client
	 * @param nodeUrns
	 * 		a list of node urns
	 */
	private void addErrorHandling(final ListenableFuture<Void> listenableFuture, final long requestId,
								  final List<NodeUrn> nodeUrns) {
		addCallback(listenableFuture, new FutureCallback<Void>() {
			@Override
			public void onSuccess(final Void result) {
				// the result is ignored since this is about catching exceptions only
			}

			@Override
			public void onFailure(final Throwable t) {
				log.error(t.getMessage(), t);

				StringBuilder sb = new StringBuilder(
						"An exception occurred calling WSNFederatorService#send using federatorRequestId '"
				);
				sb.append(requestId).append("': ").append(t.getCause()).append(t.getMessage())
						.append("\r\n").append(getStackTraceAsString(t));

				RequestStatus requestStatus = new RequestStatus();
				requestStatus.setRequestId(requestId);
				final List<SingleNodeRequestStatus> statusList = requestStatus.getSingleNodeRequestStatus();
				for (NodeUrn nodeUrn : nodeUrns) {
					SingleNodeRequestStatus status = new SingleNodeRequestStatus();
					status.setMsg(sb.toString());
					status.setValue(-1);
					status.setNodeUrn(nodeUrn);
					status.setCompleted(true);
					status.setSuccess(false);
					statusList.add(status);
				}
				federatorController.receiveStatus(newArrayList(requestStatus));
			}
		}
		);
	}


	@Override
	@WebMethod(exclude = true)
	public FederatorController getWsnFederatorController() {
		return federatorController;
	}

	@Override
	@WebMethod(exclude = true)
	public URI getEndpointUri() {
		return endpointUri;
	}

	private DeliveryManagerTestbedClientController createDeliveryManagerController(final String controllerEndpointUrl) {
		return new DeliveryManagerTestbedClientController(
				WisebedServiceHelper.getControllerService(controllerEndpointUrl, executorService),
				controllerEndpointUrl
		);
	}
}
