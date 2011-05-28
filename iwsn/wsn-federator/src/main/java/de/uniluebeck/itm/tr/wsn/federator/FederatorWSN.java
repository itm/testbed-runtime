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

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.itm.uniluebeck.tr.wiseml.merger.WiseMLMergerHelper;
import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.uniluebeck.itm.tr.util.*;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.WSNPreconditions;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.Endpoint;
import java.util.*;
import java.util.concurrent.*;


@WebService(
		serviceName = "WSNService",
		targetNamespace = Constants.NAMESPACE_WSN_SERVICE,
		portName = "WSNPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_WSN_SERVICE
)
public class FederatorWSN implements WSN {

	private static final Logger log = LoggerFactory.getLogger(FederatorWSN.class);

	private final ScheduledExecutorService executorService =
			Executors.newScheduledThreadPool(1,
					new ThreadFactoryBuilder().setNameFormat("FederatorWSN-Thread %d").build()
			);

	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	private final FederatorController federatorController;

	private final String wsnEndpointUrl;

	private final WSNPreconditions preconditions;

	private Endpoint wsnEndpoint;

	private final FederationManager<WSN> federationManager;

	public FederatorWSN(String wsnEndpointUrl, String controllerEndpointUrl,
						ImmutableMap<String, ImmutableSet<String>> federatedEndpointUrlsToUrnPrefixesMap) {

		this.wsnEndpointUrl = wsnEndpointUrl;
		this.federatorController = new FederatorController(controllerEndpointUrl);

		this.federationManager = new FederationManager<WSN>(new Function<String, WSN>() {
			@Override
			public WSN apply(final String input) {
				return WSNServiceHelper.getWSNService(input);
			}
		}, federatedEndpointUrlsToUrnPrefixesMap
		);

		this.preconditions = new WSNPreconditions();
		for (Set<String> federatedEndpointUrnPrefixes : federatedEndpointUrlsToUrnPrefixesMap.values()) {
			this.preconditions.addServedUrnPrefixes(
					federatedEndpointUrnPrefixes.toArray(new String[federatedEndpointUrnPrefixes.size()])
			);
		}
	}

	/**
	 * Starts the WSN Web Service and internal Controller Web Service endpoint.
	 *
	 * @throws Exception on failure
	 */
	public void start() throws Exception {
		federatorController.start();

		String bindAllInterfacesUrl = UrlUtils.convertHostToZeros(wsnEndpointUrl);

		log.debug("Starting WSN federator...");
		log.debug("Endpoint URL: {}", wsnEndpointUrl);
		log.debug("Binding  URL: {}", bindAllInterfacesUrl);

		wsnEndpoint = Endpoint.publish(bindAllInterfacesUrl, this);

		log.debug("Successfully started WSN federator on {}!", bindAllInterfacesUrl);
	}

	/**
	 * Stops the WSN Web Service and internal Controller Web Service endpoint.
	 *
	 * @throws Exception on failure
	 */
	public void stop() throws Exception {

		if (wsnEndpoint != null) {
			wsnEndpoint.stop();
			log.info("Stopped WSN federator on {}", wsnEndpointUrl);
		}

		ExecutorUtils.shutdown(executorService, 5, TimeUnit.SECONDS);

		log.info("Stopped WSN federator at {}", wsnEndpointUrl);

		federatorController.stop();

	}

	/**
	 * Returns the WSN endpoint URL of this federator instance.
	 *
	 * @return the WSN endpoint URL of this federator instance
	 */
	String getWsnEndpointUrl() {
		return wsnEndpointUrl;
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

		preconditions.checkSendArguments(nodeIds, message);

		String requestId = secureIdGenerator.getNextId();
		Map<WSN, List<String>> map = federationManager.getEndpointToServedUrnPrefixesMap(nodeIds);

		log.debug("Invoking send({}, {}) on {}", new Object[] {nodeIds, message, map.keySet()});
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

		preconditions.checkAreNodesAliveArguments(nodes);

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

		preconditions.checkFlashProgramsArguments(nodeIds, programIndices, programs);

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

	@Override
	public String getNetwork() {

		List<String> networkStrings = new ArrayList<String>();

		// fork getNetwork() calls to federated testbeds
		ImmutableSet<String> endpointUrls = federationManager.getEndpointUrls();

		log.debug("Invoking getNetwork() on {}", endpointUrls);

		List<Tuple<String, Future<String>>> futures = Lists.newArrayList();

		for (final String federatedEndpointUrl : endpointUrls) {
			Future<String> future = executorService.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					WSN federatedEndpoint = federationManager.getEndpointByEndpointUrl(federatedEndpointUrl);
					return federatedEndpoint.getNetwork();
				}
			}
			);
			futures.add(new Tuple<String, Future<String>>(federatedEndpointUrl, future));
		}

		// join getNetwork() calls
		for (Tuple<String, Future<String>> future : futures) {
			try {
				networkStrings.add(future.getSecond().get());
			} catch (Exception e) {
				log.error(
						"Error calling getNetwork() on federated WSN endpoint {}. "
								+ "Ignoring this endpoint on merging WiseML documents. Reason: {}",
						future.getFirst(), e
				);
			}
		}

		// Merger configuration (default)
		MergerConfiguration config = new MergerConfiguration();

		// return merged network definitions
		try {
			return WiseMLMergerHelper.mergeFromStrings(config, networkStrings);
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String resetNodes(@WebParam(name = "nodes", targetNamespace = "") List<String> nodes) {
		preconditions.checkResetNodesArguments(nodes);

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

		preconditions.checkSetVirtualLinkArguments(
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

		preconditions.checkDestroyVirtualLinkArguments(sourceNodeUrn, targetNode);

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

		preconditions.checkDisableNodeArguments(nodeUrn);

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

		preconditions.checkDisablePhysicalLinkArguments(nodeUrnA, nodeUrnB);

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

		preconditions.checkEnableNodeArguments(nodeUrn);

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

		preconditions.checkEnablePhysicalLinkArguments(nodeUrnA, nodeUrnB);

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

		/*
		ImmutableSet<String> endpointUrls = federationManager.getEndpointUrls();
		Map<String, Future<List<ChannelHandlerDescription>>> endpointUrlToResultsMapping = Maps.newHashMap();

		// fork calls to endpoints
		for (final String endpointUrl : endpointUrls) {

			Future<List<ChannelHandlerDescription>> future =
					executorService.submit(new Callable<List<ChannelHandlerDescription>>() {
						@Override
						public List<ChannelHandlerDescription> call() throws Exception {
							WSN endpoint = federationManager.getEndpointByEndpointUrl(endpointUrl);
							return endpoint.getSupportedChannelHandlers();
						}
					}
					);
			endpointUrlToResultsMapping.put(endpointUrl, future);
		}

		// join results from endpoints
		// calculate the list of ChannelHandlerDescriptions that are supported by every federated WSN endpoint
		// two ChannelHandlerDescription are equal if their names match and all keys of their configurationOptions match

		Comparator<ChannelHandlerDescription> channelHandlerDescriptionComparator =
				new Comparator<ChannelHandlerDescription>() {
					@Override
					public int compare(final ChannelHandlerDescription o1, final ChannelHandlerDescription o2) {

						int nameComparison = o1.getName().compareTo(o2.getName());

						// name is equal, so compare configurationOptions
						if (nameComparison == 0) {

							Set<String> configurationKeys1 = Sets.newHashSet();
							for (KeyValuePair keyValuePair : o1.getConfigurationOptions()) {
								configurationKeys1.add(keyValuePair.getKey());
							}

							Set<String> configurationKeys2 = Sets.newHashSet();
							for (KeyValuePair keyValuePair : o2.getConfigurationOptions()) {
								configurationKeys2.add(keyValuePair.getKey());
							}

							boolean equalConfigurationOptions = Sets.symmetricDifference(
									configurationKeys1,
									configurationKeys2
							).size() == 0;

							return equalConfigurationOptions ? 0 : -1;

						}

						return nameComparison;
					}
				};



		ImmutableSet<String> intersectedFilterNames = null;
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
		*/

		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	public String setChannelPipeline(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes,
									 @WebParam(name = "channelHandlerConfigurations", targetNamespace = "") final
									 List<ChannelHandlerConfiguration> channelHandlerConfigurations) {
		log.debug("setChannelPipeline({}, {}) called...", nodes, channelHandlerConfigurations);
		throw new RuntimeException("Not yet implemented!");
	}

}
