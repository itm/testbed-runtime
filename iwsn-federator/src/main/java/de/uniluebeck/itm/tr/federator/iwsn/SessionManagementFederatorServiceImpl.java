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

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.federator.utils.FederationManager;
import de.uniluebeck.itm.tr.federator.utils.WebservicePublisher;
import de.uniluebeck.itm.tr.iwsn.common.CommonPreconditions;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementHelper;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.util.SecureIdGenerator;
import de.uniluebeck.itm.util.TimedCache;
import de.uniluebeck.itm.util.Tuple;
import de.uniluebeck.itm.util.concurrent.ExecutorUtils;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.ChannelHandlerDescription;
import eu.wisebed.api.v3.sm.NodeConnectionStatus;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.sm.UnknownSecretReservationKeyFault;
import eu.wisebed.api.v3.wsn.WSN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import javax.xml.ws.Holder;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static eu.wisebed.api.v3.WisebedServiceHelper.createSMUnknownSecretReservationKeyFault;

@WebService(
		name = "SessionManagement",
		endpointInterface = "eu.wisebed.api.v3.sm.SessionManagement",
		portName = "SessionManagementPort",
		serviceName = "SessionManagementService",
		targetNamespace = "http://wisebed.eu/api/v3/sm"
)
public class SessionManagementFederatorServiceImpl extends AbstractService implements SessionManagementFederatorService {

	/**
	 * The logger instance for this Session Management federator instance.
	 */
	private static final Logger log = LoggerFactory.getLogger(SessionManagementFederatorServiceImpl.class);

	/**
	 * The {@link java.util.concurrent.ExecutorService} instance that is used to run jobs in parallel.
	 */
	private final ExecutorService executorService;

	/**
	 * wsnInstanceHash (see {@link de.uniluebeck.itm.tr.iwsn.common.SessionManagementHelper#calculateWSNInstanceHash(java.util.List)}
	 * ) -> Federating WSN API instance
	 */
	private final TimedCache<String, Tuple<FederatorWSN, ExecutorService>> instanceCache =
			new TimedCache<String, Tuple<FederatorWSN, ExecutorService>>();

	/**
	 * Used for generating random request IDs and URLs.
	 */
	private final SecureIdGenerator secureIdGenerator;

	private final ServicePublisher servicePublisher;

	/**
	 * Preconditions instance to check method arguments sent by user.
	 */
	private final SessionManagementPreconditions preconditions;

	private final IWSNFederatorServiceConfig config;

	private final FederationManager<SessionManagement> federationManager;

	private final RS rs;

	private ServicePublisherService jaxWsService;

	@Inject
	public SessionManagementFederatorServiceImpl(
			final FederationManager<SessionManagement> federationManager,
			final SessionManagementPreconditions preconditions,
			final IWSNFederatorServiceConfig config,
			final SecureIdGenerator secureIdGenerator,
			final ServicePublisher servicePublisher,
			final ExecutorService executorService,
			final RS rs) {
		this.federationManager = checkNotNull(federationManager);
		this.preconditions = checkNotNull(preconditions);
		this.config = checkNotNull(config);
		this.secureIdGenerator = checkNotNull(secureIdGenerator);
		this.servicePublisher = checkNotNull(servicePublisher);
		this.executorService = checkNotNull(executorService);
		this.rs = checkNotNull(rs);
	}

	@Override
	protected void doStart() {
		try {

			jaxWsService = servicePublisher.createJaxWsService(config.getContextPath(), this);
			jaxWsService.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			if (log.isInfoEnabled() && instanceCache.size() > 0) {
				log.info("Stopping all WSN federator instances...");
			}

			for (String wsnInstanceHash : instanceCache.keySet()) {
				stopFederatorWSN(wsnInstanceHash);
			}

			if (jaxWsService != null && jaxWsService.isRunning()) {
				jaxWsService.stopAndWait();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
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

	@Override
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {

		checkState(isRunning());

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

		checkState(isRunning());

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

	@Override
	public String getVersion() {
		checkState(isRunning());
		return "3.0";
	}

	private static final Comparator<ChannelHandlerDescription> CHANNEL_HANDLER_DESCRIPTION_COMPARATOR =
			new Comparator<ChannelHandlerDescription>() {
				@Override
				public int compare(final ChannelHandlerDescription o1, final ChannelHandlerDescription o2) {
					return SessionManagementFederatorServiceImpl.equals(o1, o2) ? 0 : -1;
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
			throws UnknownSecretReservationKeyFault {

		checkState(isRunning());

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


		final ImmutableSet<NodeUrn> reservedNodeUrns;

		try {

			final ImmutableSet.Builder<NodeUrn> reservedNodeUrnsBuilder = ImmutableSet.builder();
			final List<ConfidentialReservationData> reservations = rs.getReservation(copyWsnToRs(secretReservationKeys));

			for (ConfidentialReservationData reservation : reservations) {
				reservedNodeUrnsBuilder.addAll(reservation.getNodeUrns());
			}

			reservedNodeUrns = reservedNodeUrnsBuilder.build();

		} catch (eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault e) {
			throw createSMUnknownSecretReservationKeyFault(e.getMessage(), e.getFaultInfo().getSecretReservationKey(),
					e
			);
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
		final ListeningExecutorService federatorWSNThreadPool =
				MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(federatorWSNThreadFactory));

		final WebservicePublisher<WSN> federatorWSNWebservicePublisher = new WebservicePublisher<WSN>(wsnEndpointUrl);
		final FederatorController federatorWSNController = new FederatorController(controllerEndpointUrl);

		final WSNPreconditions federatorWSNPreconditions = new WSNPreconditions(new CommonPreconditions(
				new ServedNodeUrnsProvider() {
					@Override
					public Set<NodeUrn> get() {
						return reservedNodeUrns;
					}
				},
				new ServedNodeUrnPrefixesProvider() {
					@Override
					public Set<NodeUrnPrefix> get() {
						return federatorWSNFederationManager.getUrnPrefixes();
					}
				}
		)
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
		out.setKey(in.getKey());
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
	 * 		{@link SessionManagementFederatorServiceImpl#getInstance(java.util.List)}
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
	public List<NodeConnectionStatus> areNodesConnected(final List<NodeUrn> nodeUrns) {

		checkState(isRunning());

		log.debug("SessionManagementServiceImpl.checkAreNodesAlive({})", nodeUrns);

		// fork areNodesAlive() calls to federated testbeds
		final Map<URI, Set<NodeUrn>> sessionManagementEndpointUrlToNodeUrnMapping =
				createSessionManagementEndpointUrlToNodeUrnMapping(nodeUrns);

		final List<Tuple<Set<NodeUrn>, Future<List<NodeConnectionStatus>>>> futures = newArrayList();

		for (Map.Entry<URI, Set<NodeUrn>> entry : sessionManagementEndpointUrlToNodeUrnMapping.entrySet()) {

			final URI nodeUrnSubsetSessionManagementEndpointUrl = entry.getKey();
			final Set<NodeUrn> nodeUrnSubset = entry.getValue();

			final SMAreNodesAliveCallable callable = new SMAreNodesAliveCallable(
					federationManager.getEndpointByEndpointUrl(nodeUrnSubsetSessionManagementEndpointUrl),
					newArrayList(nodeUrnSubset)
			);

			futures.add(new Tuple<Set<NodeUrn>, Future<List<NodeConnectionStatus>>>(
					nodeUrnSubset,
					executorService.submit(callable)
			)
			);
		}

		final List<NodeConnectionStatus> statusList = newArrayList();

		for (Tuple<Set<NodeUrn>, Future<List<NodeConnectionStatus>>> tuple : futures) {
			try {
				statusList.addAll(tuple.getSecond().get(30, TimeUnit.SECONDS));
			} catch (Exception e) {
				for (NodeUrn nodeUrn : tuple.getFirst()) {
					final NodeConnectionStatus status = new NodeConnectionStatus();
					status.setNodeUrn(nodeUrn);
					status.setConnected(false);
					statusList.add(status);
				}
			}
		}

		return statusList;
	}

	@Override
	public void getConfiguration(final Holder<String> rsEndpointUrl,
								 final Holder<String> snaaEndpointUrl,
								 final Holder<List<NodeUrnPrefix>> servedUrnPrefixes,
								 final Holder<List<KeyValuePair>> options) {

		checkState(isRunning());

		rsEndpointUrl.value = config.getFederatorRsEndpointUri().toString();
		snaaEndpointUrl.value = config.getFederatorSnaaEndpointUri().toString();
		servedUrnPrefixes.value = newArrayList();
		for (Set<NodeUrnPrefix> nodeUrnPrefixes : config.getFederates().values()) {
			servedUrnPrefixes.value.addAll(nodeUrnPrefixes);
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

		checkState(isRunning());

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
		final URI uri = config.getFederatorSmEndpointUri();
		return URI.create(uri.getScheme() + "://" +
				uri.getHost() + ":" +
				uri.getPort() + "/" +
				secureIdGenerator.getNextId() + "/wsn"
		);
	}

	private URI createRandomControllerEndpointUrl() {
		final URI uri = config.getFederatorSmEndpointUri();
		return URI.create(uri.getScheme() + "://" +
				uri.getHost() + ":" +
				uri.getPort() + "/" +
				secureIdGenerator.getNextId() + "/controller"
		);
	}
}
