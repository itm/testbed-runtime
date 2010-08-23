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

import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import eu.wisebed.testbed.api.rs.v1.*;
import eu.wisebed.testbed.api.snaa.helpers.SNAAServiceHelper;
import eu.wisebed.testbed.api.snaa.v1.Action;
import eu.wisebed.testbed.api.snaa.v1.AuthenticationExceptionException;
import eu.wisebed.testbed.api.snaa.v1.SNAA;
import eu.wisebed.testbed.api.snaa.v1.SNAAExceptionException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

@WebService(endpointInterface = "eu.wisebed.testbed.api.rs.v1.RS", portName = "RSPort", serviceName = "RSService",
		targetNamespace = "urn:RSService")
public class SingleUrnPrefixRS implements RS {

	private static final Logger log = LoggerFactory.getLogger(SingleUrnPrefixRS.class);

	private String urnPrefix;

	private String snaaEndpointUrl;

	private RSPersistence persistence;

	public SingleUrnPrefixRS(String urnPrefix, String snaaEndpointUrl, RSPersistence persistence) {
		this.urnPrefix = urnPrefix;
		this.snaaEndpointUrl = snaaEndpointUrl;
		this.persistence = persistence;
		log.debug("New instance serving prefix: " + urnPrefix);
		log.debug("SNAA endpoint: " + snaaEndpointUrl);
	}

	@WebResult(name = "secretReservationKey")
	@Override
	public List<SecretReservationKey> makeReservation(
			@WebParam(name = "authenticationData", targetNamespace = "")
			List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "reservation") ConfidentialReservationData reservation)
			throws AuthorizationExceptionException, ReservervationConflictExceptionException, RSExceptionException {

		SecretAuthenticationKey secretAuthenticationKey;

		// Sanity check
		{
			performSanityCheck(reservation);
			secretAuthenticationKey = performSanityCheck(authenticationData);
			performServingNodeUrnsCheck(reservation.getNodeURNs());
		}

		// Authentication check
		{
			if (snaaEndpointUrl != null && !"".equals(snaaEndpointUrl)) {
				Action action = new Action();
				action.setAction("reserve"); // TODO Change to something sensible when defined
				try {
					checkAuthentication(secretAuthenticationKey, action);
				} catch (AuthenticationExceptionException e) {
					throw createAuthorizationExceptionException(e);
				}
			}
		}

		// Check if the reservation is possible (i.e., all nodes are available during the given time interval)
		{
			checkNodesAvailable(reservation);
		}

		// Create new reservation
		{
			ConfidentialReservationData crd = new ConfidentialReservationData();
			crd.setFrom(reservation.getFrom());
			crd.setTo(reservation.getTo());
			crd.getNodeURNs().addAll(reservation.getNodeURNs());
			Data data = new Data();
			data.setUrnPrefix(secretAuthenticationKey.getUrnPrefix());
			data.setUsername(secretAuthenticationKey.getUsername());
			//data.setSecretReservationKey(reservation.getData().get(0).getSecretReservationKey());
			crd.getData().add(data);

			try {
				SecretReservationKey secretReservationKey = persistence.addReservation(crd, urnPrefix);
				List<SecretReservationKey> keys = new ArrayList<SecretReservationKey>();
				keys.add(secretReservationKey);
                data.setSecretReservationKey(secretReservationKey.getSecretReservationKey());
				return keys;

			} catch (Exception e) {
				throw createRSExceptionException(e.getMessage());
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

	@Override
	public List<ConfidentialReservationData> getReservation(
			@WebParam(name = "secretReservationKey") List<SecretReservationKey> secretReservationKeys)
			throws RSExceptionException, ReservervationNotFoundExceptionException {

		SecretReservationKey secretReservationKey = performSanityCheck(secretReservationKeys);
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

		// getReservationBeforeDeletion does sanity check
		getReservation(secretReservationKeys);
		ConfidentialReservationData reservation = persistence.deleteReservation(secretReservationKeys.get(0));
		log.debug("Deleted reservation {}", reservation);

	}

	@Override
	public List<PublicReservationData> getReservations(
			@WebParam(name = "from", targetNamespace = "") XMLGregorianCalendar from,
			@WebParam(name = "to", targetNamespace = "") XMLGregorianCalendar to) throws RSExceptionException {

		Interval request =
				new Interval(new DateTime(from.toGregorianCalendar()), new DateTime(to.toGregorianCalendar()));
		List<PublicReservationData> res = new LinkedList<PublicReservationData>(persistence.getReservations(request));

		log.debug("Found " + res.size() + " reservations from " + from + " until " + to);
		return res;
	}

	@Override
	public List<ConfidentialReservationData> getConfidentialReservations(
			@WebParam(name = "secretAuthenticationKey", targetNamespace = "")
			List<SecretAuthenticationKey> secretAuthenticationKey,
			@WebParam(name = "period", targetNamespace = "") GetReservations period) throws RSExceptionException {

		// TODO implement
		throw createRSExceptionException("Not yet implemented!");
	}

	private void checkNodesAvailable(PublicReservationData reservation)
			throws ReservervationConflictExceptionException, RSExceptionException {
		List<String> requested = reservation.getNodeURNs();
		Set<String> reserved = new HashSet<String>();

		for (PublicReservationData res : getReservations(reservation.getFrom(), reservation.getTo())) {
			reserved.addAll(res.getNodeURNs());
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

	private void performServingNodeUrnsCheck(List<String> nodeUrns) throws RSExceptionException {

		// Check if we serve all node urns
		for (String nodeUrn : nodeUrns) {
			if (!nodeUrn.startsWith(urnPrefix)) {
				String msg = "Not responsible for node URN " + nodeUrn + ", only serving prefix: " + urnPrefix;
				RSException exception = new RSException();
				exception.setMessage(msg);
				throw new RSExceptionException(msg, exception);
			}
		}
	}

	private SecretReservationKey performSanityCheck(List<SecretReservationKey> secretReservationKeys)
			throws RSExceptionException {
		String msg = null;
		SecretReservationKey srk = null;

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

		return srk;
	}

	private void performSanityCheck(PublicReservationData reservation) throws RSExceptionException {
		String msg = null;

		if (reservation == null || reservation.getFrom() == null || reservation.getTo() == null) {
			msg = "No reservation data supplied.";
		} else if (reservation.getFrom().toGregorianCalendar().getTimeInMillis() < System.currentTimeMillis()) {
			msg = "From time is in the past.";
		} else if (reservation.getTo().toGregorianCalendar().getTimeInMillis() < reservation.getFrom()
				.toGregorianCalendar().getTimeInMillis()) {
			msg = "To is less than From time.";
		}

		if (msg != null) {
			log.warn(msg);
			RSException exception = new RSException();
			exception.setMessage(msg);
			throw new RSExceptionException(msg, exception);
		}

	}

	public SecretAuthenticationKey performSanityCheck(List<SecretAuthenticationKey> authenticationData)
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

		return sak;
	}

	public boolean checkAuthentication(SecretAuthenticationKey key, Action action) throws RSExceptionException,
			AuthorizationExceptionException, AuthenticationExceptionException {

		log.debug("Checking authorization for key: " + key + " and action: " + action);
		boolean authorized;

		eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey k =
				new eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey();
		k.setSecretAuthenticationKey(key.getSecretAuthenticationKey());
		k.setUrnPrefix(key.getUrnPrefix());
		k.setUsername(key.getUsername());

		List<eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey> l =
				new LinkedList<eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey>();
		l.add(k);

		// Invoke isAuthorized
		try {
			SNAA service = SNAAServiceHelper.getSNAAService(snaaEndpointUrl);
			authorized = service.isAuthorized(l, action);
			log.info("Authorization result: " + authorized);
		} catch (SNAAExceptionException e) {
			RSException rse = new RSException();
			log.warn(e.getMessage());
			rse.setMessage(e.getMessage());
			throw new RSExceptionException(e.getMessage(), rse);
		}

		if (!authorized) {
			AuthorizationException e = new AuthorizationException();
			String msg = "Authorization failed";
			e.setMessage(msg);
			log.warn(msg, e);
			throw new AuthorizationExceptionException(msg, e);
		}

		return true;
	}

}
