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
import com.google.common.collect.Maps;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import eu.wisebed.api.rs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.rs.federator.FederatorRSHelper.*;


@WebService(
		endpointInterface = "eu.wisebed.api.rs.RS",
		portName = "RSPort",
		serviceName = "RSService",
		targetNamespace = "urn:RSService"
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
	public void deleteReservation(
			@WebParam(name = "authenticationData", targetNamespace = "")
			List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "secretReservationKey", targetNamespace = "")
			List<SecretReservationKey> secretReservationKey)
			throws RSExceptionException, ReservervationNotFoundExceptionException {

		assertNotNull(authenticationData, "authenticationData");
		assertNotNull(secretReservationKey, "secretReservationKey");

		Map<RS, List<SecretReservationKey>> map = constructEndpointToReservationKeyMap(
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

	@Override
	public List<SecretReservationKey> makeReservation(
			@WebParam(name = "authenticationData", targetNamespace = "")
			List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "reservation", targetNamespace = "") ConfidentialReservationData reservation)
			throws AuthorizationExceptionException, RSExceptionException, ReservervationConflictExceptionException {

		assertNotNull(authenticationData, "authenticationData");
		assertNotNull(reservation, "reservation");

		assertTrue(authenticationData.size() > 0, "The parameter authenticationData must contain at least one element");
		assertTrue(reservation.getNodeURNs().size() > 0, "The reservation data must contain at least one node URN");

		assertUrnsServed(reservation.getNodeURNs());

		// run a set of parallel jobs to make a reservation on the federated rs services
		BiMap<RS, ConfidentialReservationData> reservationMap = constructEndpointToReservationMap(
				federationManager,
				reservation
		);

		BiMap<RS, List<SecretAuthenticationKey>> authenticationMap = constructEndpointToAuthenticationKeysMap(
				federationManager,
				authenticationData
		);

		// TODO fix check
		// assertAuthenticationForReservation(reservationMap, authenticationMap);

		Map<Future<List<SecretReservationKey>>, MakeReservationCallable> futures = Maps.newHashMap();

		// fork the parallel execution of reservations on federated services
		for (Map.Entry<RS, ConfidentialReservationData> entry : reservationMap.entrySet()) {
			MakeReservationCallable callable = new MakeReservationCallable(
					entry.getKey(),
					authenticationMap.get(entry.getKey()),
					entry.getValue()
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
				failMessages
						.add(Arrays.toString(futures.get(future).getReservation().getNodeURNs().toArray()) + ": " + e
								.getCause().getMessage()
						);
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

	@Override
	public List<PublicReservationData> getReservations(
			@WebParam(name = "from", targetNamespace = "") XMLGregorianCalendar from,
			@WebParam(name = "to", targetNamespace = "") XMLGregorianCalendar to) throws RSExceptionException {

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
				throwRSException("InterruptedException while getting reservations!", e);
			} catch (ExecutionException e) {
				throwRSException("ExecutionException while getting reservations!", e);
			}
		}

		return res;
	}

	@Override
	public List<ConfidentialReservationData> getConfidentialReservations(
			@WebParam(name = "secretAuthenticationKey", targetNamespace = "")
			List<SecretAuthenticationKey> secretAuthenticationKeys,
			@WebParam(name = "period", targetNamespace = "")
			GetReservations period) throws RSExceptionException {

		//check for null
		if (period.getFrom() == null || period.getTo() == null) {
			throw createRSExceptionException(
					"could not validate period from: " + period.getFrom() + " to: " + period.getTo()
			);
		}

		List<Future<List<ConfidentialReservationData>>> futures = newArrayList();
		Map<RS, List<SecretAuthenticationKey>> endpointToAuthenticationMap = constructEndpointToAuthenticationMap(
				federationManager,
				secretAuthenticationKeys
		);

		for (RS rs : federationManager.getEndpoints()) {
			GetConfidentialReservationsCallable callable = new GetConfidentialReservationsCallable(
					rs, endpointToAuthenticationMap.get(rs), period
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
				throwRSException(message, e);
			} catch (ExecutionException e) {
				String message = "ExecutionException while getting reservations!";
				log.warn(message);
				throwRSException(message, e);
			}
		}

		return confidentialReservationData;
	}

	@Override
	public List<ConfidentialReservationData> getReservation(
			@WebParam(name = "secretReservationKey", targetNamespace = "")
			List<SecretReservationKey> secretReservationKey)
			throws RSExceptionException, ReservervationNotFoundExceptionException {

		assertNotNull(secretReservationKey, "secretReservationKey");

		Map<RS, List<SecretReservationKey>> map = constructEndpointToReservationKeyMap(
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
				throwRSException("InterruptedException while getting reservation data!", e);
			} catch (ExecutionException e) {
				if (e.getCause() instanceof RSExceptionException) {
					throw (RSExceptionException) e.getCause();
				}
				if (e.getCause() instanceof ReservervationNotFoundExceptionException) {
					throw (ReservervationNotFoundExceptionException) e.getCause();
				}
				throwRSException("Unknown exception occurred!", e.getCause());
			}
		}

		return res;
	}

	private static void assertTrue(boolean bool, String errorMessage) throws RSExceptionException {
		if (!bool) {
			RSException exception = new RSException();
			exception.setMessage(errorMessage);
			throw new RSExceptionException(errorMessage, exception);
		}
	}

	private static void assertNotNull(Object obj, String paramName) throws RSExceptionException {
		if (obj != null) {
			return;
		}
		String msg = "Argument " + paramName + " must not be null!";
		RSException exception = new RSException();
		exception.setMessage(msg);
		throw new RSExceptionException(msg, exception);
	}

	/**
	 * Makes sure that for every reservation request there's a matching secret authentication key
	 *
	 * @param reservationMap	a mapping between federated endpoints and the reservation data belonging to them
	 * @param authenticationMap a mapping between federated endpoints and the authentication data belonging to them
	 *
	 * @throws AuthorizationExceptionException
	 *          if the user has insufficient authorization
	 */
	/*private void assertAuthenticationForReservation(
			final BiMap<RS, ConfidentialReservationData> reservationMap,
			final BiMap<RS, List<SecretAuthenticationKey>> authenticationMap)
			throws AuthorizationExceptionException {

		// TODO really check the matches
		boolean matches = reservationMap.size() == authenticationMap.size();

		if (!matches) {
			String msg = "Not for all reservation there's a matching secret authentication key!";
			AuthorizationException exception = new AuthorizationException();
			exception.setMessage(msg);
			throw new AuthorizationExceptionException(msg, exception);
		}
	}*/

	/**
	 * Checks if all nodes in {@code nodeURNs} are served by this federators federated rs services and throws an exception
	 * if not
	 *
	 * @param nodeURNs the node URNs to check
	 *
	 * @throws RSExceptionException if one of the node URNs is not served by this instance
	 */
	private void assertUrnsServed(List<String> nodeURNs) throws RSExceptionException {

		List<String> notServed = new LinkedList<String>();

		for (String nodeURN : nodeURNs) {
			String endpointUrlForNodeURN = federationManager.getEndpointUrlByNodeUrn(nodeURN);
			if (endpointUrlForNodeURN == null) {
				notServed.add(nodeURN);
			}
		}

		if (notServed.size() > 0) {
			String msg =
					"The node URNs " + Arrays.toString(notServed.toArray()) + " are not served by this RS instance!";
			RSException exception = new RSException();
			exception.setMessage(msg);
			throw new RSExceptionException(msg, exception);
		}

	}

	private RSExceptionException createRSExceptionException(String s) {
		RSException exception = new RSException();
		exception.setMessage(s);
		return new RSExceptionException(s, exception);
	}

	private List<SecretReservationKey> throwFailureException(List<String> failMessages) throws RSExceptionException {
		StringBuilder builder = new StringBuilder();
		builder.append("The following errors occurred: \n");
		for (String failMessage : failMessages) {
			builder.append(failMessage);
			builder.append("\n");
		}
		String msg = builder.toString();
		RSException exception = new RSException();
		exception.setMessage(msg);
		throw new RSExceptionException(msg, exception);
	}

	private void throwRSException(String msg, Throwable e) throws RSExceptionException {
		log.error(msg, e);
		RSException exception = new RSException();
		exception.setMessage(msg);
		throw new RSExceptionException(msg, exception, e);
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
