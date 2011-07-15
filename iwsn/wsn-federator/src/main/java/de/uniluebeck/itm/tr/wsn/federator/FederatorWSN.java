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

import com.google.common.collect.*;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.federatorutils.WebservicePublisher;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.WSNPreconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;


@WebService(
		serviceName = "WSNService",
		targetNamespace = Constants.NAMESPACE_WSN_SERVICE,
		portName = "WSNPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_WSN_SERVICE
)
public class FederatorWSN implements WSN {

	private static final Logger log = LoggerFactory.getLogger(FederatorWSN.class);

	private final ExecutorService executorService;

	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	private final FederatorController federatorController;

	private final WebservicePublisher<WSN> webservicePublisher;

	private final WSNPreconditions wsnPreconditions;

	private final FederationManager<WSN> federationManager;

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
	 * @throws Exception on failure
	 */
	public void start() throws Exception {
		federatorController.start();
		webservicePublisher.start();
	}

	/**
	 * Stops the WSN Web Service and internal Controller Web Service endpoint.
	 *
	 * @throws Exception on failure
	 */
	public void stop() throws Exception {
		webservicePublisher.stop();
		federatorController.stop();
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
	public void addController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {

		log.debug("Adding controller endpoint URL {}", controllerEndpointUrl);
		federatorController.addController(controllerEndpointUrl);
	}

	@Override
	public void removeController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {

		log.debug("Removing controller endpoint URL {}", controllerEndpointUrl);
		federatorController.removeController(controllerEndpointUrl);
	}

	@Override
	public String send(@WebParam(name = "nodeIds", targetNamespace = "") List<String> nodeIds,
					   @WebParam(name = "message", targetNamespace = "") Message message) {

		wsnPreconditions.checkSendArguments(nodeIds, message);

		String requestId = secureIdGenerator.getNextId();
		Map<WSN, List<String>> map = federationManager.getEndpointToServedUrnPrefixesMap(nodeIds);

		log.debug("Invoking send({}, {}) on {}", new Object[]{nodeIds, message, map.keySet()});
		for (Map.Entry<WSN, List<String>> entry : map.entrySet()) {

			WSN endpoint = entry.getKey();
			List<String> nodeIdSubset = entry.getValue();

			executorService.submit(new SendRunnable(federatorController, endpoint, requestId, nodeIdSubset, message));
		}

		return requestId;
	}

	@Override
	public String getVersion() {
		return Constants.VERSION;
	}

	@Override
	public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") List<String> nodes) {

		wsnPreconditions.checkAreNodesAliveArguments(nodes);

		String requestId = secureIdGenerator.getNextId();
		Map<WSN, List<String>> map = federationManager.getEndpointToServedUrnPrefixesMap(nodes);

		log.debug("Invoking areNodesAlive({}) on {}", nodes, map.keySet());
		for (Map.Entry<WSN, List<String>> entry : map.entrySet()) {

			WSN endpoint = entry.getKey();
			List<String> nodeIdSubset = entry.getValue();

			executorService
					.submit(new WSNAreNodesAliveRunnable(federatorController, endpoint, requestId, nodeIdSubset));
		}

		return requestId;
	}

	private class ProgramWrapper {

		public Program program;

		public String hashCode;

		private ProgramWrapper(Program program) {
			this.program = program;
			this.hashCode = secureIdGenerator.getNextId();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			ProgramWrapper that = (ProgramWrapper) o;

			return hashCode.equals(that.hashCode);

		}

		@Override
		public int hashCode() {
			return hashCode.hashCode();
		}

	}

	@Override
	public String flashPrograms(@WebParam(name = "nodeIds", targetNamespace = "") List<String> nodeIds,
								@WebParam(name = "programIndices", targetNamespace = "") List<Integer> programIndices,
								@WebParam(name = "programs", targetNamespace = "") List<Program> programs) {

		wsnPreconditions.checkFlashProgramsArguments(nodeIds, programIndices, programs);

		String requestId = secureIdGenerator.getNextId();
		Map<WSN, List<String>> map = federationManager.getEndpointToServedUrnPrefixesMap(nodeIds);

		//BiMap of node id <-> program (the helper class ProgramWrapper is used since Program has no hashCode)
		BiMap<String, ProgramWrapper> programsMap = createFlashProgramsMap(nodeIds, programIndices, programs);

		log.debug("Invoking flashPrograms({}, ...) on {}", nodeIds, map.keySet());
		for (Map.Entry<WSN, List<String>> entry : map.entrySet()) {

			WSN endpoint = entry.getKey();

			//The subset of nodes that is handled by the federated instance
			List<String> subsetNodeIds = entry.getValue();

			//The filtered map from above containing only the entries for the current set of node ids
			BiMap<String, ProgramWrapper> subsetProgramsMap = filterFlashProgramsMap(subsetNodeIds, programsMap);

			//A list of programs containing only the ones that are used by the current set of node ids
			List<ProgramWrapper> subsetProgramWrappers = new ArrayList<ProgramWrapper>(subsetProgramsMap.values());

			//Now we assign the corresponding index of a program to the node. The index of the program is written
			//in this array list at the same index where the node id is located in the subsetNodeIds Array List
			Integer[] subsetProgramIndices = new Integer[subsetNodeIds.size()];

			for (int subsetNodeIdIndex = 0; subsetNodeIdIndex < subsetNodeIds.size(); subsetNodeIdIndex++) {
				FederatorWSN.ProgramWrapper nodeProgram = subsetProgramsMap.get(subsetNodeIds.get(subsetNodeIdIndex));
				int nodeProgramIndex = subsetProgramWrappers.indexOf(nodeProgram);
				subsetProgramIndices[subsetNodeIdIndex] = nodeProgramIndex;
			}

			//Convert the ProgramWrappers list to a Program list (necessary since Program has no hashCode)
			Program[] subsetPrograms = new Program[subsetProgramWrappers.size()];
			for (int i = 0; i < subsetProgramWrappers.size(); i++) {
				subsetPrograms[i] = subsetProgramWrappers.get(i).program;
			}

			//Submit the sub-job
			executorService.submit(
					new FlashProgramsRunnable(
							federatorController,
							endpoint,
							requestId,
							subsetNodeIds,
							Arrays.asList(subsetProgramIndices),
							Arrays.asList(subsetPrograms)
					)
			);

		}

		return requestId;
	}

	@Override
	public String getNetwork() {

		final BiMap<String, Callable<String>> endpointUrlToCallableMap = HashBiMap.create();
		final Set<String> endpointUrls = federationManager.getEndpointUrls();

		for (final String endpointUrl : endpointUrls) {
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
	public String resetNodes(@WebParam(name = "nodes", targetNamespace = "") List<String> nodes) {
		wsnPreconditions.checkResetNodesArguments(nodes);

		String requestId = secureIdGenerator.getNextId();
		Map<WSN, List<String>> map = federationManager.getEndpointToServedUrnPrefixesMap(nodes);

		log.debug("Invoking resetNodes({}) on {}", nodes, map.keySet());
		for (Map.Entry<WSN, List<String>> entry : map.entrySet()) {

			WSN endpoint = entry.getKey();
			List<String> nodeIdSubset = entry.getValue();

			executorService.submit(new ResetNodesRunnable(federatorController, endpoint, requestId, nodeIdSubset));
		}

		return requestId;
	}

	@Override
	public String setVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") String sourceNodeUrn,
								 @WebParam(name = "targetNode", targetNamespace = "") String targetNode,
								 @WebParam(name = "remoteServiceInstance", targetNamespace = "")
								 String remoteServiceInstance,
								 @WebParam(name = "parameters", targetNamespace = "") List<String> parameters,
								 @WebParam(name = "filters", targetNamespace = "") List<String> filters) {

		wsnPreconditions.checkSetVirtualLinkArguments(
				sourceNodeUrn,
				targetNode,
				remoteServiceInstance,
				parameters,
				filters
		);

		String requestId = secureIdGenerator.getNextId();
		WSN endpoint = federationManager.getEndpointByNodeUrn(sourceNodeUrn);

		log.debug("Invoking setVirtualLink({}, {}, {}, {}, {}) on {}",
				new Object[]{sourceNodeUrn, targetNode, remoteServiceInstance, parameters, filters, endpoint}
		);
		executorService.submit(
				new SetVirtualLinkRunnable(
						federatorController,
						endpoint,
						requestId,
						sourceNodeUrn,
						targetNode,
						remoteServiceInstance,
						parameters,
						filters
				)
		);

		return requestId;
	}

	@Override
	public String destroyVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") String sourceNodeUrn,
									 @WebParam(name = "targetNode", targetNamespace = "") String targetNode) {

		wsnPreconditions.checkDestroyVirtualLinkArguments(sourceNodeUrn, targetNode);

		String requestId = secureIdGenerator.getNextId();
		WSN endpoint = federationManager.getEndpointByNodeUrn(sourceNodeUrn);

		log.debug("Invoking destroyVirtualLink({}, {}) on {}", new Object[]{sourceNodeUrn, targetNode, endpoint});
		executorService.submit(
				new DestroyVirtualLinkRunnable(
						federatorController,
						endpoint,
						requestId,
						sourceNodeUrn,
						targetNode
				)
		);

		return requestId;
	}

	@Override
	public String disableNode(@WebParam(name = "node", targetNamespace = "") String nodeUrn) {

		wsnPreconditions.checkDisableNodeArguments(nodeUrn);

		String requestId = secureIdGenerator.getNextId();
		WSN endpoint = federationManager.getEndpointByNodeUrn(nodeUrn);

		log.debug("Invoking disableNode({}) on {}", nodeUrn, endpoint);
		executorService.submit(
				new DisableNodeRunnable(
						federatorController,
						endpoint,
						requestId,
						nodeUrn
				)
		);

		return requestId;
	}

	@Override
	public String disablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") String nodeUrnA,
									  @WebParam(name = "nodeB", targetNamespace = "") String nodeUrnB) {

		wsnPreconditions.checkDisablePhysicalLinkArguments(nodeUrnA, nodeUrnB);

		String requestId = secureIdGenerator.getNextId();
		WSN endpoint = federationManager.getEndpointByNodeUrn(nodeUrnA);

		log.debug("Invoking disablePhysicalLink({}, {}) on {}", new Object[]{nodeUrnA, nodeUrnB, endpoint});
		executorService.submit(
				new DisablePhysicalLinkRunnable(
						federatorController,
						endpoint,
						requestId,
						nodeUrnA,
						nodeUrnB
				)
		);

		return requestId;
	}

	@Override
	public String enableNode(@WebParam(name = "node", targetNamespace = "") String nodeUrn) {

		wsnPreconditions.checkEnableNodeArguments(nodeUrn);

		String requestId = secureIdGenerator.getNextId();
		WSN endpoint = federationManager.getEndpointByNodeUrn(nodeUrn);

		log.debug("Invoking enableNode({}) on {}", new Object[]{nodeUrn, endpoint});
		executorService.submit(
				new EnableNodeRunnable(
						federatorController,
						endpoint,
						requestId,
						nodeUrn
				)
		);

		return requestId;
	}

	@Override
	public String enablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") String nodeUrnA,
									 @WebParam(name = "nodeB", targetNamespace = "") String nodeUrnB) {

		wsnPreconditions.checkEnablePhysicalLinkArguments(nodeUrnA, nodeUrnB);

		String requestId = secureIdGenerator.getNextId();
		WSN endpoint = federationManager.getEndpointByNodeUrn(nodeUrnA);

		log.debug("Invoking enablePhysicalLink({}, {}) on {}", new Object[]{nodeUrnA, nodeUrnB, endpoint});
		executorService.submit(
				new EnablePhysicalLinkRunnable(
						federatorController,
						endpoint,
						requestId,
						nodeUrnA,
						nodeUrnB
				)
		);

		return requestId;
	}

	@Override
	public List<String> getFilters() {

		ImmutableSet<String> endpointUrls = federationManager.getEndpointUrls();
		Map<String, Future<ImmutableSet<String>>> endpointUrlToResultsMapping = Maps.newHashMap();

		// fork calls to endpoints
		log.debug("Invoking getFilters() on {}", endpointUrls);
		for (final String endpointUrl : endpointUrls) {
			Future<ImmutableSet<String>> future = executorService.submit(new Callable<ImmutableSet<String>>() {
				@Override
				public ImmutableSet<String> call() throws Exception {
					WSN endpoint = federationManager.getEndpointByEndpointUrl(endpointUrl);
					return ImmutableSet.copyOf(endpoint.getFilters());
				}
			}
			);
			endpointUrlToResultsMapping.put(endpointUrl, future);
		}

		// join results from endpoints
		ImmutableSet<String> intersectedFilters = null;
		for (Map.Entry<String, Future<ImmutableSet<String>>> entry : endpointUrlToResultsMapping.entrySet()) {

			try {

				ImmutableSet<String> endpointFilters = entry.getValue().get();

				if (intersectedFilters == null) {
					intersectedFilters = endpointFilters;
				} else {
					intersectedFilters = ImmutableSet.copyOf(Sets.intersection(intersectedFilters, endpointFilters));
				}

			} catch (Exception e) {
				log.error("Error while calling getFilters() on federated WSN endpoint \"{}\". Ignoring this endpoint.",
						entry.getKey()
				);
			}
		}

		return Lists.newArrayList(intersectedFilters);
	}

	@Override
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {

		log.debug("getSupportedChannelHandlers() called...");

		final ImmutableSet<FederationManager.Entry<WSN>> entries = federationManager.getEntries();
		final Map<FederationManager.Entry<WSN>, Future<List<ChannelHandlerDescription>>> entryToResultMapping =
				Maps.newHashMap();

		// fork calls to endpoints
		for (final FederationManager.Entry<WSN> entry : entries) {
			final Future<List<ChannelHandlerDescription>> future = executorService.submit(
					new GetSupportedChannelHandlersCallable(entry.endpoint)
			);
			entryToResultMapping.put(entry, future);
		}

		final Set<ChannelHandlerDescription> commonHandlers = newTreeSet(CHANNEL_HANDLER_DESCRIPTION_COMPARATOR);

		for (Map.Entry<FederationManager.Entry<WSN>, Future<List<ChannelHandlerDescription>>> outerEntry : entryToResultMapping
				.entrySet()) {

			try {

				List<ChannelHandlerDescription> outerChannelHandlers = outerEntry.getValue().get();

				for (ChannelHandlerDescription outerChannelHandler : outerChannelHandlers) {

					boolean containedInAllOthers = true;

					for (Map.Entry<FederationManager.Entry<WSN>, Future<List<ChannelHandlerDescription>>> innerEntry : entryToResultMapping
							.entrySet()) {

						if (innerEntry != outerEntry) {

							boolean outerContainedInInnerEntry = false;

							final List<ChannelHandlerDescription> innerChannelHandlers = innerEntry.getValue().get();
							for (ChannelHandlerDescription innerChannelHandler : innerChannelHandlers) {
								if (equals(outerChannelHandler, innerChannelHandler)) {
									outerContainedInInnerEntry = true;
									break;
								}
							}

							if (!outerContainedInInnerEntry) {
								containedInAllOthers = false;
								break;
							}
						}
					}

					if (containedInAllOthers) {
						commonHandlers.add(outerChannelHandler);
					}
				}

			} catch (Exception e) {
				log.error("Error while calling getFilters() on federated WSN endpoint \"{}\". Ignoring this endpoint.",
						outerEntry.getKey()
				);
			}
		}

		return newArrayList(commonHandlers);
	}

	private static final Comparator<ChannelHandlerDescription> CHANNEL_HANDLER_DESCRIPTION_COMPARATOR = new Comparator<ChannelHandlerDescription>() {
		@Override
		public int compare(final ChannelHandlerDescription o1, final ChannelHandlerDescription o2) {
			return FederatorWSN.equals(o1, o2) ? 0 : -1;
		}
	};

	private static boolean equals(final ChannelHandlerDescription outerChannelHandler,
						   final ChannelHandlerDescription innerChannelHandler) {

		if (!outerChannelHandler.getName().equals(innerChannelHandler.getName())) {
			return false;
		}

		Set<String> outerConfigurationKeys = newHashSet();
		Set<String> innerConfigurationKeys = newHashSet();

		for (KeyValuePair keyValuePair : outerChannelHandler.getConfigurationOptions()) {
			outerConfigurationKeys.add(keyValuePair.getKey());
		}

		for (KeyValuePair keyValuePair : innerChannelHandler.getConfigurationOptions()) {
			innerConfigurationKeys.add(keyValuePair.getKey());
		}

		return Sets.symmetricDifference(outerConfigurationKeys, innerConfigurationKeys).size() == 0;
	}

	@Override
	public String setChannelPipeline(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes,
									 @WebParam(name = "channelHandlerConfigurations", targetNamespace = "") final
									 List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		log.debug("setChannelPipeline({}, {}) called...", nodes, channelHandlerConfigurations);

		final String federatorRequestId = secureIdGenerator.getNextId();
		final Map<WSN, List<String>> endpointToNodesMapping = constructEndpointToNodesMapping(nodes);

		for (WSN wsnEndpoint : endpointToNodesMapping.keySet()) {

			final SetChannelPipelineRunnable setChannelPipelineRunnable =
					new SetChannelPipelineRunnable(
							federatorController,
							wsnEndpoint,
							federatorRequestId,
							endpointToNodesMapping.get(wsnEndpoint),
							channelHandlerConfigurations
					);

			executorService.submit(setChannelPipelineRunnable);
		}

		return federatorRequestId;
	}

	private Map<WSN, List<String>> constructEndpointToNodesMapping(final List<String> nodes) {

		final Map<WSN, List<String>> mapping = newHashMap();

		for (String node : nodes) {

			final WSN endpoint = federationManager.getEndpointByNodeUrn(node);

			List<String> filteredNodeUrns = mapping.get(endpoint);
			if (filteredNodeUrns == null) {
				filteredNodeUrns = newArrayList();
				mapping.put(endpoint, filteredNodeUrns);
			}
			filteredNodeUrns.add(node);

		}

		return mapping;
	}

	private BiMap<String, ProgramWrapper> filterFlashProgramsMap(List<String> subsetNodeIds,
																 BiMap<String, ProgramWrapper> programsMap) {

		BiMap<String, ProgramWrapper> retMap = HashBiMap.create(subsetNodeIds.size());
		for (Map.Entry<String, ProgramWrapper> entry : programsMap.entrySet()) {
			if (subsetNodeIds.contains(entry.getKey())) {
				retMap.put(entry.getKey(), entry.getValue());
			}
		}
		return retMap;
	}

	private BiMap<String, ProgramWrapper> createFlashProgramsMap(List<String> nodeIds, List<Integer> programIndices,
																 List<Program> programs) {

		BiMap<String, ProgramWrapper> retMap = HashBiMap.create(nodeIds.size());
		for (int i = 0; i < nodeIds.size(); i++) {
			retMap.put(nodeIds.get(i), new ProgramWrapper(programs.get(programIndices.get(i))));
		}
		return retMap;

	}

}
