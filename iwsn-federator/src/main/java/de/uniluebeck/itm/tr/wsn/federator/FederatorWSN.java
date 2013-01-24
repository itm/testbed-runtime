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

package de.uniluebeck.itm.tr.wsn.federator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.federatorutils.WebservicePublisher;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;


@WebService(
		name = "WSN",
		endpointInterface = "eu.wisebed.api.v3.wsn.WSN",
		portName = "WSNPort",
		serviceName = "WSNService",
		targetNamespace = "http://wisebed.eu/api/v3/wsn"
)
public class FederatorWSN implements WSN {

	private static final Logger log = LoggerFactory.getLogger(FederatorWSN.class);

	private final ExecutorService executorService;

	private final FederatorController federatorController;

	private final WebservicePublisher<WSN> webservicePublisher;

	private final WSNPreconditions wsnPreconditions;

	private final FederationManager<WSN> federationManager;

	private final Random requestIdGenerator = new Random();

	public FederatorWSN(final FederatorController federatorController,
						final FederationManager<WSN> federationManager,
						final WebservicePublisher<WSN> webservicePublisher,
						final WSNPreconditions wsnPreconditions,
						final ExecutorService executorService) {

		this.federatorController = federatorController;
		this.webservicePublisher = webservicePublisher;
		this.federationManager = federationManager;
		this.wsnPreconditions = wsnPreconditions;
		this.executorService = executorService;
	}

	/**
	 * Starts the WSN Web Service and internal Controller Web Service endpoint.
	 *
	 * @throws Exception
	 * 		on failure
	 */
	public void start() throws Exception {
		federatorController.startAndWait();
		webservicePublisher.startAndWait();
	}

	/**
	 * Stops the WSN Web Service and internal Controller Web Service endpoint.
	 *
	 * @throws Exception
	 * 		on failure
	 */
	public void stop() throws Exception {
		webservicePublisher.stopAndWait();
		federatorController.stopAndWait();
	}

	/**
	 * Returns the endpoint URL of this WSN federator instance.
	 *
	 * @return the endpoint URL of this WSN federator instance
	 */
	public String getEndpointUrl() {
		return webservicePublisher.getEndpointUrl().toString();
	}

	@Override
	public void addController(final String controllerEndpointUrl) {

		if (!"NONE".equals(controllerEndpointUrl)) {
			log.debug("Adding controller endpoint URL {}", controllerEndpointUrl);
			federatorController.addController(controllerEndpointUrl);
		}
	}

	@Override
	public void removeController(final String controllerEndpointUrl) {

		log.debug("Removing controller endpoint URL {}", controllerEndpointUrl);
		federatorController.removeController(controllerEndpointUrl);
	}

	@Override
	public void send(final long federatorRequestId, final List<NodeUrn> nodeUrns, final byte[] messageBytes) {

		wsnPreconditions.checkSendArguments(nodeUrns, messageBytes);

		Map<WSN, List<NodeUrn>> map = federationManager.getEndpointToNodeUrnMap(nodeUrns);

		log.debug("Invoking send({}, {}) on {}", new Object[]{nodeUrns, messageBytes, map.keySet()});
		for (Map.Entry<WSN, List<NodeUrn>> entry : map.entrySet()) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = entry.getKey();
			final List<NodeUrn> nodeIdSubset = entry.getValue();

			executorService.submit(new SendRunnable(
					federatorController,
					endpoint,
					federatedRequestId,
					federatorRequestId,
					nodeIdSubset,
					messageBytes
			)
			);
		}
	}

	@Override
	public String getVersion() {
		return "3.0";
	}

	@Override
	public void areNodesAlive(final long federatorRequestId, final List<NodeUrn> nodeUrns) {

		wsnPreconditions.checkAreNodesAliveArguments(nodeUrns);

		final Map<WSN, List<NodeUrn>> map = federationManager.getEndpointToNodeUrnMap(nodeUrns);

		log.debug("Invoking areNodesAlive({}) on {}", nodeUrns, map.keySet());
		for (Map.Entry<WSN, List<NodeUrn>> entry : map.entrySet()) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = entry.getKey();
			final List<NodeUrn> nodeUrnSubset = entry.getValue();

			executorService.submit(new WSNAreNodesAliveRunnable(
					federatorController,
					endpoint,
					federatedRequestId,
					federatorRequestId,
					nodeUrnSubset
			)
			);
		}
	}

	@Override
	public List<ChannelPipelinesMap> getChannelPipelines(final List<NodeUrn> nodeUrns) {
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	public String getNetwork() {

		final BiMap<URI, Callable<String>> endpointUrlToCallableMap = HashBiMap.create();
		final Set<URI> endpointUrls = federationManager.getEndpointUrls();

		for (final URI endpointUrl : endpointUrls) {
			endpointUrlToCallableMap.put(endpointUrl, new Callable<String>() {
				@Override
				public String call() throws Exception {
					return federationManager.getEndpointByEndpointUrl(endpointUrl).getNetwork();
				}
			}
			);
		}

		return FederatorWiseMLMerger.merge(endpointUrlToCallableMap, executorService);
	}

	@Override
	public void resetNodes(final long federatorRequestId, final List<NodeUrn> nodeUrns) {

		wsnPreconditions.checkResetNodesArguments(nodeUrns);

		final Map<WSN, List<NodeUrn>> map = federationManager.getEndpointToNodeUrnMap(nodeUrns);

		log.debug("Invoking resetNodes({}) on {}", nodeUrns, map.keySet());
		for (Map.Entry<WSN, List<NodeUrn>> entry : map.entrySet()) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = entry.getKey();
			final List<NodeUrn> nodeIdSubset = entry.getValue();

			executorService.submit(new ResetNodesRunnable(
					federatorController,
					endpoint,
					federatedRequestId,
					federatorRequestId,
					nodeIdSubset
			)
			);
		}
	}


	@Override
	public void setVirtualLinks(final long requestId, final List<VirtualLink> links) {

		log.debug("FederatorWSN.setVirtualLinks({}, {})", requestId, links);

		wsnPreconditions.checkSetVirtualLinkArguments(links);

		for (VirtualLink link : links) {

			final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
			final NodeUrn targetNodeUrn = link.getTargetNodeUrn();
			final String remoteWSNServiceEndpointUrl = link.getRemoteWSNServiceEndpointUrl();
			final List<String> parameters = link.getParameters();
			final List<String> filters = link.getFilters();

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = federationManager.getEndpointByNodeUrn(sourceNodeUrn);

			log.debug("Invoking setVirtualLink({}, {}, {}, {}, {}) on {}",
					new Object[]{
							sourceNodeUrn, targetNodeUrn, remoteWSNServiceEndpointUrl, parameters, filters, endpoint
					}
			);

			executorService.submit(new SetVirtualLinkRunnable(
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
		}
	}

	@Override
	public void destroyVirtualLinks(final long requestId, final List<Link> links) {

		log.debug("FederatorWSN.destroyVirtualLinks({}, {})", requestId, links);

		wsnPreconditions.checkDestroyVirtualLinkArguments(links);

		for (Link link : links) {

			final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
			final NodeUrn targetNodeUrn = link.getTargetNodeUrn();


			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = federationManager.getEndpointByNodeUrn(sourceNodeUrn);

			log.debug(
					"Invoking destroyVirtualLink({}, {}) on {}",
					new Object[]{sourceNodeUrn, targetNodeUrn, endpoint}
			);

			executorService.submit(new DestroyVirtualLinkRunnable(
					federatorController,
					endpoint,
					federatedRequestId,
					requestId,
					sourceNodeUrn,
					targetNodeUrn
			)
			);
		}
	}

	@Override
	public void disableNodes(final long requestId, final List<NodeUrn> nodeUrns) {

		log.debug("FederatorWSN.disableNodes({}, {})", requestId, nodeUrns);

		wsnPreconditions.checkDisableNodeArguments(nodeUrns);

		for (NodeUrn nodeUrn : nodeUrns) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = federationManager.getEndpointByNodeUrn(nodeUrn);

			log.debug("Invoking disableNode({}) on {}", nodeUrn, endpoint);

			executorService.submit(new DisableNodeRunnable(
					federatorController,
					endpoint,
					federatedRequestId,
					requestId,
					nodeUrn
			)
			);
		}
	}

	@Override
	public void disablePhysicalLinks(final long requestId, final List<Link> links) {

		log.debug("FederatorWSN.disablePhysicalLinks({}, {})", requestId, links);

		wsnPreconditions.checkDisablePhysicalLinkArguments(links);


		for (Link link : links) {

			final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
			final NodeUrn targetNodeUrn = link.getTargetNodeUrn();

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = federationManager.getEndpointByNodeUrn(sourceNodeUrn);

			log.debug(
					"Invoking disablePhysicalLink({}, {}) on {}",
					new Object[]{sourceNodeUrn, targetNodeUrn, endpoint}
			);

			executorService.submit(new DisablePhysicalLinkRunnable(
					federatorController,
					endpoint,
					federatedRequestId,
					requestId,
					sourceNodeUrn,
					targetNodeUrn
			)
			);
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
	public void enableNodes(final long requestId, final List<NodeUrn> nodeUrns) {

		log.debug("FederatorWSN.enableNodes({}, {})", requestId, nodeUrns);

		wsnPreconditions.checkEnableNodeArguments(nodeUrns);

		for (NodeUrn nodeUrn : nodeUrns) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = federationManager.getEndpointByNodeUrn(nodeUrn);

			log.debug("Invoking enableNode({}) on {}", new Object[]{nodeUrn, endpoint});

			executorService.submit(new EnableNodeRunnable(
					federatorController,
					endpoint,
					federatedRequestId,
					requestId,
					nodeUrn
			)
			);
		}
	}

	@Override
	public void enablePhysicalLinks(final long requestId, final List<Link> links) {

		log.debug("FederatorWSN.enablePhysicalLinks({}, {})", requestId, links);

		wsnPreconditions.checkEnablePhysicalLinkArguments(links);

		for (Link link : links) {

			final NodeUrn sourceNodeUrn = link.getSourceNodeUrn();
			final NodeUrn targetNodeUrn = link.getTargetNodeUrn();

			final long federatedRequestId = requestIdGenerator.nextLong();
			final WSN endpoint = federationManager.getEndpointByNodeUrn(sourceNodeUrn);

			log.debug(
					"Invoking enablePhysicalLink({}, {}) on {}",
					new Object[]{sourceNodeUrn, targetNodeUrn, endpoint}
			);

			executorService.submit(new EnablePhysicalLinkRunnable(
					federatorController,
					endpoint,
					federatedRequestId,
					requestId,
					sourceNodeUrn,
					targetNodeUrn
			)
			);
		}
	}

	@Override
	public void flashPrograms(final long federatorRequestId,
							  final List<FlashProgramsConfiguration> flashProgramsConfigurations) {

		wsnPreconditions.checkFlashProgramsArguments(flashProgramsConfigurations);

		final Multimap<WSN, FlashProgramsConfiguration> federatedConfigurations = HashMultimap.create();

		for (FlashProgramsConfiguration flashProgramsConfiguration : flashProgramsConfigurations) {

			final Map<WSN, List<NodeUrn>> endpointToNodeUrnMap = federationManager.getEndpointToNodeUrnMap(
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
			executorService.submit(new FlashProgramsRunnable(
					federatorController,
					wsn,
					federatedRequestId,
					federatorRequestId,
					newArrayList(federatedConfigurations.get(wsn))
			)
			);
		}
	}

	@Override
	public void setChannelPipeline(final long federatorRequestId,
								   final List<NodeUrn> nodeUrns,
								   final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		log.debug("setChannelPipeline({}, {}) called...", nodeUrns, channelHandlerConfigurations);

		final Map<WSN, List<NodeUrn>> endpointToNodesMapping = constructEndpointToNodesMapping(nodeUrns);

		for (WSN wsnEndpoint : endpointToNodesMapping.keySet()) {

			final long federatedRequestId = requestIdGenerator.nextLong();
			executorService.submit(new SetChannelPipelineRunnable(
					federatorController,
					wsnEndpoint,
					federatedRequestId,
					federatorRequestId,
					endpointToNodesMapping.get(wsnEndpoint),
					channelHandlerConfigurations
			)
			);
		}
	}

	@Override
	public void setSerialPortParameters(final List<NodeUrn> nodeUrns, final SerialPortParameters parameters) {
		throw new RuntimeException("Not yet implemented!");
	}

	private Map<WSN, List<NodeUrn>> constructEndpointToNodesMapping(final List<NodeUrn> nodeUrns) {

		final Map<WSN, List<NodeUrn>> mapping = newHashMap();

		for (NodeUrn nodeUrn : nodeUrns) {

			final WSN endpoint = federationManager.getEndpointByNodeUrn(nodeUrn);

			List<NodeUrn> filteredNodeUrns = mapping.get(endpoint);
			if (filteredNodeUrns == null) {
				filteredNodeUrns = newArrayList();
				mapping.put(endpoint, filteredNodeUrns);
			}
			filteredNodeUrns.add(nodeUrn);
		}

		return mapping;
	}

}
