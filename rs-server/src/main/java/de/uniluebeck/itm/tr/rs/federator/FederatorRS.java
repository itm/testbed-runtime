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

package de.uniluebeck.itm.tr.rs.federator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;


@WebService(
		name = "RS",
		endpointInterface = "eu.wisebed.api.v3.rs.RS",
		portName = "RSPort",
		serviceName = "RSService",
		targetNamespace = "http://wisebed.eu/api/v3/rs"
)
public class FederatorRS implements RS {

	private static final Logger log = LoggerFactory.getLogger(FederatorRS.class);

	private ExecutorService executorService;

	private FederationManager<RS> federationManager;

	public FederatorRS(final FederationManager<RS> federationManager,
					   final ExecutorService executorService) {
		this.federationManager = federationManager;
		this.executorService = executorService;
	}

	@Override
	public void deleteReservation(final List<SecretReservationKey> secretReservationKey)
			throws RSFault_Exception, ReservationNotFoundFault_Exception {

		assertNotNull(secretReservationKey, "secretReservationKey");

		Map<RS, List<SecretReservationKey>> map = FederatorRSHelper.constructEndpointToReservationKeyMap(
				federationManager,
				secretReservationKey
		);

		// calling getReservation assures that every reservation exists
		getReservation(secretReservationKey);

		// fork some processes to delete in parallel
		List<Future<Void>> futures = newArrayList();
		for (Map.Entry<RS, List<SecretReservationKey>> entry : map.entrySet()) {
			DeleteReservationCallable deleteReservationCallable = new DeleteReservationCallable(
					entry.getKey(),
					entry.getValue()
			);
			futures.add(executorService.submit(deleteReservationCallable));
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

	private static class MakeReservationArguments {

		public Set<SecretAuthenticationKey> secretAuthenticationKeys;

		public Set<String> nodeUrns;

		public XMLGregorianCalendar from;

		public XMLGregorianCalendar to;

		private MakeReservationArguments(final Set<SecretAuthenticationKey> secretAuthenticationKeys,
										 final Set<String> nodeUrns,
										 final XMLGregorianCalendar from,
										 final XMLGregorianCalendar to) {
			this.secretAuthenticationKeys = secretAuthenticationKeys;
			this.nodeUrns = nodeUrns;
			this.from = from;
			this.to = to;
		}
	}

	@Override
	public List<SecretReservationKey> makeReservation(final List<SecretAuthenticationKey> secretAuthenticationKeys,
													  final List<String> nodeUrns,
													  final XMLGregorianCalendar from,
													  final XMLGregorianCalendar to)
			throws AuthorizationFault_Exception, RSFault_Exception, ReservationConflictFault_Exception {

		try {

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

		BiMap<RS, MakeReservationArguments> map = HashBiMap.create(federationManager.getEndpoints().size());

		for (String nodeUrn : nodeUrns) {

			RS rs = federationManager.getEndpointByNodeUrn(nodeUrn);

			final SecretAuthenticationKey sak = getSAKByNodeUrn(secretAuthenticationKeys, nodeUrn);
			MakeReservationArguments args = map.get(rs);
			if (args == null) {
				args = new MakeReservationArguments(newHashSet(sak), newHashSet(nodeUrn), from, to);
				map.put(rs, args);
			} else {
				args.secretAuthenticationKeys.add(sak);
				args.nodeUrns.add(nodeUrn);
			}
		}

		// TODO fix check
		// assertAuthenticationForReservation(reservationMap, authenticationMap);

		Map<Future<List<SecretReservationKey>>, MakeReservationCallable> futures = Maps.newHashMap();

		// fork the parallel execution of reservations on federated services
		for (Map.Entry<RS, MakeReservationArguments> entry : map.entrySet()) {

			MakeReservationCallable callable = new MakeReservationCallable(
					entry.getKey(),
					newArrayList(entry.getValue().secretAuthenticationKeys),
					newArrayList(entry.getValue().nodeUrns),
					entry.getValue().from,
					entry.getValue().to
			);

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

			undoReservations(succeeded);

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

	private SecretAuthenticationKey getSAKByNodeUrn(final List<SecretAuthenticationKey> saks, final String nodeUrn) {

		final ImmutableSet<String> urnPrefixes = federationManager.getUrnPrefixesByNodeUrn(nodeUrn);

		for (SecretAuthenticationKey secretAuthenticationKey : saks) {
			if (urnPrefixes.contains(secretAuthenticationKey.getUrnPrefix())) {
				return secretAuthenticationKey;
			}
		}

		throw new IllegalArgumentException("No federated testbed for node \"" + nodeUrn + "\" found");
	}

	@Override
	public List<PublicReservationData> getReservations(final XMLGregorianCalendar from,
													   final XMLGregorianCalendar to) throws RSFault_Exception {

		assertNotNull(from, "from");
		assertNotNull(to, "to");

		// fork processes to collect reservations from federated services
		List<Future<List<PublicReservationData>>> futures = newArrayList();
		for (RS rs : federationManager.getEndpoints()) {
			futures.add(executorService.submit(new GetReservationsCallable(rs, from, to)));
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
			final XMLGregorianCalendar from,
			final XMLGregorianCalendar to) throws RSFault_Exception {

		//check for null
		if (from == null || to == null) {
			throw createRSFault_Exception(
					"could not validate period from: " + from + " to: " + to
			);
		}

		List<Future<List<ConfidentialReservationData>>> futures = newArrayList();
		Map<RS, List<SecretAuthenticationKey>> endpointToAuthenticationMap = FederatorRSHelper
				.constructEndpointToAuthenticationMap(
						federationManager,
						secretAuthenticationKey
				);

		for (RS rs : federationManager.getEndpoints()) {
			GetConfidentialReservationsCallable callable = new GetConfidentialReservationsCallable(
					rs, endpointToAuthenticationMap.get(rs), from, to
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
	public List<ConfidentialReservationData> getReservation(final List<SecretReservationKey> secretReservationKey)
			throws RSFault_Exception, ReservationNotFoundFault_Exception {

		assertNotNull(secretReservationKey, "secretReservationKey");

		Map<RS, List<SecretReservationKey>> map = FederatorRSHelper.constructEndpointToReservationKeyMap(
				federationManager,
				secretReservationKey
		);

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
				if (e.getCause() instanceof ReservationNotFoundFault_Exception) {
					throw (ReservationNotFoundFault_Exception) e.getCause();
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
	private void assertUrnsServed(List<String> nodeUrns) throws RSFault_Exception {

		List<String> notServed = new LinkedList<String>();

		for (String nodeURN : nodeUrns) {
			String endpointUrlForNodeURN = federationManager.getEndpointUrlByNodeUrn(nodeURN);
			if (endpointUrlForNodeURN == null) {
				notServed.add(nodeURN);
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

	private void undoReservations(Map<MakeReservationCallable, List<SecretReservationKey>> succeeded) {
		RS rs;
		List<SecretReservationKey> reservationKeys;
		List<Future> futures = new ArrayList<Future>(succeeded.size());

		// fork processes to delete reservations
		for (Map.Entry<MakeReservationCallable, List<SecretReservationKey>> entry : succeeded.entrySet()) {

			rs = entry.getKey().getRs();
			reservationKeys = entry.getValue();

			DeleteReservationCallable callable = new DeleteReservationCallable(rs, reservationKeys);
			futures.add(executorService.submit(callable));
		}

		// join deletion processes
		for (Future future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				log.error("InterruptedException while trying to delete reservation!", e);
			} catch (ExecutionException e) {
				log.warn("Exception occurred while deleting reservation!", e);
			}
		}

	}

}
