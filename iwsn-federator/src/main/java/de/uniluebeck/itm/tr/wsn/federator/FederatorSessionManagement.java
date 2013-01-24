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
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.federatorutils.WebservicePublisher;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementHelper;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import de.uniluebeck.itm.tr.util.TimedCache;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.rs.ReservationNotFoundFault_Exception;
import eu.wisebed.api.v3.sm.*;
import eu.wisebed.api.v3.wsn.WSN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;

@WebService(
		name = "SessionManagement",
		endpointInterface = "eu.wisebed.api.v3.sm.SessionManagement",
		portName = "SessionManagementPort",
		serviceName = "SessionManagementService",
		targetNamespace = "http://wisebed.eu/api/v3/sm"
)
public class FederatorSessionManagement implements SessionManagement {

	/**
	 * The logger instance for this Session Management federator instance.
	 */
	private static final Logger log = LoggerFactory.getLogger(FederatorSessionManagement.class);

	/**
	 * The {@link java.util.concurrent.ExecutorService} instance that is used to run jobs in parallel.
	 */
	private final ExecutorService executorService = Executors.newCachedThreadPool(
			new ThreadFactoryBuilder().setNameFormat("FederatorSessionManagement-Thread %d").build()
	);

	/**
	 * wsnInstanceHash (see {@link de.uniluebeck.itm.tr.iwsn.common.SessionManagementHelper#calculateWSNInstanceHash(java.util.List)}
	 * ) -> Federating WSN API instance
	 */
	private final TimedCache<String, Tuple<FederatorWSN, ExecutorService>> instanceCache =
			new TimedCache<String, Tuple<FederatorWSN, ExecutorService>>();

	/**
	 * Used for generating random request IDs and URLs.
	 */
	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	/**
	 * The actual Web service endpoint of this SessionManagement federator instance.
	 */
	private Endpoint sessionManagementEndpoint;

	/**
	 * Preconditions instance to check method arguments sent by user.
	 */
	private final SessionManagementPreconditions preconditions;

	/**
	 * FederatorController instance managing asynchronous replies for {@link SessionManagement#areNodesAlive(long,
	 * java.util.List, String)}.
	 */
	private final FederatorController federatorController;

	private final FederatorWSNConfig config;

	private final Random requestIdGenerator = new Random();

	private final FederationManager<SessionManagement> federationManager;

	public FederatorSessionManagement(
			final FederationManager<SessionManagement> federationManager,
			final SessionManagementPreconditions preconditions,
			final FederatorController federatorController,
			final FederatorWSNConfig config) {

		this.federationManager = federationManager;
		this.preconditions = preconditions;
		this.federatorController = federatorController;
		this.config = config;
	}

	public void start() throws Exception {

		log.debug("Starting Session Management federator on endpoint URL {}...", config.getFederatorSmEndpointURL());

		federatorController.startAndWait();
		sessionManagementEndpoint = Endpoint.publish(config.getFederatorSmEndpointURL().toString(), this);

		log.info("Started Session Management federator on endpoint URL {}", config.getFederatorSmEndpointURL());
	}

	public void stop() throws Exception {

		if (log.isInfoEnabled() && instanceCache.size() > 0) {
			log.info("Stopping all WSN federator instances...");
		}
		for (String wsnInstanceHash : instanceCache.keySet()) {
			stopFederatorWSN(wsnInstanceHash);
		}

		log.debug("Stopping Session Management federator controller...");
		federatorController.stopAndWait();

		if (sessionManagementEndpoint != null) {
			log.info("Stopping Session Management federator instance on {}...", config.getFederatorSmEndpointURL());
			sessionManagementEndpoint.stop();
		}

		ExecutorUtils.shutdown(executorService, 5, TimeUnit.SECONDS);

	}

	private void stopFederatorWSN(final String wsnInstanceHash) {

		final FederatorWSN federatorWSN = instanceCache.get(wsnInstanceHash).getFirst();
		final ExecutorService federatorWSNThreadPool = instanceCache.get(wsnInstanceHash).getSecond();

		if (log.isInfoEnabled()) {
			log.info("Stopping WSN federator at {}...", federatorWSN.getEndpointUrl());
		}

		try {
			federatorWSN.stop();
		} catch (Exception e) {
			log.warn("Exception while stopping WSN federator: " + e, e);
		}

		ExecutorUtils.shutdown(federatorWSNThreadPool, 5, TimeUnit.SECONDS);
	}

	private static class GetInstanceCallable implements Callable<GetInstanceCallable.Result> {

		public static class Result {

			public URI federatedWSNInstanceEndpointUrl;

			public List<SecretReservationKey> secretReservationKey;

			public URI controller;

			private Result(final List<SecretReservationKey> secretReservationKey,
						   final URI controller,
						   final URI federatedWSNInstanceEndpointUrl) {
				this.secretReservationKey = secretReservationKey;
				this.controller = controller;
				this.federatedWSNInstanceEndpointUrl = federatedWSNInstanceEndpointUrl;
			}

		}

		private SessionManagement sm;

		private List<SecretReservationKey> secretReservationKey;

		private URI controller;

		public GetInstanceCallable(final SessionManagement sm,
								   final List<SecretReservationKey> secretReservationKey,
								   final URI controller) {
			this.sm = sm;
			this.secretReservationKey = secretReservationKey;
			this.controller = controller;
		}

		@Override
		public GetInstanceCallable.Result call() throws Exception {
			URI federatedWSNInstanceEndpointUrl = URI.create(sm.getInstance(secretReservationKey));
			return new GetInstanceCallable.Result(secretReservationKey, controller, federatedWSNInstanceEndpointUrl);
		}
	}

	@Override
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {

		log.debug("getSupportedChannelHandlers() called...");

		final ImmutableSet<FederationManager.Entry<SessionManagement>> entries = federationManager.getEntries();
		final Map<FederationManager.Entry<SessionManagement>, Future<List<ChannelHandlerDescription>>>
				entryToResultMapping = Maps.newHashMap();

		// fork calls to endpoints
		for (final FederationManager.Entry<SessionManagement> entry : entries) {
			final Future<List<ChannelHandlerDescription>> future = executorService.submit(
					new GetSupportedChannelHandlersCallable(entry.endpoint)
			);
			entryToResultMapping.put(entry, future);
		}

		final Set<ChannelHandlerDescription> commonHandlers = newTreeSet(CHANNEL_HANDLER_DESCRIPTION_COMPARATOR);

		for (Map.Entry<FederationManager.Entry<SessionManagement>, Future<List<ChannelHandlerDescription>>> outerEntry : entryToResultMapping
				.entrySet()) {

			try {

				List<ChannelHandlerDescription> outerChannelHandlers = outerEntry.getValue().get();

				for (ChannelHandlerDescription outerChannelHandler : outerChannelHandlers) {

					boolean containedInAllOthers = true;

					for (Map.Entry<FederationManager.Entry<SessionManagement>, Future<List<ChannelHandlerDescription>>> innerEntry : entryToResultMapping
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

	@Override
	public List<String> getSupportedVirtualLinkFilters() {

		ImmutableSet<URI> endpointUrls = federationManager.getEndpointUrls();
		Map<URI, Future<ImmutableSet<String>>> endpointUrlToResultsMapping = Maps.newHashMap();

		// fork calls to endpoints
		log.debug("Invoking getFilters() on {}", endpointUrls);
		for (final URI endpointUrl : endpointUrls) {
			Future<ImmutableSet<String>> future = executorService.submit(new Callable<ImmutableSet<String>>() {
				@Override
				public ImmutableSet<String> call() throws Exception {
					SessionManagement endpoint = federationManager.getEndpointByEndpointUrl(endpointUrl);
					return ImmutableSet.copyOf(endpoint.getSupportedVirtualLinkFilters());
				}
			}
			);
			endpointUrlToResultsMapping.put(endpointUrl, future);
		}

		// join results from endpoints
		ImmutableSet<String> intersectedFilters = null;
		for (Map.Entry<URI, Future<ImmutableSet<String>>> entry : endpointUrlToResultsMapping.entrySet()) {

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

	private static final Comparator<ChannelHandlerDescription> CHANNEL_HANDLER_DESCRIPTION_COMPARATOR =
			new Comparator<ChannelHandlerDescription>() {
				@Override
				public int compare(final ChannelHandlerDescription o1, final ChannelHandlerDescription o2) {
					return FederatorSessionManagement.equals(o1, o2) ? 0 : -1;
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
	public String getInstance(final List<SecretReservationKey> secretReservationKeys)
			throws ExperimentNotRunningFault_Exception, UnknownReservationIdFault_Exception {

		preconditions.checkGetInstanceArguments(secretReservationKeys);

		final String wsnInstanceHash = SessionManagementHelper.calculateWSNInstanceHash(secretReservationKeys);

		// check if instance already exists and return it if that's the case
		final Tuple<FederatorWSN, ExecutorService> instanceCacheEntry = instanceCache.get(wsnInstanceHash);
		if (instanceCacheEntry != null && instanceCacheEntry.getFirst() != null) {

			final FederatorWSN existingWSNFederatorInstance = instanceCacheEntry.getFirst();

			log.debug("Found existing WSN federator instance at {} for secret reservation keys {}",
					new Object[]{
							existingWSNFederatorInstance.getEndpointUrl(),
							secretReservationKeys
					}
			);

			return existingWSNFederatorInstance.getEndpointUrl();
		}


		ImmutableSet<NodeUrn> reservedNodeUrns;

		try {

			final ImmutableSet.Builder<NodeUrn> reservedNodeUrnsBuilder = ImmutableSet.builder();
			final RS rs = WisebedServiceHelper.getRSService(config.getFederatorRsEndpointURL().toString());
			final List<ConfidentialReservationData> reservations =
					rs.getReservation(copyWsnToRs(secretReservationKeys));

			for (ConfidentialReservationData reservation : reservations) {
				reservedNodeUrnsBuilder.addAll(reservation.getNodeUrns());
			}

			reservedNodeUrns = reservedNodeUrnsBuilder.build();

		} catch (ReservationNotFoundFault_Exception e) {
			final UnknownReservationIdFault faultInfo = new UnknownReservationIdFault();
			faultInfo.setMessage(e.getFaultInfo().getMessage());
			throw new UnknownReservationIdFault_Exception(e.getMessage(), faultInfo, e);
		} catch (Exception e) {
			log.warn("An exception was thrown by the reservation system: " + e, e);
			throw new RuntimeException("An exception was thrown by the reservation system: " + e, e);
		}

		// create a WSN API instance under a generated secret URL and remember
		// to which set of secret URLs of federated
		// WSN API instances it maps
		URI wsnEndpointUrl = createRandomWsnEndpointUrl();
		URI controllerEndpointUrl = createRandomControllerEndpointUrl();

		// delegate calls to the relevant federated Session Management API
		// endpoints (fork)

		final List<Future<GetInstanceCallable.Result>> futures = new ArrayList<Future<GetInstanceCallable.Result>>();
		final Map<SessionManagement, List<SecretReservationKey>> serviceMapping =
				getServiceMapping(secretReservationKeys);

		for (Map.Entry<SessionManagement, List<SecretReservationKey>> entry : serviceMapping.entrySet()) {

			final SessionManagement sm = entry.getKey();
			final List<SecretReservationKey> srks = entry.getValue();
			GetInstanceCallable getInstanceCallable = new GetInstanceCallable(
					sm, srks, controllerEndpointUrl
			);

			log.debug("Calling getInstance on {}", entry.getKey());

			futures.add(executorService.submit(getInstanceCallable));
		}

		ImmutableMap.Builder<URI, ImmutableSet<NodeUrnPrefix>> federatedEndpointUrlsToUrnPrefixesMapBuilder =
				ImmutableMap.builder();

		// collect call results (join)
		for (Future<GetInstanceCallable.Result> future : futures) {
			try {

				GetInstanceCallable.Result result = future.get();

				Set<NodeUrnPrefix> federatedUrnPrefixSet = convertToUrnPrefixSet(result.secretReservationKey);
				federatedEndpointUrlsToUrnPrefixesMapBuilder.put(
						result.federatedWSNInstanceEndpointUrl,
						ImmutableSet.copyOf(federatedUrnPrefixSet)
				);

			} catch (Exception e) {

				// if one delegate call fails also fail
				log.error("" + e, e);
				throw new RuntimeException("The federating WSN service could not be started. Reason: " + e, e);

			}
		}

		final FederationManager<WSN> federatorWSNFederationManager =
				new FederationManager<WSN>(new Function<URI, WSN>() {
					@Override
					public WSN apply(final URI input) {
						assert input != null;
						return WisebedServiceHelper.getWSNService(input.toString());
					}
				}, federatedEndpointUrlsToUrnPrefixesMapBuilder.build()
				);

		final ThreadFactory federatorWSNThreadFactory = new ThreadFactoryBuilder()
				.setNameFormat("WSN Federator %d")
				.build();
		final ExecutorService federatorWSNThreadPool = Executors.newCachedThreadPool(federatorWSNThreadFactory);

		final WebservicePublisher<WSN> federatorWSNWebservicePublisher = new WebservicePublisher<WSN>(wsnEndpointUrl);
		final FederatorController federatorWSNController = new FederatorController(controllerEndpointUrl);
		final WSNPreconditions federatorWSNPreconditions = new WSNPreconditions(
				federatorWSNFederationManager.getUrnPrefixes(),
				reservedNodeUrns
		);

		final FederatorWSN federatorWSN = new FederatorWSN(
				federatorWSNController,
				federatorWSNFederationManager,
				federatorWSNWebservicePublisher,
				federatorWSNPreconditions,
				federatorWSNThreadPool
		);

		federatorWSNWebservicePublisher.setImplementer(federatorWSN);

		try {

			federatorWSN.start();

		} catch (Exception e) {
			throw new RuntimeException("The federating WSN service could not be started. Reason: " + e, e);
		}

		instanceCache.put(
				wsnInstanceHash,
				new Tuple<FederatorWSN, ExecutorService>(federatorWSN, federatorWSNThreadPool)
		);

		// return the instantiated WSN API instance sessionManagementEndpoint
		// URL
		return federatorWSN.getEndpointUrl();
	}

	private List<eu.wisebed.api.v3.common.SecretReservationKey> copyWsnToRs(
			final List<eu.wisebed.api.v3.common.SecretReservationKey> ins) {
		List<eu.wisebed.api.v3.common.SecretReservationKey> outs = newArrayListWithCapacity(ins.size());
		for (SecretReservationKey in : ins) {
			outs.add(convertWsnToRs(in));
		}
		return outs;
	}

	private eu.wisebed.api.v3.common.SecretReservationKey convertWsnToRs(
			final eu.wisebed.api.v3.common.SecretReservationKey in) {
		eu.wisebed.api.v3.common.SecretReservationKey out = new eu.wisebed.api.v3.common.SecretReservationKey();
		out.setSecretReservationKey(in.getSecretReservationKey());
		out.setUrnPrefix(in.getUrnPrefix());
		return out;
	}

	/**
	 * Calculates the set of URN prefixes that are "buried" inside {@code secretReservationKeys}.
	 *
	 * @param secretReservationKeys
	 * 		the list of {@link SecretReservationKey} instances
	 *
	 * @return the set of URN prefixes that are "buried" inside {@code secretReservationKeys}
	 */
	private Set<NodeUrnPrefix> convertToUrnPrefixSet(List<SecretReservationKey> secretReservationKeys) {
		Set<NodeUrnPrefix> retSet = new HashSet<NodeUrnPrefix>(secretReservationKeys.size());
		for (SecretReservationKey secretReservationKey : secretReservationKeys) {
			retSet.add(secretReservationKey.getUrnPrefix());
		}
		return retSet;
	}

	/**
	 * Checks for a given list of {@link SecretReservationKey} instances which federated Session Management endpoints are
	 * responsible for which set of URN prefixes.
	 *
	 * @param srks
	 * 		the list of {@link SecretReservationKey} instances as passed in as parameter e.g. to
	 * 		{@link de.uniluebeck.itm.tr.wsn.federator.FederatorSessionManagement#getInstance(java.util.List)}
	 *
	 * @return a mapping between the Session Management sessionManagementEndpoint URL and the subset of URN prefixes they
	 *         serve
	 */
	private Map<SessionManagement, List<SecretReservationKey>> getServiceMapping(
			final List<SecretReservationKey> srks) {

		Map<SessionManagement, List<SecretReservationKey>> map = newHashMap();

		final ImmutableSet<FederationManager.Entry<SessionManagement>> entries = federationManager.getEntries();
		for (FederationManager.Entry<SessionManagement> entry : entries) {
			for (NodeUrnPrefix urnPrefix : entry.urnPrefixes) {
				for (SecretReservationKey srk : srks) {
					if (urnPrefix.equals(srk.getUrnPrefix())) {
						List<SecretReservationKey> secretReservationKeyList = map.get(entry.endpoint);
						if (secretReservationKeyList == null) {
							secretReservationKeyList = new ArrayList<SecretReservationKey>();
							map.put(entry.endpoint, secretReservationKeyList);
						}
						secretReservationKeyList.add(srk);
					}
				}
			}
		}

		return map;
	}

	@Override
	public void areNodesAlive(final long clientRequest, final List<NodeUrn> nodeUrns,
							  final String controllerEndpointUrl) {

		log.debug("SessionManagementServiceImpl.checkAreNodesAlive({}, {})", nodeUrns, controllerEndpointUrl);

		// fork areNodesAlive() calls to federated testbeds
		final Map<URI, Set<NodeUrn>> sessionManagementEndpointUrlToNodeUrnMapping =
				createSessionManagementEndpointUrlToNodeUrnMapping(nodeUrns);

		for (Map.Entry<URI, Set<NodeUrn>> entry : sessionManagementEndpointUrlToNodeUrnMapping.entrySet()) {

			final URI nodeUrnSubsetSessionManagementEndpointUrl = entry.getKey();
			final Set<NodeUrn> nodeUrnSubset = entry.getValue();
			final long federatedRequestId = requestIdGenerator.nextLong();

			final SMAreNodesAliveRunnable runnable = new SMAreNodesAliveRunnable(
					federatorController,
					federationManager.getEndpointByEndpointUrl(nodeUrnSubsetSessionManagementEndpointUrl),
					federatedRequestId,
					clientRequest,
					newArrayList(nodeUrnSubset)
			);

			executorService.submit(runnable);
		}
	}

	@Override
	public void getConfiguration(final Holder<String> rsEndpointUrl,
								 final Holder<String> snaaEndpointUrl,
								 final Holder<List<NodeUrnPrefix>> servedUrnPrefixes,
								 final Holder<List<KeyValuePair>> options) {

		rsEndpointUrl.value = config.getFederatorRsEndpointURL().toString();
		snaaEndpointUrl.value = config.getFederatorSnaaEndpointUrl().toString();
		servedUrnPrefixes.value = newArrayList();
		for (FederatorWSNTestbedConfig federatorWSNTestbedConfig : config.getFederates()) {
			servedUrnPrefixes.value.addAll(federatorWSNTestbedConfig.getUrnPrefixes());
		}
	}

	/**
	 * Returns a {@link Map} that maps Session Management Endpoint URLs to the subset of node URNs of {@code nodeUrns} for
	 * which each map entry is responsible.
	 *
	 * @param nodeUrns
	 * 		the node URNs for which to create the mapping
	 *
	 * @return see above
	 */
	private Map<URI, Set<NodeUrn>> createSessionManagementEndpointUrlToNodeUrnMapping(final List<NodeUrn> nodeUrns) {

		final Map<URI, Set<NodeUrn>> map = Maps.newHashMap();

		final ImmutableSet<FederationManager.Entry<SessionManagement>> entries = federationManager.getEntries();
		for (FederationManager.Entry<SessionManagement> entry : entries) {
			for (NodeUrn nodeUrn : nodeUrns) {
				for (NodeUrnPrefix urnPrefix : entry.urnPrefixes) {
					if (nodeUrn.belongsTo(urnPrefix)) {
						Set<NodeUrn> remoteNodeUrnsToInclude = map.get(entry.endpointUrl);
						if (remoteNodeUrnsToInclude == null) {
							remoteNodeUrnsToInclude = Sets.newHashSet();
							map.put(entry.endpointUrl, remoteNodeUrnsToInclude);
						}
						remoteNodeUrnsToInclude.add(nodeUrn);
					}
				}
			}
		}
		return map;
	}

	@Override
	public String getNetwork() {
		final BiMap<URI, Callable<String>> endpointUrlToCallableMap = HashBiMap.create();
		for (final FederationManager.Entry<SessionManagement> entry : federationManager.getEntries()) {
			endpointUrlToCallableMap.put(entry.endpointUrl, new Callable<String>() {
				@Override
				public String call() throws Exception {
					return entry.endpoint.getNetwork();
				}
			}
			);
		}
		return FederatorWiseMLMerger.merge(endpointUrlToCallableMap, executorService);

	}

	private URI createRandomWsnEndpointUrl() {
		final URI federatorSmEndpointURL = config.getFederatorSmEndpointURL();
		return URI.create(federatorSmEndpointURL.getScheme() + "://" +
				federatorSmEndpointURL.getHost() + ":" +
				federatorSmEndpointURL.getPort() + "/" +
				secureIdGenerator.getNextId() + "/wsn"
		);
	}

	private URI createRandomControllerEndpointUrl() {
		final URI federatorSmEndpointURL = config.getFederatorSmEndpointURL();
		return URI.create(federatorSmEndpointURL.getScheme() + "://" +
				federatorSmEndpointURL.getHost() + ":" +
				federatorSmEndpointURL.getPort() + "/" +
				secureIdGenerator.getNextId() + "/controller"
		);
	}
}
