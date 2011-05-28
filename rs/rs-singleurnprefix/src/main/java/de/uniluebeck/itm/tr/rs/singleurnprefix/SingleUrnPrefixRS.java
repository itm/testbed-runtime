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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.rs.AuthorizationException;
import eu.wisebed.api.rs.AuthorizationExceptionException;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.Data;
import eu.wisebed.api.rs.GetReservations;
import eu.wisebed.api.rs.PublicReservationData;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.RSException;
import eu.wisebed.api.rs.RSExceptionException;
import eu.wisebed.api.rs.ReservervationConflictException;
import eu.wisebed.api.rs.ReservervationConflictExceptionException;
import eu.wisebed.api.rs.ReservervationNotFoundException;
import eu.wisebed.api.rs.ReservervationNotFoundExceptionException;
import eu.wisebed.api.rs.SecretAuthenticationKey;
import eu.wisebed.api.rs.SecretReservationKey;
import eu.wisebed.testbed.api.snaa.helpers.SNAAServiceHelper;
import eu.wisebed.api.snaa.Action;
import eu.wisebed.api.snaa.AuthenticationExceptionException;
import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.api.snaa.SNAAExceptionException;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;

@WebService(endpointInterface = "eu.wisebed.api.rs.RS", portName = "RSPort", serviceName = "RSService",
        targetNamespace = "urn:RSService")
public class SingleUrnPrefixRS implements RS {

    private static final Logger log = LoggerFactory.getLogger(SingleUrnPrefixRS.class);

    private String urnPrefix;

    private String snaaEndpointUrl;

    private String sessionManagementEndpointUrl;

    private RSPersistence persistence;

    private SessionManagement sessionManagementEndpoint;

    public SingleUrnPrefixRS(String urnPrefix, String snaaEndpointUrl, String sessionManagementEndpointUrl,
                             RSPersistence persistence) {
        this.urnPrefix = urnPrefix;
        this.snaaEndpointUrl = snaaEndpointUrl;
        this.sessionManagementEndpointUrl = sessionManagementEndpointUrl;
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
            secretAuthenticationKey = performAuthenticationSanityCheck(authenticationData);
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
            crd.setUserData(reservation.getUserData());

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

        Preconditions.checkNotNull(secretReservationKeys, "Parameter secretReservationKeys is null!");

        SecretReservationKey secretReservationKey = performReservationSanityCheck(secretReservationKeys);
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

        Preconditions.checkNotNull(authenticationData, "Parameter authenticationData is null!");
        Preconditions.checkNotNull(secretReservationKeys, "Parameter secretReservationKeys is null!");

        // getReservationBeforeDeletion does sanity check
        getReservation(secretReservationKeys);
        ConfidentialReservationData reservation = persistence.deleteReservation(secretReservationKeys.get(0));
        log.debug("Deleted reservation {}", reservation);

    }

    @Override
    public List<PublicReservationData> getReservations(
            @WebParam(name = "from", targetNamespace = "") XMLGregorianCalendar from,
            @WebParam(name = "to", targetNamespace = "") XMLGregorianCalendar to) throws RSExceptionException {

        Preconditions.checkNotNull(from, "Parameter from date is null or empty");
        Preconditions.checkNotNull(to, "Parameter to date is null or empty");

        Interval request = new Interval(new DateTime(from.toGregorianCalendar()), new DateTime(to.toGregorianCalendar()));
        List<PublicReservationData> res = convertToPublic(persistence.getReservations(request));

        log.debug("Found " + res.size() + " reservations from " + from + " until " + to);
        return res;
    }

    private List<PublicReservationData> convertToPublic(List<ConfidentialReservationData> confidentialReservationDataList) {
        List<PublicReservationData> publicReservationDataList = Lists.newArrayList();
        for (ConfidentialReservationData confidentialReservationData : confidentialReservationDataList) {
            publicReservationDataList.add(convertToPublic(confidentialReservationData));
        }
        return publicReservationDataList;
    }

    private PublicReservationData convertToPublic(ConfidentialReservationData confidentialReservationData) {
        PublicReservationData publicReservationData = new PublicReservationData();
        publicReservationData.setFrom(confidentialReservationData.getFrom());
        publicReservationData.setTo(confidentialReservationData.getTo());
        publicReservationData.setUserData(confidentialReservationData.getUserData());
        publicReservationData.getNodeURNs().addAll(confidentialReservationData.getNodeURNs());
        return publicReservationData;
    }

    @Override
    public List<ConfidentialReservationData> getConfidentialReservations(
            @WebParam(name = "secretAuthenticationKey", targetNamespace = "")
            List<SecretAuthenticationKey> secretAuthenticationKey,
            @WebParam(name = "period", targetNamespace = "") GetReservations period) throws RSExceptionException {

        Preconditions.checkNotNull(period, "Parameter period is null!");
        Preconditions.checkNotNull(secretAuthenticationKey, "Parameter secretAuthenticationKey is null!");

        //checking on null for period
        //checking on null for secretAuthenticationKey in performSanityCheck
        {
            if (period == null || period.getFrom() == null || period.getTo() == null) {
                String message = "Error on checking null for period. Either period, period.from or period.to is null.";
                log.warn(message);
                RSException rse = new RSException();
                rse.setMessage(message);
                throw new RSExceptionException(message, rse);
            }
        }
        //SanityCheck
        SecretAuthenticationKey key;
        {
            key = performAuthenticationSanityCheck(secretAuthenticationKey);
        }

        Action get = new Action();
        get.setAction("get");
        //AuthenticationCheck
        {
            try {
                checkAuthentication(key, get);
            } catch (Exception e) {
                RSException rse = new RSException();
                log.warn(e.getMessage());
                rse.setMessage(e.getMessage());
                throw new RSExceptionException(e.getMessage(), rse);
            }
        }

        Interval i = new Interval(new DateTime(period.getFrom().toGregorianCalendar()),
                new DateTime(period.getTo().toGregorianCalendar())
        );

        return persistence.getReservations(i);
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
        if (sessionManagementEndpointUrl != null) {

            List<String> unservedNodes = new LinkedList<String>();
            try {

                // lazy load endpoint proxy
                if (sessionManagementEndpoint == null) {
                    sessionManagementEndpoint = WSNServiceHelper.getSessionManagementService(
                            sessionManagementEndpointUrl
                    );
                }

                List<String> networkNodes = WiseMLHelper.getNodeUrns(sessionManagementEndpoint.getNetwork());

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

            } catch (Exception e) {
                log.warn(
                        "Could not contact session management endpoint {}! Skipping validity check of nodes to be reserved.",
                        sessionManagementEndpointUrl
                );
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

    private SecretReservationKey performReservationSanityCheck(List<SecretReservationKey> secretReservationKeys)
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

        if (reservation.getNodeURNs() == null || reservation.getNodeURNs().size() == 0) {
            msg = "Empty reservation request! At least one node URN must be reserved.";
        }

        if (msg != null) {
            log.warn(msg);
            RSException exception = new RSException();
            exception.setMessage(msg);
            throw new RSExceptionException(msg, exception);
        }

    }

    public SecretAuthenticationKey performAuthenticationSanityCheck(List<SecretAuthenticationKey> authenticationData)
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

        eu.wisebed.api.snaa.SecretAuthenticationKey k =
                new eu.wisebed.api.snaa.SecretAuthenticationKey();
        k.setSecretAuthenticationKey(key.getSecretAuthenticationKey());
        k.setUrnPrefix(key.getUrnPrefix());
        k.setUsername(key.getUsername());

        List<eu.wisebed.api.snaa.SecretAuthenticationKey> l =
                new LinkedList<eu.wisebed.api.snaa.SecretAuthenticationKey>();
        l.add(k);

        // Invoke isAuthorized
        try {
            SNAA service = SNAAServiceHelper.getSNAAService(snaaEndpointUrl);
            authorized = service.isAuthorized(l, action);
            log.debug("Authorization result: " + authorized);
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
