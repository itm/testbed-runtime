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

import static com.google.common.collect.Lists.newArrayListWithCapacity;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import eu.wisebed.api.WisebedServiceHelper;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.common.SecretReservationKey;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.ReservationNotFoundExceptionException;
import eu.wisebed.api.sm.ExperimentNotRunningException_Exception;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.sm.UnknownReservationIdException;
import eu.wisebed.api.sm.UnknownReservationIdException_Exception;
import eu.wisebed.api.wsn.WSN;

@WebService(
		serviceName = "SessionManagementService",
		targetNamespace = "urn:SessionManagementService",
		portName = "SessionManagementPort",
		endpointInterface = "eu.wisebed.api.sm.SessionManagement"
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
	private final BiMap<String, Set<String>> sessionManagementEndpointUrlPrefixSet = HashBiMap.create();

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
	 * FederatorController instance managing asynchronous replies for {@link SessionManagement#areNodesAlive(java.util.List,
	 * String)}.
	 */
	private final FederatorController federatorController;

	private final FederatorWSNConfig config;

	public FederatorSessionManagement(final FederatorWSNConfig config) {

		this.config = config;

		for (FederatorWSNTestbedConfig testbedConfig : config.getFederates()) {
			sessionManagementEndpointUrlPrefixSet.put(
					testbedConfig.getSmEndpointUrl().toString(),
					testbedConfig.getUrnPrefixes()
			);
		}

		this.preconditions = new SessionManagementPreconditions();
		for (Set<String> endpointPrefixSet : sessionManagementEndpointUrlPrefixSet.values()) {
			this.preconditions.addServedUrnPrefixes(endpointPrefixSet);
		}

		this.federatorController = new FederatorController(createRandomControllerEndpointUrl());

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

			SessionManagement service = WisebedServiceHelper
					.getSessionManagementService(federatedSessionManagementEndpointUrl);
			String federatedWSNInstanceEndpointUrl = service.getInstance(secretReservationKey);

			return new GetInstanceCallable.Result(secretReservationKey, controller, federatedWSNInstanceEndpointUrl);

		}

	}
	


	@Override
	public String getInstance(
			@WebParam(name = "secretReservationKeys", targetNamespace = "")
			List<SecretReservationKey> secretReservationKeys)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {		

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


		ImmutableSet<String> reservedNodeUrns;

		try {

			final ImmutableSet.Builder<String> reservedNodeUrnsBuilder = ImmutableSet.builder();
			final RS rs = WisebedServiceHelper.getRSService(config.getFederatorRsEndpointURL().toString());
			final List<ConfidentialReservationData> reservations =
					rs.getReservation(copyWsnToRs(secretReservationKeys));

			for (ConfidentialReservationData reservation : reservations) {
				reservedNodeUrnsBuilder.addAll(reservation.getNodeURNs());
			}

			reservedNodeUrns = reservedNodeUrnsBuilder.build();

		} catch (ReservationNotFoundExceptionException e) {
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
		String wsnEndpointUrl = createRandomWsnEndpointUrl();
		String controllerEndpointUrl = createRandomControllerEndpointUrl();

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
						return WisebedServiceHelper.getWSNService(input);
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

	private List<eu.wisebed.api.common.SecretReservationKey> copyWsnToRs(final List<eu.wisebed.api.common.SecretReservationKey> ins) {
		List<eu.wisebed.api.common.SecretReservationKey> outs = newArrayListWithCapacity(ins.size());
		for (SecretReservationKey in : ins) {
			outs.add(convertWsnToRs(in));
		}
		return outs;
	}

	private eu.wisebed.api.common.SecretReservationKey convertWsnToRs(final eu.wisebed.api.common.SecretReservationKey in) {
		eu.wisebed.api.common.SecretReservationKey out = new eu.wisebed.api.common.SecretReservationKey();
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
	 * @param secretReservationKeys
	 * 		the list of {@link SecretReservationKey} instances as passed in as parameter e.g. to
	 * 		{@link de.uniluebeck.itm.tr.wsn.federator.FederatorSessionManagement#getInstance(java.util.List,
	 *         String)}
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
							WisebedServiceHelper.getSessionManagementService(nodeUrnSubsetSessionManagementEndpointUrl),
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
	 * @param nodeUrns
	 * 		the node URNs for which to create the mapping
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
	public String getNetwork() {

		final BiMap<String, Callable<String>> endpointUrlToCallableMap = HashBiMap.create();
		final Set<String> endpointUrls = sessionManagementEndpointUrlPrefixSet.keySet();

		for (final String endpointUrl : endpointUrls) {
			endpointUrlToCallableMap.put(endpointUrl, new Callable<String>() {
				@Override
				public String call() throws Exception {
					return WisebedServiceHelper.getSessionManagementService(endpointUrl).getNetwork();
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
			@WebParam(name = "federatorSnaaEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> snaaEndpointUrl,
			@WebParam(name = "options", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<List<KeyValuePair>> options) {

		rsEndpointUrl.value = config.getFederatorRsEndpointURL().toString();
		snaaEndpointUrl.value = config.getFederatorSnaaEndpointUrl().toString();
	}

	private String createRandomControllerEndpointUrl() {
		final URI federatorSmEndpointURL = config.getFederatorSmEndpointURL();
		return federatorSmEndpointURL.getScheme() + "://" +
				federatorSmEndpointURL.getHost() + ":" +
				federatorSmEndpointURL.getPort() + "/" +
				secureIdGenerator.getNextId() + "/controller";
	}

	private String createRandomWsnEndpointUrl() {
		final URI federatorSmEndpointURL = config.getFederatorSmEndpointURL();
		return federatorSmEndpointURL.getScheme() + "://" +
				federatorSmEndpointURL.getHost() + ":" +
				federatorSmEndpointURL.getPort() + "/" +
				secureIdGenerator.getNextId() + "/wsn";
	}


}
