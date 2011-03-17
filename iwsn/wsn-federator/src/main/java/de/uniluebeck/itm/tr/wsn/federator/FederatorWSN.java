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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.itm.uniluebeck.tr.wiseml.merger.WiseMLMergerHelper;
import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.uniluebeck.itm.tr.util.*;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.WSNPreconditions;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v22.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
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

	/**
	 * Federated WSN instance endpoint URL -> Set<URN Prefixes>
	 */
	private final Map<String, Set<String>> prefixSet = new HashMap<String, Set<String>>();

	/**
	 * Maps between node URNs and the WSN endpoint that serves them.
	 */
	private final TimedCache<String, WSN> nodeUrnEndpointMapping = new TimedCache<String, WSN>(10, TimeUnit.MINUTES);

	/**
	 * Maps between node URN prefixes and the WSN endpoint that serves them.
	 */
	private final TimedCache<String, WSN> nodeUrnPrefixEndpointMapping =
			new TimedCache<String, WSN>(10, TimeUnit.MINUTES);

	private final ScheduledExecutorService executorService =
			Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("FederatorWSN-Thread %d").build());

	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	private final FederatorController federatorController;

	private final String wsnEndpointUrl;

	private final WSNPreconditions preconditions;

	private Endpoint wsnEndpoint;

	public FederatorWSN(String wsnEndpointUrl, String controllerEndpointUrl) {
		this.wsnEndpointUrl = wsnEndpointUrl;
		this.federatorController = new FederatorController(controllerEndpointUrl);
		this.preconditions = new WSNPreconditions();
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

	public void addFederatedWSNEndpoint(String federatedWSNEndpointUrl, Set<String> federatedUrnPrefixSet) {
		synchronized (prefixSet) {
			prefixSet.put(federatedWSNEndpointUrl, federatedUrnPrefixSet);
		}
		preconditions.addServedUrnPrefixes(federatedUrnPrefixSet.toArray(new String[federatedUrnPrefixSet.size()]));
	}

	/**
	 * For a given list of node IDs calculate a mapping between federated WSN endpoint URLs and the corresponding list of
	 * node IDs.
	 *
	 * @param nodeIds list of node IDs
	 *
	 * @return see above
	 */
	private Map<WSN, List<String>> calculateEndpointNodeIdsMapping(List<String> nodeIds) {

		Map<WSN, List<String>> retMap = new HashMap<WSN, List<String>>();

		for (String nodeId : nodeIds) {

			WSN endpoint = getWSNEndpoint(nodeId);

			List<String> nodeIdList = retMap.get(endpoint);
			if (nodeIdList == null) {
				nodeIdList = new ArrayList<String>();
				retMap.put(endpoint, nodeIdList);
			}
			nodeIdList.add(nodeId);

		}

		return retMap;
	}

	private WSN getWSNEndpoint(String nodeId) {
		synchronized (prefixSet) {
			synchronized (nodeUrnEndpointMapping) {
				synchronized (nodeUrnPrefixEndpointMapping) {

					// check for existing endpoint instance
					WSN endpoint = nodeUrnEndpointMapping.get(nodeId);

					// create on if it does not exist
					if (endpoint == null) {

						for (Map.Entry<String, Set<String>> entry : prefixSet.entrySet()) {
							for (String urnPrefix : entry.getValue()) {

								if (nodeId.startsWith(urnPrefix)) {

									// check if we already an endpoint created for that URN prefix
									endpoint = nodeUrnPrefixEndpointMapping.get(urnPrefix);

									if (endpoint == null) {
										endpoint = WSNServiceHelper.getWSNService(entry.getKey());
										nodeUrnPrefixEndpointMapping.put(urnPrefix, endpoint);
									}

									nodeUrnEndpointMapping.put(nodeId, endpoint);

								}
							}
						}

					}
					return endpoint;
				}
			}
		}
	}

	private WSN calculateEndpointUrl(String nodeId) {
		ArrayList<String> list = new ArrayList<String>();
		list.add(nodeId);
		return calculateEndpointNodeIdsMapping(list).keySet().iterator().next();
	}

	String getWsnEndpointUrl() {
		return wsnEndpointUrl;
	}

	private static abstract class AbstractRequestRunnable implements Runnable {

		private FederatorController federatorController;

		protected final WSN wsnEndpoint;

		protected final String federatorRequestId;

		protected AbstractRequestRunnable(FederatorController federatorController, WSN wsnEndpoint,
										  String federatorRequestId) {

			this.federatorController = federatorController;
			this.wsnEndpoint = wsnEndpoint;
			this.federatorRequestId = federatorRequestId;
		}

		protected void done(String federatedRequestId) {
			federatorController.addRequestIdMapping(federatedRequestId, federatorRequestId);
		}
	}

	// =================================================================================================================

	private static class SendRunnable extends AbstractRequestRunnable {

		private List<String> nodeIds;

		private Message message;

		private SendRunnable(FederatorController federatorController, WSN wsnEndpoint, String federatorRequestId,
							 List<String> nodeIds, Message message) {

			super(federatorController, wsnEndpoint, federatorRequestId);

			this.nodeIds = nodeIds;
			this.message = message;
		}

		@Override
		public void run() {
			// instance wsnEndpoint is potentially not thread-safe!!!
			synchronized (wsnEndpoint) {
				done(wsnEndpoint.send(nodeIds, message));
			}
		}

	}

	@Override
	public void addController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {

		federatorController.addController(controllerEndpointUrl);
	}

	@Override
	public void removeController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {

		federatorController.removeController(controllerEndpointUrl);
	}

	@Override
	public String send(@WebParam(name = "nodeIds", targetNamespace = "") List<String> nodeIds,
					   @WebParam(name = "message", targetNamespace = "") Message message) {

		preconditions.checkSendArguments(nodeIds, message);

		String requestId = secureIdGenerator.getNextId();
		Map<WSN, List<String>> map = calculateEndpointNodeIdsMapping(nodeIds);

		log.debug("Invoking send on {}", map.keySet());
		for (Map.Entry<WSN, List<String>> entry : map.entrySet()) {

			WSN endpoint = entry.getKey();
			List<String> nodeIdSubset = entry.getValue();

			executorService.submit(new SendRunnable(federatorController, endpoint, requestId, nodeIdSubset, message));
		}

		return requestId;
	}

	// =================================================================================================================

	@Override
	public String getVersion() {
		return Constants.VERSION;
	}

	// =================================================================================================================

	private static class AreNodesAliveRunnable extends AbstractRequestRunnable {

		private List<String> nodes;

		private AreNodesAliveRunnable(FederatorController federatorController, WSN wsnEndpoint,
									  String federatorRequestId,
									  List<String> nodes) {
			super(federatorController, wsnEndpoint, federatorRequestId);
			this.nodes = nodes;
		}

		@Override
		public void run() {
			// instance wsnEndpoint is potentially not thread-safe!!!
			synchronized (wsnEndpoint) {
				done(wsnEndpoint.areNodesAlive(nodes));
			}
		}
	}

	@Override
	public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") List<String> nodes) {

		preconditions.checkAreNodesAliveArguments(nodes);

		String requestId = secureIdGenerator.getNextId();
		Map<WSN, List<String>> map = calculateEndpointNodeIdsMapping(nodes);

		log.debug("Invoking areNodesAlive on {}", map.keySet());
		for (Map.Entry<WSN, List<String>> entry : map.entrySet()) {

			WSN endpoint = entry.getKey();
			List<String> nodeIdSubset = entry.getValue();

			executorService.submit(new AreNodesAliveRunnable(federatorController, endpoint, requestId, nodeIdSubset));
		}

		return requestId;
	}

	// =================================================================================================================

	private static class FlashProgramsRunnable extends AbstractRequestRunnable {

		private List<String> nodeIds;

		private List<Integer> programIndices;

		private List<Program> programs;

		private FlashProgramsRunnable(FederatorController federatorController, WSN wsnEndpoint,
									  String federatorRequestId,
									  List<String> nodeIds, List<Integer> programIndices, List<Program> programs) {

			super(federatorController, wsnEndpoint, federatorRequestId);

			this.nodeIds = nodeIds;
			this.programIndices = programIndices;
			this.programs = programs;
		}

		@Override
		public void run() {
			// instance wsnEndpoint is potentially not thread-safe!!!
			synchronized (wsnEndpoint) {
				done(wsnEndpoint.flashPrograms(nodeIds, programIndices, programs));
			}
		}

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
		Map<WSN, List<String>> map = calculateEndpointNodeIdsMapping(nodeIds);

		//BiMap of node id <-> program (the helper class ProgramWrapper is used since Program has no hashCode)
		BiMap<String, ProgramWrapper> programsMap = createFlashProgramsMap(nodeIds, programIndices, programs);

		log.debug("Invoking flashPrograms on {}", map.keySet());
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
			executorService.submit(new FlashProgramsRunnable(federatorController, endpoint, requestId, subsetNodeIds,
					Arrays.asList(subsetProgramIndices), Arrays.asList(subsetPrograms)
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

	// =================================================================================================================

	@Override
	public String getNetwork() {

		List<String> networkStrings = new ArrayList<String>();

		// fork getNetwork() calls to federated testbeds
		Set<String> federatedWsnEndpointUrls = prefixSet.keySet();

		List<Future<String>> futures = new ArrayList<Future<String>>(federatedWsnEndpointUrls.size());
		for (final String wsnEndpointURL : federatedWsnEndpointUrls) {
			futures.add(executorService.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					return WSNServiceHelper.getWSNService(wsnEndpointURL).getNetwork();
				}
			}
			));
		}

		// join getNetwork() calls
		for (Future<String> future : futures) {
			try {
				networkStrings.add(future.get());
			} catch (Exception e) {
				throw new RuntimeException(e);
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

	// =================================================================================================================

	private static class ResetNodesRunnable extends AbstractRequestRunnable {

		private List<String> nodes;

		private ResetNodesRunnable(FederatorController federatorController, WSN wsnEndpoint, String federatorRequestId,
								   List<String> nodes) {

			super(federatorController, wsnEndpoint, federatorRequestId);

			this.nodes = nodes;
		}

		@Override
		public void run() {
			// instance wsnEndpoint is potentially not thread-safe!!!
			synchronized (wsnEndpoint) {
				done(wsnEndpoint.resetNodes(nodes));
			}
		}
	}

	@Override
	public String resetNodes(@WebParam(name = "nodes", targetNamespace = "") List<String> nodes) {
		preconditions.checkResetNodesArguments(nodes);

		String requestId = secureIdGenerator.getNextId();
		Map<WSN, List<String>> map = calculateEndpointNodeIdsMapping(nodes);

		for (Map.Entry<WSN, List<String>> entry : map.entrySet()) {
			log.debug("Invoking resetNodes for nodes {} on {}", StringUtils.toString(entry.getValue()), entry.getKey());

			WSN endpoint = entry.getKey();
			List<String> nodeIdSubset = entry.getValue();

			executorService.submit(new ResetNodesRunnable(federatorController, endpoint, requestId, nodeIdSubset));
		}

		return requestId;
	}

	// =================================================================================================================

	private static class SetVirtualLinkRunnable extends AbstractRequestRunnable {

		private String sourceNode;

		private String targetNode;

		private String remoteServiceInstance;

		private List<String> parameters;

		private List<String> filters;

		private SetVirtualLinkRunnable(FederatorController federatorController, WSN wsnEndpoint,
									   String federatorRequestId,
									   String sourceNode, String targetNode, String remoteServiceInstance,
									   List<String> parameters,
									   List<String> filters) {

			super(federatorController, wsnEndpoint, federatorRequestId);

			this.sourceNode = sourceNode;
			this.targetNode = targetNode;
			this.remoteServiceInstance = remoteServiceInstance;
			this.parameters = parameters;
			this.filters = filters;
		}

		@Override
		public void run() {
			// instance wsnEndpoint is potentially not thread-safe!!!
			synchronized (wsnEndpoint) {
				done(wsnEndpoint.setVirtualLink(sourceNode, targetNode, remoteServiceInstance, parameters, filters));
			}
		}
	}

	@Override
	public String setVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") String sourceNode,
								 @WebParam(name = "targetNode", targetNamespace = "") String targetNode,
								 @WebParam(name = "remoteServiceInstance", targetNamespace = "")
								 String remoteServiceInstance,
								 @WebParam(name = "parameters", targetNamespace = "") List<String> parameters,
								 @WebParam(name = "filters", targetNamespace = "") List<String> filters) {

		preconditions.checkSetVirtualLinkArguments(sourceNode, targetNode, remoteServiceInstance, parameters, filters);

		String requestId = secureIdGenerator.getNextId();
		WSN endpoint = calculateEndpointUrl(sourceNode);

		log.debug("Invoking setVirtualLink on {}", endpoint);
		executorService
				.submit(new SetVirtualLinkRunnable(federatorController, endpoint, requestId, sourceNode, targetNode,
						remoteServiceInstance, parameters, filters
				)
				);

		return requestId;
	}

	// =================================================================================================================

	private static class DestroyVirtualLinkRunnable extends AbstractRequestRunnable {

		private String sourceNode;

		private String targetNode;

		private DestroyVirtualLinkRunnable(FederatorController federatorController, WSN wsnEndpoint,
										   String federatorRequestId,
										   String sourceNode, String targetNode) {

			super(federatorController, wsnEndpoint, federatorRequestId);

			this.sourceNode = sourceNode;
			this.targetNode = targetNode;
		}

		@Override
		public void run() {
			// instance wsnEndpoint is potentially not thread-safe!!!
			synchronized (wsnEndpoint) {
				done(wsnEndpoint.destroyVirtualLink(sourceNode, targetNode));
			}
		}
	}

	@Override
	public String destroyVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") String sourceNode,
									 @WebParam(name = "targetNode", targetNamespace = "") String targetNode) {

		preconditions.checkDestroyVirtualLinkArguments(sourceNode, targetNode);

		String requestId = secureIdGenerator.getNextId();
		WSN endpoint = calculateEndpointUrl(sourceNode);

		log.debug("Invoking destroyVirtualLink on {}", endpoint);
		executorService
				.submit(new DestroyVirtualLinkRunnable(federatorController, endpoint, requestId, sourceNode, targetNode
				)
				);

		return requestId;
	}

	// *****************************************************************************************************************

	@Override
	public String defineNetwork(@WebParam(name = "newNetwork", targetNamespace = "") String newNetwork) {

		preconditions.checkDefineNetworkArguments(newNetwork);

		throw new RuntimeException("Operation not implemented.");
	}

	@Override
	public String describeCapabilities(@WebParam(name = "capability", targetNamespace = "") String capability)
			throws UnsupportedOperationException_Exception {

		preconditions.checkDescribeCapabilitiesArguments(capability);

		throw new RuntimeException("Operation not implemented.");
	}

	@Override
	public String disableNode(@WebParam(name = "node", targetNamespace = "") String node) {

		preconditions.checkDisableNodeArguments(node);

		throw new RuntimeException("Operation not implemented.");
	}

	@Override
	public String disablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") String nodeA,
									  @WebParam(name = "nodeB", targetNamespace = "") String nodeB) {

		preconditions.checkDisablePhysicalLinkArguments(nodeA, nodeB);

		throw new RuntimeException("Operation not implemented.");
	}

	@Override
	public String enableNode(@WebParam(name = "node", targetNamespace = "") String node) {

		preconditions.checkEnableNodeArguments(node);

		throw new RuntimeException("Operation not implemented.");
	}

	@Override
	public String enablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") String nodeA,
									 @WebParam(name = "nodeB", targetNamespace = "") String nodeB) {

		preconditions.checkEnablePhysicalLinkArguments(nodeA, nodeB);

		throw new RuntimeException("Operation not implemented.");
	}

	@Override
	public List<String> getFilters() {
		throw new RuntimeException("Operation not implemented.");
	}

	@Override
	public List<String> getNeighbourhood(@WebParam(name = "node", targetNamespace = "") String node)
			throws UnknownNodeUrnException_Exception {

		preconditions.checkGetNeighbourhoodArguments(node);

		throw new RuntimeException("Operation not implemented.");
	}

	@Override
	public String getPropertyValueOf(@WebParam(name = "node", targetNamespace = "") String node,
									 @WebParam(name = "propertyName", targetNamespace = "") String propertyName)
			throws UnknownNodeUrnException_Exception {

		preconditions.checkGetPropertyValueOfArguments(node, propertyName);

		throw new RuntimeException("Operation not implemented.");
	}

	@Override
	public String setStartTime(@WebParam(name = "time", targetNamespace = "") XMLGregorianCalendar time) {

		preconditions.checkSetStartTimeArguments(time);

		throw new RuntimeException("Operation not implemented.");
	}

}
