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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import de.uniluebeck.itm.tr.util.TimedCache;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.SessionManagementPreconditions;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v211.ExperimentNotRunningException_Exception;
import eu.wisebed.testbed.api.wsn.v211.SecretReservationKey;
import eu.wisebed.testbed.api.wsn.v211.SessionManagement;
import eu.wisebed.testbed.api.wsn.v211.UnknownReservationIdException_Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import java.util.*;
import java.util.concurrent.*;

@WebService(serviceName = "SessionManagementService", targetNamespace = Constants.NAMESPACE_SESSION_MANAGEMENT_SERVICE,
		portName = "SessionManagementPort", endpointInterface = Constants.ENDPOINT_INTERFACE_SESSION_MANGEMENT_SERVICE)
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
	 * Session Management Service sessionManagementEndpoint URL <-> Set<URN Prefixes>
	 */
	private final BiMap<String, Set<String>> sessionManagementEndpointUrlPrefixSet;

	/**
	 *
	 */
	private final String endpointUrlBase;

	/**
	 * The Reservation System sessionManagementEndpoint URL. Usually this would be a federated reservation system that
	 * serves all URN prefixes that are federated by this Session Management federator.
	 */
	private final String reservationEndpointUrl;

	/**
	 * wsnInstanceHash (see {@link de.uniluebeck.itm.tr.wsn.federator.FederatorSessionManagement#calculateWSNInstanceHash(java.util.List)}
	 * ) -> Federating WSN API instance
	 */
	private final TimedCache<String, FederatorWSN> instanceCache = new TimedCache<String, FederatorWSN>();

	/**
	 *
	 */
	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	/**
	 *
	 */
	private final String sessionManagementEndpointUrl;

	/**
	 *
	 */
	private Endpoint sessionManagementEndpoint;

	/**
	 *
	 */
	private SessionManagementPreconditions preconditions;

	public FederatorSessionManagement(BiMap<String, Set<String>> sessionManagementEndpointUrlPrefixSet,
									  String endpointUrlBase, String path, String reservationEndpointUrl) {

		this.sessionManagementEndpointUrlPrefixSet = sessionManagementEndpointUrlPrefixSet;
		this.endpointUrlBase = endpointUrlBase.endsWith("/") ? endpointUrlBase : endpointUrlBase + "/";
		this.sessionManagementEndpointUrl = endpointUrlBase + (path.startsWith("/") ? path.substring(1) : path);
		this.reservationEndpointUrl = reservationEndpointUrl;

		this.preconditions = new SessionManagementPreconditions();
		for (Set<String> endpointPrefixSet : sessionManagementEndpointUrlPrefixSet.values()) {
			this.preconditions.addServedUrnPrefixes(endpointPrefixSet.toArray(new String[endpointPrefixSet.size()]));
		}

	}

	public void start() throws Exception {
		String bindAllInterfacesUrl = UrlUtils.convertHostToZeros(sessionManagementEndpointUrl);

		log.debug("Starting Session Management federator...");
		log.debug("Endpoint URL: {}", sessionManagementEndpointUrl);
		log.debug("Binding  URL: {}", bindAllInterfacesUrl);

		sessionManagementEndpoint = Endpoint.publish(bindAllInterfacesUrl, this);

		log.info("Successfully started Session Management federator on {}", bindAllInterfacesUrl);
	}

	public void stop() throws Exception {

		if (sessionManagementEndpoint != null) {
			sessionManagementEndpoint.stop();
			log.info("Stopped Session Management federator on {}", sessionManagementEndpointUrl);
		}

		ExecutorUtils.shutdown(executorService, 5, TimeUnit.SECONDS);

	}

	private static class GetInstanceCallable implements Callable<GetInstanceCallable.Result> {

		private static class Result {

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

		final String wsnInstanceHash = calculateWSNInstanceHash(secretReservationKeys);

		// check if instance already exists and return it if that's the case
		FederatorWSN existingWSNFederatorInstance = instanceCache.get(wsnInstanceHash);
		if (existingWSNFederatorInstance != null) {

			String msg = "Found existing federating WSN API instance for secretReservationKeys {} with "
					+ "wsnInstanceHash {}. Returning its' sessionManagementEndpoint URL: {}";
			log.debug(msg, new Object[]{
					secretReservationKeys, wsnInstanceHash,
					existingWSNFederatorInstance.getWsnEndpointUrl()
			}
			);

			log.debug("Adding controller to the set of controllers: {}", controller);
			existingWSNFederatorInstance.addController(controller);

			return existingWSNFederatorInstance.getWsnEndpointUrl();
		}

		// create a WSN API instance under a generated secret URL and remember
		// to which set of secret URLs of federated
		// WSN API instances it maps
		String wsnEndpointUrl = endpointUrlBase + secureIdGenerator.getNextId() + "/wsn";
		String controllerEndpointUrl = endpointUrlBase + secureIdGenerator.getNextId() + "/controller";

		FederatorWSN federatorWSN = new FederatorWSN(wsnEndpointUrl, controllerEndpointUrl);
		try {

			federatorWSN.start();

			// add controller to set of upstream controllers so that output is
			// sent upwards
			federatorWSN.addController(controller);

		} catch (Exception e) {
			// TODO throw generic but declared exception
			throw WSNServiceHelper
					.createExperimentNotRunningException("The federator service could not be started.", e);
		}

		// delegate calls to the relevant federated Session Management API
		// endpoints (fork)

		final List<Future<GetInstanceCallable.Result>> futures = new ArrayList<Future<GetInstanceCallable.Result>>();
		final Map<String, List<SecretReservationKey>> serviceMapping = getServiceMapping(secretReservationKeys);

		for (Map.Entry<String, List<SecretReservationKey>> entry : serviceMapping.entrySet()) {

			String sessionManagementEndpointUrl = entry.getKey();

			GetInstanceCallable getInstanceCallable = new GetInstanceCallable(
					sessionManagementEndpointUrl, secretReservationKeys, controllerEndpointUrl
			);

			log.debug("Calling getInstance on {}", entry.getKey());

			futures.add(executorService.submit(getInstanceCallable));
		}

		// collect call results (join)
		for (Future<GetInstanceCallable.Result> future : futures) {
			try {

				GetInstanceCallable.Result result = future.get();

				Set<String> federatedUrnPrefixSet = convertToUrnPrefixSet(result.secretReservationKey);
				federatorWSN.addFederatedWSNEndpoint(result.federatedWSNInstanceEndpointUrl, federatedUrnPrefixSet);

			} catch (InterruptedException e) {

				// if one delegate call fails also fail
				log.error("" + e, e);
				// TODO use more generic error message
				throw WSNServiceHelper.createExperimentNotRunningException(e.getMessage(), e);

			} catch (ExecutionException e) {

				// if one delegate call fails also fail
				log.error("" + e, e);
				// TODO use more generic error message
				throw WSNServiceHelper.createExperimentNotRunningException(e.getMessage(), e);
			}
		}

		instanceCache.put(wsnInstanceHash, federatorWSN);

		// return the instantiated WSN API instance sessionManagementEndpoint
		// URL
		return federatorWSN.getWsnEndpointUrl();
	}

	/**
	 * Calculates an instance hash based on the set of (secretReservationKey,urnPrefix)-tuples that are provided in {@code
	 * secretReservationKeys}.
	 *
	 * @param secretReservationKeys the list of {@link eu.wisebed.testbed.api.wsn.v211.SecretReservationKey} instances that
	 *                              contain the (secretReservationKey,urnPrefix)-tuples used for the calculation
	 *
	 * @return an instance hash
	 */
	private String calculateWSNInstanceHash(List<SecretReservationKey> secretReservationKeys) {
		// secretReservationKey -> urnPrefix
		Map<String, String> map = new TreeMap<String, String>();
		for (SecretReservationKey secretReservationKey : secretReservationKeys) {
			map.put(secretReservationKey.getSecretReservationKey(), secretReservationKey.getUrnPrefix());
		}
		return "wsnInstanceHash" + map.hashCode();
	}

	/**
	 * Calculates the set of URN prefixes that are "buried" inside {@code secretReservationKeys}.
	 *
	 * @param secretReservationKeys the list of {@link eu.wisebed.testbed.api.wsn.v211.SecretReservationKey} instances
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
	 * Checks for a given list of {@link eu.wisebed.testbed.api.wsn.v211.SecretReservationKey} instances which federated
	 * Session Management endpoints are responsible for which set of URN prefixes.
	 *
	 * @param secretReservationKeys the list of {@link eu.wisebed.testbed.api.wsn.v211.SecretReservationKey} instances as
	 *                              passed in as parameter e.g. to {@link de.uniluebeck.itm.tr.wsn.federator.FederatorSessionManagement#getInstance(java.util.List,
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
	public void free(
			@WebParam(name = "secretReservationKeys", targetNamespace = "")
			List<SecretReservationKey> secretReservationKeys)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		preconditions.checkFreeArguments(secretReservationKeys);

		// check if instance still exists and if not simply exit
		String wsnInstanceHash = calculateWSNInstanceHash(secretReservationKeys);
		FederatorWSN federatorWSN = instanceCache.remove(wsnInstanceHash);
		if (federatorWSN == null) {
			log.warn("Trying to free a not existing instance for keys {} with a hash of {}.", secretReservationKeys,
					wsnInstanceHash
			);
			return;
		}

		// free the WSN API instance created by this implementation
		try {
			log.debug("Stopping FederatorWSN instance on URL {}", federatorWSN.getWsnEndpointUrl());
			federatorWSN.stop();
		} catch (Exception e) {
			log.error("" + e, e);
		}

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
				log
						.debug("Freeing WSN instance on {} for keys {}", sessionManagementEndpointUrl,
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

		// call all federated Session Management API endpoints and collect their
		// network information (fork)

		// wait for all calls to return (join)

		// merge results into one WiseML document
		// TODO check if there's something like an XPath expression that does
		// this job for us

		return null; // TODO implement

	}
}
