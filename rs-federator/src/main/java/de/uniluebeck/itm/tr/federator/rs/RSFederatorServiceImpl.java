/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
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

package de.uniluebeck.itm.tr.federator.rs;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.federator.utils.FederationManager;
import eu.wisebed.api.v3.common.*;
import eu.wisebed.api.v3.rs.AuthorizationFault;
import eu.wisebed.api.v3.rs.*;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;


@WebService(
		name = "RS",
		endpointInterface = "eu.wisebed.api.v3.rs.RS",
		portName = "RSPort",
		serviceName = "RSService",
		targetNamespace = "http://wisebed.eu/api/v3/rs"
)
public class RSFederatorServiceImpl extends AbstractService implements RSFederatorService {

	private static final Logger log = LoggerFactory.getLogger(RSFederatorServiceImpl.class);

	private final ExecutorService executorService;

	private final ServicePublisher servicePublisher;

	private final FederationManager<RS> federationManager;

	private final RSFederatorServiceConfig config;

	private ServicePublisherService jaxWsService;

	@Inject
	public RSFederatorServiceImpl(@Named(RS_FEDERATOR_EXECUTOR_SERVICE) final ExecutorService executorService,
								  final ServicePublisher servicePublisher,
								  final FederationManager<RS> federationManager,
								  final RSFederatorServiceConfig config) {
		this.executorService = checkNotNull(executorService);
		this.servicePublisher = checkNotNull(servicePublisher);
		this.federationManager = checkNotNull(federationManager);
		this.config = checkNotNull(config);
	}

	@Override
	public void deleteReservation(final List<SecretAuthenticationKey> secretAuthenticationKeys,
								  final List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {

		checkState(isRunning());
		assertNotNull(secretAuthenticationKeys, "Parameter secretAuthenticationKeys is null");
		assertNotNull(secretReservationKeys, "Parameter secretReservationKeys is null");

		Map<RS, DeleteReservationCallable> map = map(secretAuthenticationKeys, secretReservationKeys);

		// calling getReservation assures that every reservation exists
		getReservation(secretReservationKeys);

		// fork some processes to delete in parallel
		final List<Future<Void>> futures = newArrayList();
		for (DeleteReservationCallable callable : map.values()) {
			futures.add(executorService.submit(callable));
		}

		// join processes and check results
		List<String> failMessages = new LinkedList<String>();
		for (Future<Void> future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				log.error("InterruptedException while executing deleteReservation!", e);
			} catch (ExecutionException e) {
				log.error("ExecutionException while executing deleteReservation!", e);
				failMessages.add(e.getCause().getMessage());
			}
		}

		if (failMessages.size() > 0) {
			throwFailureException(failMessages);
		}

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
			if (jaxWsService != null) {
				jaxWsService.stopAndWait();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public List<SecretReservationKey> makeReservation(final List<SecretAuthenticationKey> secretAuthenticationKeys,
													  final List<NodeUrn> nodeUrns,
													  final DateTime from,
													  final DateTime to,
													  final String description,
													  final List<KeyValuePair> opts)
			throws AuthorizationFault, RSFault_Exception, ReservationConflictFault_Exception {

		try {

			checkState(isRunning());
			checkNotNull(secretAuthenticationKeys, "Parameter secretAuthenticationKeys is null");
			checkNotNull(nodeUrns, "Parameter nodeUrns is null");
			checkNotNull(from, "Parameter from is null");
			checkNotNull(to, "Parameter to is null");

			checkArgument(!secretAuthenticationKeys.isEmpty(), "Parameter secretAuthenticationKeys must not be empty");
			checkArgument(!nodeUrns.isEmpty(), "Parameter nodeUrns must contain at least one node URN");

		} catch (Exception e) {
			RSFault exception = new RSFault();
			exception.setMessage(e.getMessage());
			throw new RSFault_Exception(e.getMessage(), exception);
		}

		assertUrnsServed(nodeUrns);

		// run a set of parallel jobs to make a reservation on the federated rs services

		BiMap<RS, MakeReservationCallable> map = HashBiMap.create(federationManager.getEndpoints().size());

		for (NodeUrn nodeUrn : nodeUrns) {

			RS rs = federationManager.getEndpointByNodeUrn(nodeUrn);

			final SecretAuthenticationKey sak = getSAKByNodeUrn(secretAuthenticationKeys, nodeUrn);
			MakeReservationCallable callable = map.get(rs);
			if (callable == null) {
				callable = new MakeReservationCallable(rs, from, to, description, opts);
				map.put(rs, callable);
			}

			callable.getSecretAuthenticationKeys().add(sak);
			callable.getNodeUrns().add(nodeUrn);
		}

		// TODO fix check
		// assertAuthenticationForReservation(reservationMap, authenticationMap);

		Map<Future<List<SecretReservationKey>>, MakeReservationCallable> futures = Maps.newHashMap();

		// fork the parallel execution of reservations on federated services
		for (MakeReservationCallable callable : map.values()) {
			futures.put(executorService.submit(callable), callable);
		}

		// join the parallel execution and check if one or more of the jobs failed
		boolean failed = false;

		Map<MakeReservationCallable, List<SecretReservationKey>> succeeded = Maps.newHashMap();
		List<String> failMessages = newArrayList();

		for (Future<List<SecretReservationKey>> future : futures.keySet()) {
			try {
				List<SecretReservationKey> reservationKeys = future.get();
				succeeded.put(futures.get(future), reservationKeys);
			} catch (InterruptedException e) {
				failed = true;
			} catch (ExecutionException e) {
				failed = true;
				final String nodeUrnsString = Arrays.toString(futures.get(future).getNodeUrns().toArray());
				final String msg = nodeUrnsString + ": " + e.getCause().getMessage();
				failMessages.add(msg);
			}
		}

		// if a job failed delete the successful reservations and return failure
		if (failed) {

			// since this could also fail, add the according message
			try {
				undoReservations(succeeded);
			} catch (RSFault_Exception e) {
				failMessages.add(e.getMessage());
			}

			// construct error and throw exception
			return throwFailureException(failMessages);
		}

		// return secret reservation keys (all jobs successful)
		List<SecretReservationKey> res = newArrayList();
		for (List<SecretReservationKey> secretReservationKeyList : succeeded.values()) {
			res.addAll(secretReservationKeyList);
		}

		return res;
	}

	private SecretAuthenticationKey getSAKByNodeUrn(final List<SecretAuthenticationKey> saks, final NodeUrn nodeUrn) {

		final ImmutableSet<NodeUrnPrefix> urnPrefixes = federationManager.getUrnPrefixesByNodeUrn(nodeUrn);

		for (SecretAuthenticationKey secretAuthenticationKey : saks) {
			if (urnPrefixes.contains(secretAuthenticationKey.getUrnPrefix())) {
				return secretAuthenticationKey;
			}
		}

		throw new IllegalArgumentException("No federated testbed for node \"" + nodeUrn + "\" found");
	}

	@Override
	public List<PublicReservationData> getReservations(final DateTime from, final DateTime to,
													   final Integer offset, final Integer amount)
			throws RSFault_Exception {

		checkState(isRunning());
		assertNotNull(from, "from");
		assertNotNull(to, "to");

		// fork processes to collect reservations from federated services
		List<Future<List<PublicReservationData>>> futures = newArrayList();
		for (RS rs : federationManager.getEndpoints()) {
			futures.add(executorService.submit(new GetReservationsCallable(rs, from, to, offset, amount)));
		}

		// join processes and collect results
		List<PublicReservationData> res = newArrayList();
		for (Future<List<PublicReservationData>> future : futures) {
			try {
				res.addAll(future.get());
			} catch (InterruptedException e) {
				throwRSFault("InterruptedException while getting reservations!", e);
			} catch (ExecutionException e) {
				throwRSFault("ExecutionException while getting reservations!", e);
			}
		}

		return res;
	}

	@Override
	public List<ConfidentialReservationData> getConfidentialReservations(
			final List<SecretAuthenticationKey> secretAuthenticationKey,
			final DateTime from,
			final DateTime to,
			final Integer offset,
			final Integer amount)
			throws AuthorizationFault, RSFault_Exception {

		checkState(isRunning());

		//check for null
		if (from == null || to == null) {
			throw createRSFault_Exception(
					"could not validate period from: " + from + " to: " + to
			);
		}

		List<Future<List<ConfidentialReservationData>>> futures = newArrayList();
		Map<RS, List<SecretAuthenticationKey>> endpointToAuthenticationMap = constructEndpointToAuthenticationMap(
				federationManager,
				secretAuthenticationKey
		);

		for (RS rs : federationManager.getEndpoints()) {
			GetConfidentialReservationsCallable callable = new GetConfidentialReservationsCallable(
					rs, endpointToAuthenticationMap.get(rs), from, to, offset, amount
			);
			futures.add(executorService.submit(callable));
		}

		List<ConfidentialReservationData> confidentialReservationData = newArrayList();
		for (Future<List<ConfidentialReservationData>> future : futures) {
			try {
				confidentialReservationData.addAll(future.get());
			} catch (InterruptedException e) {
				String message = "InterruptedException while getting reservations!";
				log.warn(message);
				throwRSFault(message, e);
			} catch (ExecutionException e) {
				String message = "ExecutionException while getting reservations!";
				log.warn(message);
				throwRSFault(message, e);
			}
		}

		return confidentialReservationData;
	}

	@Override
	public List<ConfidentialReservationData> getReservation(final List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {

		checkState(isRunning());
		assertNotNull(secretReservationKeys, "Parameter secretReservationKeys is null");

		Map<RS, List<SecretReservationKey>> map = constructEndpointToReservationKeyMap(secretReservationKeys);

		// fork some processes to fetch the individual reservation data
		List<Future<List<ConfidentialReservationData>>> futures = newArrayList();
		for (Map.Entry<RS, List<SecretReservationKey>> entry : map.entrySet()) {
			final GetReservationCallable callable = new GetReservationCallable(entry.getKey(), entry.getValue());
			futures.add(executorService.submit(callable));
		}

		// join processes and unify their results
		List<ConfidentialReservationData> res = newArrayList();
		for (Future<List<ConfidentialReservationData>> future : futures) {
			try {
				res.addAll(future.get());
			} catch (InterruptedException e) {
				throwRSFault("InterruptedException while getting reservation data!", e);
			} catch (ExecutionException e) {
				if (e.getCause() instanceof RSFault_Exception) {
					throw (RSFault_Exception) e.getCause();
				}
				if (e.getCause() instanceof UnknownSecretReservationKeyFault) {
					throw (UnknownSecretReservationKeyFault) e.getCause();
				}
				throwRSFault("Unknown exception occurred!", e.getCause());
			}
		}

		return res;
	}

	private static void assertNotNull(Object obj, String paramName) throws RSFault_Exception {
		if (obj != null) {
			return;
		}
		String msg = "Argument " + paramName + " must not be null!";
		RSFault exception = new RSFault();
		exception.setMessage(msg);
		throw new RSFault_Exception(msg, exception);
	}

	/**
	 * Makes sure that for every reservation request there's a matching secret authentication key
	 *
	 * @param reservationMap    a mapping between federated endpoints and the reservation data belonging to them
	 * @param authenticationMap a mapping between federated endpoints and the authentication data belonging to them
	 *
	 * @throws AuthorizationFault_Exception
	 *          if the user has insufficient authorization
	 */
	/*private void assertAuthenticationForReservation(
			final BiMap<RS, ConfidentialReservationData> reservationMap,
			final BiMap<RS, List<SecretAuthenticationKey>> authenticationMap)
			throws AuthorizationFault_Exception {

		// TODO really check the matches
		boolean matches = reservationMap.size() == authenticationMap.size();

		if (!matches) {
			String msg = "Not for all reservation there's a matching secret authentication key!";
			AuthorizationException exception = new AuthorizationException();
			exception.setMessage(msg);
			throw new AuthorizationFault_Exception(msg, exception);
		}
	}*/


	/**
	 * Checks if all nodes in {@code nodeUrns} are served by this federators federated rs services and throws an exception
	 * if not
	 *
	 * @param nodeUrns
	 * 		the node URNs to check
	 *
	 * @throws RSFault_Exception
	 * 		if one of the node URNs is not served by this instance
	 */
	private void assertUrnsServed(List<NodeUrn> nodeUrns) throws RSFault_Exception {

		List<NodeUrn> notServed = new LinkedList<NodeUrn>();

		for (NodeUrn nodeUrn : nodeUrns) {
			if (federationManager.getEndpointUrlByNodeUrn(nodeUrn) == null) {
				notServed.add(nodeUrn);
			}
		}

		if (notServed.size() > 0) {
			String msg = "The nodes " + Arrays.toString(notServed.toArray()) + " are not served by this RS instance!";
			RSFault exception = new RSFault();
			exception.setMessage(msg);
			throw new RSFault_Exception(msg, exception);
		}

	}

	private RSFault_Exception createRSFault_Exception(String s) {
		RSFault exception = new RSFault();
		exception.setMessage(s);
		return new RSFault_Exception(s, exception);
	}

	private List<SecretReservationKey> throwFailureException(List<String> failMessages) throws RSFault_Exception {
		StringBuilder builder = new StringBuilder();
		builder.append("The following errors occurred: \n");
		for (String failMessage : failMessages) {
			builder.append(failMessage);
			builder.append("\n");
		}
		String msg = builder.toString();
		RSFault exception = new RSFault();
		exception.setMessage(msg);
		throw new RSFault_Exception(msg, exception);
	}

	private void throwRSFault(String msg, Throwable e) throws RSFault_Exception {
		log.error(msg, e);
		RSFault exception = new RSFault();
		exception.setMessage(msg);
		throw new RSFault_Exception(msg, exception, e);
	}

	private void undoReservations(Map<MakeReservationCallable, List<SecretReservationKey>> succeeded)
			throws RSFault_Exception {

		List<Future> futures = new ArrayList<Future>(succeeded.size());

		// fork processes to delete reservations
		for (Map.Entry<MakeReservationCallable, List<SecretReservationKey>> entry : succeeded.entrySet()) {

			final RS rs = entry.getKey().getRs();
			final List<SecretAuthenticationKey> secretAuthenticationKeys = entry.getKey().getSecretAuthenticationKeys();
			final List<SecretReservationKey> secretReservationKeys = entry.getValue();

			final DeleteReservationCallable callable = new DeleteReservationCallable(rs);
			callable.getSecretAuthenticationKeys().addAll(secretAuthenticationKeys);
			callable.getSecretReservationKeys().addAll(secretReservationKeys);

			futures.add(executorService.submit(callable));
		}

		// join deletion processes
		for (Future future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				final String msg = "InterruptedException while trying to delete reservation!";
				log.error(msg, e);
				throwRSFault(msg, e);
			} catch (ExecutionException e) {
				final String msg = "Exception occurred while deleting reservation!";
				log.warn(msg, e);
				throwRSFault(msg, e);
			}
		}
	}

	private static Map<RS, List<SecretAuthenticationKey>> constructEndpointToAuthenticationMap(
			final FederationManager<RS> federationManager,
			final List<SecretAuthenticationKey> secretAuthenticationKey) throws RSFault_Exception {

		Map<RS, List<SecretAuthenticationKey>> map = newHashMap();

		for (SecretAuthenticationKey authenticationKey : secretAuthenticationKey) {

			RS rs = federationManager.getEndpointByUrnPrefix(authenticationKey.getUrnPrefix());

			if (rs == null) {
				String msg = "The node URN prefix " +
						authenticationKey.getUrnPrefix() +
						" is not served by this RS instance!";

				RSFault exception = new RSFault();
				exception.setMessage(msg);
				throw new RSFault_Exception(msg, exception);
			}

			List<SecretAuthenticationKey> secretReservationKeyList = map.get(rs);
			if (secretReservationKeyList == null) {
				secretReservationKeyList = new LinkedList<SecretAuthenticationKey>();
				map.put(rs, secretReservationKeyList);
			}
			secretReservationKeyList.add(authenticationKey);
		}
		return map;
	}

	private Map<RS, DeleteReservationCallable> map(
			final List<SecretAuthenticationKey> secretAuthenticationKeys,
			final List<SecretReservationKey> secretReservationKeys) throws RSFault_Exception {

		final Map<RS, DeleteReservationCallable> map = newHashMap();

		for (SecretAuthenticationKey secretAuthenticationKey : secretAuthenticationKeys) {

			final NodeUrnPrefix urnPrefix = secretAuthenticationKey.getUrnPrefix();
			final RS rs = assertServed(federationManager.getEndpointByUrnPrefix(urnPrefix), urnPrefix);

			addOrGetDeleteReservationCallable(map, rs).addSecretAuthenticationKey(secretAuthenticationKey);
		}

		for (SecretReservationKey secretReservationKey : secretReservationKeys) {

			final NodeUrnPrefix urnPrefix = secretReservationKey.getUrnPrefix();
			final RS rs = assertServed(federationManager.getEndpointByUrnPrefix(urnPrefix), urnPrefix);

			addOrGetDeleteReservationCallable(map, rs).addSecretReservationKey(secretReservationKey);
		}

		return map;
	}

	private DeleteReservationCallable addOrGetDeleteReservationCallable(final Map<RS, DeleteReservationCallable> map,
																		final RS rs) {
		DeleteReservationCallable callable = map.get(rs);
		if (callable == null) {
			callable = new DeleteReservationCallable(rs);
			map.put(rs, callable);
		}
		return callable;
	}

	private RS assertServed(final RS rs, final NodeUrnPrefix urnPrefix) throws RSFault_Exception {
		if (rs == null) {
			String msg = "The node URN prefix " + urnPrefix + " is not served by this RS instance!";
			RSFault exception = new RSFault();
			exception.setMessage(msg);
			throw new RSFault_Exception(msg, exception);
		}
		return rs;
	}

	private Map<RS, List<SecretReservationKey>> constructEndpointToReservationKeyMap(
			final List<SecretReservationKey> secretReservationKey) throws RSFault_Exception {

		Map<RS, List<SecretReservationKey>> map = newHashMap();

		for (SecretReservationKey reservationKey : secretReservationKey) {

			RS rs = federationManager.getEndpointByUrnPrefix(reservationKey.getUrnPrefix());

			if (rs == null) {
				String msg = "The node URN prefix "
						+ reservationKey.getUrnPrefix() +
						" is not served by this RS instance!";

				RSFault exception = new RSFault();
				exception.setMessage(msg);
				throw new RSFault_Exception(msg, exception);
			}

			List<SecretReservationKey> secretReservationKeyList = map.get(rs);
			if (secretReservationKeyList == null) {
				secretReservationKeyList = new LinkedList<SecretReservationKey>();
				map.put(rs, secretReservationKeyList);
			}
			secretReservationKeyList.add(reservationKey);

		}
		return map;
	}

}
