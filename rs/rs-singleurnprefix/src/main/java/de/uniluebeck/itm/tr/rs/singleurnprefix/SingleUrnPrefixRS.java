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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.rs.singleurnprefix;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.internal.Nullable;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.api.rs.*;
import eu.wisebed.api.rs.SecretAuthenticationKey;
import eu.wisebed.api.snaa.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

@WebService(endpointInterface = "eu.wisebed.api.rs.RS", portName = "RSPort", serviceName = "RSService",
		targetNamespace = "urn:RSService")
public class SingleUrnPrefixRS implements RS {

	private static final Logger log = LoggerFactory.getLogger(SingleUrnPrefixRS.class);

	@Inject
	@Named("SingleUrnPrefixRS.urnPrefix")
	private String urnPrefix;

	@Inject
	private SNAA snaa;

	@Inject
	private RSPersistence persistence;

	@Inject
	@Nullable
	@Named("SingleUrnPrefixRS.servedNodeUrns")
	private Provider<String[]> servedNodeUrns;

	@WebResult(name = "secretReservationKey")
	@Override
	public List<SecretReservationKey> makeReservation(
			@WebParam(name = "authenticationData", targetNamespace = "")
			List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "reservation") ConfidentialReservationData reservation)
			throws AuthorizationExceptionException, ReservervationConflictExceptionException, RSExceptionException {

		checkNotNull(
				reservation.getUserData() != null && !"".equals(reservation.getUserData()),
				"The field userData must be set!"
		);

		checkArgumentValid(reservation);
		checkArgumentValidAuthentication(authenticationData);
		checkNodesServed(reservation.getNodeURNs());

		SecretAuthenticationKey secretAuthenticationKey = authenticationData.get(0);

		checkAuthorization(secretAuthenticationKey, Actions.MAKE_RESERVATION);
		checkNodesAvailable(reservation);

		return makeReservationInternal(reservation, secretAuthenticationKey);

	}

	private List<SecretReservationKey> makeReservationInternal(final ConfidentialReservationData reservation,
															   final SecretAuthenticationKey secretAuthenticationKey)
			throws RSExceptionException {

		ConfidentialReservationData crd = new ConfidentialReservationData();
		crd.setFrom(reservation.getFrom());
		crd.setTo(reservation.getTo());
		crd.setUserData(reservation.getUserData());
		crd.getNodeURNs().addAll(reservation.getNodeURNs());

		Data data = new Data();
		data.setUrnPrefix(secretAuthenticationKey.getUrnPrefix());
		data.setUsername(secretAuthenticationKey.getUsername());

		crd.getData().add(data);

		try {

			SecretReservationKey secretReservationKey = persistence.addReservation(crd, urnPrefix);

			data.setSecretReservationKey(secretReservationKey.getSecretReservationKey());

			List<SecretReservationKey> keys = new ArrayList<SecretReservationKey>();
			keys.add(secretReservationKey);
			return keys;

		} catch (Exception e) {
			throw createRSExceptionException(e.getMessage());
		}
	}

	@Override
	public List<ConfidentialReservationData> getReservation(
			@WebParam(name = "secretReservationKey") List<SecretReservationKey> secretReservationKeys)
			throws RSExceptionException, ReservervationNotFoundExceptionException {

		checkNotNull(secretReservationKeys, "Parameter secretReservationKeys is null!");
		checkArgumentValidReservation(secretReservationKeys);

		SecretReservationKey secretReservationKey = secretReservationKeys.get(0);
		ConfidentialReservationData reservation = persistence.getReservation(secretReservationKey);

		if (reservation == null) {
			String msg = "Reservation not found for key " + secretReservationKey;
			ReservervationNotFoundException exception = new ReservervationNotFoundException();
			exception.setMessage(msg);
			throw new ReservervationNotFoundExceptionException(msg, exception);
		}

		List<ConfidentialReservationData> res = new LinkedList<ConfidentialReservationData>();
		res.add(reservation);
		return res;
	}

	@Override
	public void deleteReservation(
			@WebParam(name = "authenticationData", targetNamespace = "")
			List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "secretReservationKey", targetNamespace = "")
			List<SecretReservationKey> secretReservationKeys)
			throws RSExceptionException, ReservervationNotFoundExceptionException {

		checkNotNull(authenticationData, "Parameter authenticationData is null!");
		checkNotNull(secretReservationKeys, "Parameter secretReservationKeys is null!");

		checkArgumentValidReservation(secretReservationKeys);

		// TODO check authentication (https://github.com/itm/testbed-runtime/issues/47)
		//checkArgumentValidAuthentication(authenticationData);
		//try {
		//	checkAuthorization(authenticationData.get(0), Actions.DELETE_RESERVATION);
		//} catch (AuthorizationExceptionException e) {
		//	throwRSExceptionException(e);
		//}

		SecretReservationKey secretReservationKeyToDelete = secretReservationKeys.get(0);
		ConfidentialReservationData reservationToDelete = persistence.getReservation(secretReservationKeyToDelete);

		checkNotAlreadyStarted(reservationToDelete);

		persistence.deleteReservation(secretReservationKeyToDelete);

		log.debug("Deleted reservation {}", reservationToDelete);
	}

	private void checkNotAlreadyStarted(final ConfidentialReservationData reservation)
			throws RSExceptionException {

		DateTime reservationFrom = new DateTime(reservation.getFrom().toGregorianCalendar());

		if (reservationFrom.isBeforeNow()) {
			final String msg = "You are not allowed to delete reservations that have already started.";
			throw createRSExceptionException(msg);
		}
	}

	@Override
	public List<PublicReservationData> getReservations(
			@WebParam(name = "from", targetNamespace = "") XMLGregorianCalendar from,
			@WebParam(name = "to", targetNamespace = "") XMLGregorianCalendar to) throws RSExceptionException {

		Preconditions.checkNotNull(from, "Parameter from date is null or empty");
		Preconditions.checkNotNull(to, "Parameter to date is null or empty");

		Interval request =
				new Interval(new DateTime(from.toGregorianCalendar()), new DateTime(to.toGregorianCalendar()));
		List<PublicReservationData> res = convertToPublic(persistence.getReservations(request));

		log.debug("Found " + res.size() + " reservations from " + from + " until " + to);
		return res;
	}

	@Override
	public List<ConfidentialReservationData> getConfidentialReservations(
			@WebParam(name = "secretAuthenticationKey", targetNamespace = "")
			List<SecretAuthenticationKey> secretAuthenticationKeys,
			@WebParam(name = "period", targetNamespace = "") GetReservations period) throws RSExceptionException {

		checkNotNull(period, "Parameter period is null!");
		checkNotNull(secretAuthenticationKeys, "Parameter secretAuthenticationKeys is null!");

		checkArgumentValid(period);
		checkArgumentValidAuthentication(secretAuthenticationKeys);

		SecretAuthenticationKey key = secretAuthenticationKeys.get(0);

		try {
			checkAuthorization(key, Actions.GET_CONFIDENTIAL_RESERVATION);
		} catch (Exception e) {
			return throwRSExceptionException(e);
		}

		Interval interval = new Interval(new DateTime(period.getFrom().toGregorianCalendar()),
				new DateTime(period.getTo().toGregorianCalendar())
		);

		List<ConfidentialReservationData> reservationsOfAllUsersInInterval = persistence.getReservations(interval);
		List<ConfidentialReservationData> reservationsOfAuthenticatedUserInInterval = newArrayList();

		for (ConfidentialReservationData crd : reservationsOfAllUsersInInterval) {
			boolean sameUser = crd.getUserData().equals(key.getUsername());
			if (sameUser) {
				reservationsOfAuthenticatedUserInInterval.add(crd);
			}
		}

		return reservationsOfAuthenticatedUserInInterval;
	}

	public void checkArgumentValidAuthentication(List<SecretAuthenticationKey> authenticationData)
			throws RSExceptionException {

		// Check if authentication data has been supplied
		if (authenticationData == null || authenticationData.size() != 1) {
			String msg = "No or too much authentication data supplied -> error.";
			log.warn(msg);
			RSException exception = new RSException();
			exception.setMessage(msg);
			throw new RSExceptionException(msg, exception);
		}

		SecretAuthenticationKey sak = authenticationData.get(0);
		if (!urnPrefix.equals(sak.getUrnPrefix())) {
			String msg = "Not serving urn prefix " + sak.getUrnPrefix();
			log.warn(msg);
			RSException exception = new RSException();
			exception.setMessage(msg);
			throw new RSExceptionException(msg, exception);
		}
	}

	private boolean checkAuthentication(SecretAuthenticationKey key, Action action) throws RSExceptionException,
			AuthorizationExceptionException, AuthenticationExceptionException {

		log.debug("Checking authorization for key: " + key + " and action: " + action);
		boolean authorized;

		eu.wisebed.api.snaa.SecretAuthenticationKey sak = new eu.wisebed.api.snaa.SecretAuthenticationKey();
		sak.setSecretAuthenticationKey(key.getSecretAuthenticationKey());
		sak.setUrnPrefix(key.getUrnPrefix());
		sak.setUsername(key.getUsername());

		List<eu.wisebed.api.snaa.SecretAuthenticationKey> saks = Lists.newArrayList();
		saks.add(sak);

		// Invoke isAuthorized
		try {

			authorized = snaa.isAuthorized(saks, action);
			log.debug("Authorization result: " + authorized);

			if (!authorized) {
				AuthorizationException e = new AuthorizationException();
				String msg = "Authorization failed";
				e.setMessage(msg);
				log.warn(msg, e);
				throw new AuthorizationExceptionException(msg, e);
			}

		} catch (SNAAExceptionException e) {
			throwRSExceptionException(e);
		}

		return true;
	}

	private void checkAuthorization(final SecretAuthenticationKey secretAuthenticationKey, final Action action)
			throws RSExceptionException, AuthorizationExceptionException {

		if (snaa != null) {
			try {
				checkAuthentication(secretAuthenticationKey, action);
			} catch (AuthenticationExceptionException e) {
				throw createAuthorizationExceptionException(e);
			}
		}
	}

	private RSExceptionException createRSExceptionException(String message) {
		RSException exception = new RSException();
		exception.setMessage(message);
		return new RSExceptionException(message, exception);
	}

	private AuthorizationExceptionException createAuthorizationExceptionException(AuthenticationExceptionException e) {
		AuthorizationException exception = new AuthorizationException();
		exception.setMessage(e.getMessage());
		return new AuthorizationExceptionException(e.getMessage(), exception, e);
	}

	private PublicReservationData convertToPublic(ConfidentialReservationData confidentialReservationData) {
		PublicReservationData publicReservationData = new PublicReservationData();
		publicReservationData.setFrom(confidentialReservationData.getFrom());
		publicReservationData.setTo(confidentialReservationData.getTo());
		publicReservationData.setUserData(confidentialReservationData.getUserData());
		publicReservationData.getNodeURNs().addAll(confidentialReservationData.getNodeURNs());
		return publicReservationData;
	}

	private void checkNodesServed(List<String> nodeUrns) throws RSExceptionException {

		// Check if we serve all node urns by urnPrefix
		for (String nodeUrn : nodeUrns) {
			if (!nodeUrn.startsWith(urnPrefix)) {
				throw createRSExceptionException(
						"Not responsible for node URN " + nodeUrn + ", only serving prefix: " + urnPrefix
				);
			}
		}

		// Ask Session Management Endpoint of the testbed we're responsible for for it's network description
		// and check if the individual node urns of the reservation are existing
		if (servedNodeUrns.get() != null) {

			String[] networkNodes;
			try {
				networkNodes = servedNodeUrns.get();
			} catch (Exception e) {
				throw createRSExceptionException(e.getMessage());
			}

			List<String> unservedNodes = new LinkedList<String>();

			boolean contained;
			for (String nodeUrn : nodeUrns) {

				contained = false;

				for (String networkNode : networkNodes) {
					if (networkNode.equalsIgnoreCase(nodeUrn)) {
						contained = true;
					}
				}

				if (!contained) {
					unservedNodes.add(nodeUrn);
				}
			}


			if (unservedNodes.size() > 0) {
				throw createRSExceptionException("The node URNs " + Arrays
						.toString(unservedNodes.toArray()) + " are unknown to the reservation system!"
				);
			}

		} else {
			log.debug("Not checking session management endpoint for node URN validity as no endpoint is configured.");
		}

	}

	private void checkArgumentValid(PublicReservationData reservation) throws RSExceptionException {
		String msg = null;

		if (reservation == null || reservation.getFrom() == null || reservation.getTo() == null) {
			//if reservation-data is null
			msg = "No reservation data supplied.";
		} else if (reservation.getTo().toGregorianCalendar().getTimeInMillis() < System.currentTimeMillis()) {
			//if "to" of reservation-timestamp lies in the past
			msg = "To time is in the past.";
		} else if (reservation.getTo().toGregorianCalendar().getTimeInMillis() < reservation.getFrom()
				.toGregorianCalendar().getTimeInMillis()) {
			//if "from" is later then "to"
			msg = "To is less than From time.";
		}

		if (reservation == null || reservation.getNodeURNs() == null || reservation.getNodeURNs().size() == 0) {
			msg = "Empty reservation request! At least one node URN must be reserved.";
		}

		if (msg != null) {
			log.warn(msg);
			RSException exception = new RSException();
			exception.setMessage(msg);
			throw new RSExceptionException(msg, exception);
		}

	}

	private void checkArgumentValidReservation(List<SecretReservationKey> secretReservationKeys)
			throws RSExceptionException {

		String msg = null;
		SecretReservationKey srk;

		// Check if reservation data has been supplied
		if (secretReservationKeys == null || secretReservationKeys.size() != 1) {
			msg = "No or too much secretReservationKeys supplied -> error.";

		} else {
			srk = secretReservationKeys.get(0);
			if (!urnPrefix.equals(srk.getUrnPrefix())) {
				msg = "Not serving urn prefix " + srk.getUrnPrefix();
			}
		}

		if (msg != null) {
			log.warn(msg);
			RSException exception = new RSException();
			exception.setMessage(msg);
			throw new RSExceptionException(msg, exception);
		}
	}

	private void checkNodesAvailable(PublicReservationData reservation)
			throws ReservervationConflictExceptionException, RSExceptionException {

		List<String> requested = transform(reservation.getNodeURNs(), StringUtils.STRING_TO_LOWER_CASE);
		Set<String> reserved = new HashSet<String>();

		for (PublicReservationData res : getReservations(reservation.getFrom(), reservation.getTo())) {
			reserved.addAll(transform(res.getNodeURNs(), StringUtils.STRING_TO_LOWER_CASE));
		}

		Set<String> intersection = new HashSet<String>(reserved);
		intersection.retainAll(requested);

		if (intersection.size() > 0) {
			String msg = "Some of the nodes are reserved during the requested time ("
					+ Arrays.toString(intersection.toArray()) + ")";
			log.warn(msg);
			ReservervationConflictException exception = new ReservervationConflictException();
			exception.setMessage(msg);
			throw new ReservervationConflictExceptionException(msg, exception);
		}
	}

	private List<ConfidentialReservationData> throwRSExceptionException(final Exception cause)
			throws RSExceptionException {
		RSException rse = new RSException();
		log.warn(cause.getMessage());
		rse.setMessage(cause.getMessage());
		throw new RSExceptionException(cause.getMessage(), rse);
	}

	private void checkArgumentValid(final GetReservations period) throws RSExceptionException {
		if (period == null || period.getFrom() == null || period.getTo() == null) {
			String message = "Error on checking null for period. Either period, period.from or period.to is null.";
			log.warn(message);
			RSException rse = new RSException();
			rse.setMessage(message);
			throw new RSExceptionException(message, rse);
		}
	}

	private List<PublicReservationData> convertToPublic(
			List<ConfidentialReservationData> confidentialReservationDataList) {
		List<PublicReservationData> publicReservationDataList = Lists.newArrayList();
		for (ConfidentialReservationData confidentialReservationData : confidentialReservationDataList) {
			publicReservationDataList.add(convertToPublic(confidentialReservationData));
		}
		return publicReservationDataList;
	}

}
