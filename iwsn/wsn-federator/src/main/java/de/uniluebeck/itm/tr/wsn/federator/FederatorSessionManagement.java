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
import de.uniluebeck.itm.tr.util.*;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.ReservervationNotFoundExceptionException;
import eu.wisebed.api.sm.*;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.rs.RSServiceHelper;
import eu.wisebed.testbed.api.wsn.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.collect.Lists.newArrayListWithCapacity;

@WebService(
		serviceName = "SessionManagementService",
		targetNamespace = Constants.NAMESPACE_SESSION_MANAGEMENT_SERVICE,
		portName = "SessionManagementPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_SESSION_MANAGEMENT_SERVICE
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
	 * A mapping between the federator SessionManagement Endpoint URLs and the set of URN Prefixes they are serving.
	 */
	private final BiMap<String, Set<String>> sessionManagementEndpointUrlPrefixSet;

	/**
	 *
	 */
	private final String snaaEndpointUrl;

	/**
	 * The base URL (e.g. http://testbed.wisebed.eu:1234/) under which this SessionManagement federator instance is run.
	 */
	private final String endpointUrlBase;

	/**
	 * The Reservation System sessionManagementEndpoint URL. Usually this would be a federated reservation system that
	 * serves all URN prefixes that are federated by this Session Management federator.
	 */
	private final String reservationEndpointUrl;

	/**
	 * wsnInstanceHash (see {@link eu.wisebed.testbed.api.wsn.SessionManagementHelper#calculateWSNInstanceHash(java.util.List)}
	 * ) -> Federating WSN API instance
	 */
	private final TimedCache<String, Tuple<FederatorWSN, ExecutorService>> instanceCache =
			new TimedCache<String, Tuple<FederatorWSN, ExecutorService>>();

	/**
	 * Used for generating random request IDs and URLs.
	 */
	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	/**
	 * The URL of the Web service endpoint of this SessionManagement federator instance.
	 */
	private final String sessionManagementEndpointUrl;

	/**
	 * The actual Web service endpoint of this SessionManagement federator instance.
	 */
	private Endpoint sessionManagementEndpoint;

	/**
	 * Preconditions instance to check method arguments sent by user.
	 */
	private final SessionManagementPreconditions preconditions;

	/**
	 * FederatorController instance managing asynchronous replies for {@link SessionManagement#areNodesAlive(java.util.List,
	 * String)}.
	 */
	private final FederatorController federatorController;

	public FederatorSessionManagement(final BiMap<String, Set<String>> sessionManagementEndpointUrlPrefixSet,
									  final String endpointUrlBase,
									  final String path,
									  final String reservationEndpointUrl,
									  final String snaaEndpointUrl) {

		this.sessionManagementEndpointUrlPrefixSet = sessionManagementEndpointUrlPrefixSet;
		this.snaaEndpointUrl = snaaEndpointUrl;
		this.endpointUrlBase = endpointUrlBase.endsWith("/") ? endpointUrlBase : endpointUrlBase + "/";
		this.sessionManagementEndpointUrl = endpointUrlBase + (path.startsWith("/") ? path.substring(1) : path);
		this.reservationEndpointUrl = reservationEndpointUrl;

		this.preconditions = new SessionManagementPreconditions();
		for (Set<String> endpointPrefixSet : sessionManagementEndpointUrlPrefixSet.values()) {
			this.preconditions.addServedUrnPrefixes(endpointPrefixSet.toArray(new String[endpointPrefixSet.size()]));
		}

		String controllerEndpointUrl = endpointUrlBase + secureIdGenerator.getNextId() + "/controller";
		this.federatorController = new FederatorController(controllerEndpointUrl);

	}

	public void start() throws Exception {
		String bindAllInterfacesUrl = UrlUtils.convertHostToZeros(sessionManagementEndpointUrl);

		log.debug("Starting Session Management federator on endpoint URL {} and binding URL {}...",
				sessionManagementEndpointUrl,
				bindAllInterfacesUrl
		);

		federatorController.start();
		sessionManagementEndpoint = Endpoint.publish(bindAllInterfacesUrl, this);

		log.info("Started Session Management federator on {}", sessionManagementEndpointUrl);
	}

	public void stop() throws Exception {

		if (log.isInfoEnabled() && instanceCache.size() > 0) {
			log.info("Stopping all WSN federator instances...");
		}
		for (String wsnInstanceHash : instanceCache.keySet()) {
			stopFederatorWSN(wsnInstanceHash);
		}

		log.debug("Stopping Session Management federator controller...");
		federatorController.stop();

		if (sessionManagementEndpoint != null) {
			log.info("Stopping Session Management federator instance on {}...", sessionManagementEndpointUrl);
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

			public String federatedWSNInstanceEndpointUrl;

			public List<SecretReservationKey> secretReservationKey;

			public String controller;

			private Result(List<SecretReservationKey> secretReservationKey, String controller,
						   String federatedWSNInstanceEndpointUrl) {
				this.secretReservationKey = secretReservationKey;
				this.controller = controller;
				this.federatedWSNInstanceEndpointUrl = federatedWSNInstanceEndpointUrl;
			}
		}

		private String federatedSessionManagementEndpointUrl;

		private List<SecretReservationKey> secretReservationKey;

		private String controller;

		public GetInstanceCallable(String federatedSessionManagementEndpointUrl,
								   List<SecretReservationKey> secretReservationKey, String controller) {
			this.federatedSessionManagementEndpointUrl = federatedSessionManagementEndpointUrl;
			this.secretReservationKey = secretReservationKey;
			this.controller = controller;
		}

		@Override
		public GetInstanceCallable.Result call() throws Exception {

			SessionManagement service = WSNServiceHelper
					.getSessionManagementService(federatedSessionManagementEndpointUrl);
			String federatedWSNInstanceEndpointUrl = service.getInstance(secretReservationKey, controller);

			return new GetInstanceCallable.Result(secretReservationKey, controller, federatedWSNInstanceEndpointUrl);

		}
	}

	@Override
	public String getInstance(
			@WebParam(name = "secretReservationKeys", targetNamespace = "")
			List<SecretReservationKey> secretReservationKeys,
			@WebParam(name = "controller", targetNamespace = "")
			String controller)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		preconditions.checkGetInstanceArguments(secretReservationKeys, controller);

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

			log.debug("Adding controller to the set of controllers: {}", controller);
			existingWSNFederatorInstance.addController(controller);

			return existingWSNFederatorInstance.getEndpointUrl();
		}


		ImmutableSet<String> reservedNodeUrns;

		try {

			final ImmutableSet.Builder<String> reservedNodeUrnsBuilder = ImmutableSet.builder();
			final RS rs = RSServiceHelper.getRSService(reservationEndpointUrl);
			final List<ConfidentialReservationData> reservations =
					rs.getReservation(copyWsnToRs(secretReservationKeys));

			for (ConfidentialReservationData reservation : reservations) {
				reservedNodeUrnsBuilder.addAll(reservation.getNodeURNs());
			}

			reservedNodeUrns = reservedNodeUrnsBuilder.build();

		} catch (ReservervationNotFoundExceptionException e) {
			final UnknownReservationIdException faultInfo = new UnknownReservationIdException();
			faultInfo.setMessage(e.getFaultInfo().getMessage());
			throw new UnknownReservationIdException_Exception(e.getMessage(), faultInfo, e);
		} catch (Exception e) {
			log.warn("An exception was thrown by the reservation system: " + e, e);
			throw new RuntimeException("An exception was thrown by the reservation system: " + e, e);
		}

		// create a WSN API instance under a generated secret URL and remember
		// to which set of secret URLs of federated
		// WSN API instances it maps
		String wsnEndpointUrl = endpointUrlBase + secureIdGenerator.getNextId() + "/wsn";
		String controllerEndpointUrl = endpointUrlBase + secureIdGenerator.getNextId() + "/controller";

		// delegate calls to the relevant federated Session Management API
		// endpoints (fork)

		final List<Future<GetInstanceCallable.Result>> futures = new ArrayList<Future<GetInstanceCallable.Result>>();
		final Map<String, List<SecretReservationKey>> serviceMapping = getServiceMapping(secretReservationKeys);

		for (Map.Entry<String, List<SecretReservationKey>> entry : serviceMapping.entrySet()) {

			String sessionManagementEndpointUrl = entry.getKey();

			GetInstanceCallable getInstanceCallable = new GetInstanceCallable(
					sessionManagementEndpointUrl, entry.getValue(), controllerEndpointUrl
			);

			log.debug("Calling getInstance on {}", entry.getKey());

			futures.add(executorService.submit(getInstanceCallable));
		}

		ImmutableMap.Builder<String, ImmutableSet<String>> federatedEndpointUrlsToUrnPrefixesMapBuilder =
				ImmutableMap.builder();

		// collect call results (join)
		for (Future<GetInstanceCallable.Result> future : futures) {
			try {

				GetInstanceCallable.Result result = future.get();

				Set<String> federatedUrnPrefixSet = convertToUrnPrefixSet(result.secretReservationKey);
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
				new FederationManager<WSN>(new Function<String, WSN>() {
					@Override
					public WSN apply(final String input) {
						return WSNServiceHelper.getWSNService(input);
					}
				}, federatedEndpointUrlsToUrnPrefixesMapBuilder.build()
				);

		final URL endpointUrl;
		try {
			endpointUrl = new URL(wsnEndpointUrl);
		} catch (MalformedURLException e) {
			throw new RuntimeException("" + e, e);
		}

		final ThreadFactory federatorWSNThreadFactory = new ThreadFactoryBuilder()
				.setNameFormat("WSN Federator %d")
				.build();
		final ExecutorService federatorWSNThreadPool = Executors.newCachedThreadPool(federatorWSNThreadFactory);

		final WebservicePublisher<WSN> federatorWSNWebservicePublisher = new WebservicePublisher<WSN>(endpointUrl);
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

			// add controller to set of upstream controllers so that output is
			// sent upwards
			federatorWSN.addController(controller);

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

	private List<eu.wisebed.api.rs.SecretReservationKey> copyWsnToRs(final List<SecretReservationKey> ins) {
		List<eu.wisebed.api.rs.SecretReservationKey> outs = newArrayListWithCapacity(ins.size());
		for (SecretReservationKey in : ins) {
			outs.add(convertWsnToRs(in));
		}
		return outs;
	}

	private eu.wisebed.api.rs.SecretReservationKey convertWsnToRs(final SecretReservationKey in) {
		eu.wisebed.api.rs.SecretReservationKey out = new eu.wisebed.api.rs.SecretReservationKey();
		out.setSecretReservationKey(in.getSecretReservationKey());
		out.setUrnPrefix(in.getUrnPrefix());
		return out;
	}

	/**
	 * Calculates the set of URN prefixes that are "buried" inside {@code secretReservationKeys}.
	 *
	 * @param secretReservationKeys the list of {@link SecretReservationKey} instances
	 *
	 * @return the set of URN prefixes that are "buried" inside {@code secretReservationKeys}
	 */
	private Set<String> convertToUrnPrefixSet(List<SecretReservationKey> secretReservationKeys) {
		Set<String> retSet = new HashSet<String>(secretReservationKeys.size());
		for (SecretReservationKey secretReservationKey : secretReservationKeys) {
			retSet.add(secretReservationKey.getUrnPrefix());
		}
		return retSet;
	}

	/**
	 * Checks for a given list of {@link SecretReservationKey} instances which federated Session Management endpoints are
	 * responsible for which set of URN prefixes.
	 *
	 * @param secretReservationKeys the list of {@link SecretReservationKey} instances as passed in as parameter e.g. to
	 *                              {@link de.uniluebeck.itm.tr.wsn.federator.FederatorSessionManagement#getInstance(java.util.List,
	 *							  String)}
	 *
	 * @return a mapping between the Session Management sessionManagementEndpoint URL and the subset of URN prefixes they
	 *         serve
	 */
	private Map<String, List<SecretReservationKey>> getServiceMapping(
			List<SecretReservationKey> secretReservationKeys) {

		HashMap<String, List<SecretReservationKey>> map = new HashMap<String, List<SecretReservationKey>>();

		for (Map.Entry<String, Set<String>> entry : sessionManagementEndpointUrlPrefixSet.entrySet()) {
			for (String urnPrefix : entry.getValue()) {
				for (SecretReservationKey srk : secretReservationKeys) {
					if (urnPrefix.equals(srk.getUrnPrefix())) {

						List<SecretReservationKey> secretReservationKeyList = map.get(entry.getKey());
						if (secretReservationKeyList == null) {
							secretReservationKeyList = new ArrayList<SecretReservationKey>();
							map.put(entry.getKey(), secretReservationKeyList);
						}
						secretReservationKeyList.add(srk);

					}
				}
			}
		}

		return map;
	}

	@Override
	public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes,
								@WebParam(name = "controllerEndpointUrl", targetNamespace = "") final
								String controllerEndpointUrl) {

		log.debug("SessionManagementServiceImpl.checkAreNodesAlive({}, {})", nodes, controllerEndpointUrl);

		// fork areNodesAlive() calls to federated testbeds
		final String requestId = secureIdGenerator.getNextId();
		final Map<String, Set<String>> sessionManagementEndpointUrlToNodeUrnMapping =
				createSessionManagementEndpointUrlToNodeUrnMapping(nodes);

		for (Map.Entry<String, Set<String>> entry : sessionManagementEndpointUrlToNodeUrnMapping.entrySet()) {

			final String nodeUrnSubsetSessionManagementEndpointUrl = entry.getKey();
			final Set<String> nodeUrnSubset = entry.getValue();

			executorService.submit(
					new SMAreNodesAliveRunnable(
							federatorController,
							WSNServiceHelper.getSessionManagementService(nodeUrnSubsetSessionManagementEndpointUrl),
							requestId,
							Lists.newArrayList(nodeUrnSubset)
					)
			);
		}

		return requestId;
	}

	/**
	 * Returns a {@link Map} that maps Session Management Endpoint URLs to the subset of node URNs of {@code nodeUrns} for
	 * which each map entry is responsible.
	 *
	 * @param nodeUrns the node URNs for which to create the mapping
	 *
	 * @return see above
	 */
	private Map<String, Set<String>> createSessionManagementEndpointUrlToNodeUrnMapping(final List<String> nodeUrns) {

		Map<String, Set<String>> map = Maps.newHashMap();

		for (Map.Entry<String, Set<String>> entry : sessionManagementEndpointUrlPrefixSet.entrySet()) {

			final String remoteSessionManagementEndpointUrl = entry.getKey();
			final Set<String> remoteNodeUrnsServed = entry.getValue();

			for (String nodeUrn : nodeUrns) {

				if (remoteNodeUrnsServed.contains(nodeUrn)) {
					Set<String> remoteNodeUrnsToInclude = map.get(remoteSessionManagementEndpointUrl);
					if (remoteNodeUrnsToInclude == null) {
						remoteNodeUrnsToInclude = Sets.newHashSet();
						map.put(remoteSessionManagementEndpointUrl, remoteNodeUrnsToInclude);
					}
					remoteNodeUrnsToInclude.add(nodeUrn);
				}
			}

		}

		return map;
	}

	@Override
	public void free(
			@WebParam(name = "secretReservationKeys", targetNamespace = "")
			List<SecretReservationKey> secretReservationKeys)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		preconditions.checkFreeArguments(secretReservationKeys);

		// check if instance still exists and if not simply exit
		String wsnInstanceHash = SessionManagementHelper.calculateWSNInstanceHash(secretReservationKeys);
		stopFederatorWSN(wsnInstanceHash);
		instanceCache.remove(wsnInstanceHash);

		// call free on all relevant federated Session Management API endpoints
		// (asynchronously)
		// only fork, but no join since return values are irrelevant for us
		Map<String, List<SecretReservationKey>> serviceMapping = getServiceMapping(secretReservationKeys);

		for (Map.Entry<String, List<SecretReservationKey>> entry : serviceMapping.entrySet()) {

			String federatedSessionManagementEndpointUrl = entry.getKey();
			List<SecretReservationKey> secretReservationKeysToFree = entry.getValue();

			executorService
					.submit(new FreeRunnable(federatedSessionManagementEndpointUrl, secretReservationKeysToFree));

		}

	}

	private static class FreeRunnable implements Runnable {

		String sessionManagementEndpointUrl;

		List<SecretReservationKey> secretReservationKeys;

		private FreeRunnable(String sessionManagementEndpointUrl, List<SecretReservationKey> secretReservationKeys) {
			this.sessionManagementEndpointUrl = sessionManagementEndpointUrl;
			this.secretReservationKeys = secretReservationKeys;
		}

		@Override
		public void run() {
			try {
				log.debug(
						"Freeing WSN instance on {} for keys {}",
						sessionManagementEndpointUrl,
						secretReservationKeys
				);
				WSNServiceHelper.getSessionManagementService(sessionManagementEndpointUrl).free(secretReservationKeys);
			} catch (ExperimentNotRunningException_Exception e) {
				log.warn("" + e, e);
			} catch (UnknownReservationIdException_Exception e) {
				log.warn("" + e, e);
			}
		}
	}

	@Override
	public String getNetwork() {

		final BiMap<String, Callable<String>> endpointUrlToCallableMap = HashBiMap.create();
		final Set<String> endpointUrls = sessionManagementEndpointUrlPrefixSet.keySet();

		for (final String endpointUrl : endpointUrls) {
			endpointUrlToCallableMap.put(endpointUrl, new Callable<String>() {
				@Override
				public String call() throws Exception {
					return WSNServiceHelper.getSessionManagementService(endpointUrl).getNetwork();
				}
			}
			);
		}

		return FederatorWiseMLMerger.merge(endpointUrlToCallableMap, executorService);

	}

	@Override
	public void getConfiguration(
			@WebParam(name = "rsEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> rsEndpointUrl,
			@WebParam(name = "snaaEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> snaaEndpointUrl,
			@WebParam(name = "options", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<List<KeyValuePair>> options) {

		rsEndpointUrl.value = this.reservationEndpointUrl;
		snaaEndpointUrl.value = this.snaaEndpointUrl;
	}

}
