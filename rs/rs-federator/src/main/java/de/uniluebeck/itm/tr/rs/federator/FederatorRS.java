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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.wisebed.testbed.api.rs.RSServiceHelper;
import eu.wisebed.testbed.api.rs.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.concurrent.*;


@WebService(endpointInterface = "eu.wisebed.testbed.api.rs.v1.RS", portName = "RSPort", serviceName = "RSService",
        targetNamespace = "urn:RSService")
public class FederatorRS implements RS {

    private static final Logger log = LoggerFactory.getLogger(FederatorRS.class);

    /**
     *
     */
    private ExecutorService executorService = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("FederatorRS-Thread %d").build()
    );

    private BiMap<String, Set<String>> prefixSet;

    public FederatorRS(Map<String, Set<String>> prefixSet) {
        super();
        this.prefixSet = HashBiMap.create(prefixSet);
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

    private static class MakeReservationCallable implements Callable<List<SecretReservationKey>> {

        private String endpointUrl;

        private List<SecretAuthenticationKey> secretAuthenticationKeys;

        private ConfidentialReservationData reservation;

        private MakeReservationCallable(String endpointUrl, List<SecretAuthenticationKey> secretAuthenticationKeys,
                                        ConfidentialReservationData reservation) {
            this.endpointUrl = endpointUrl;
            this.secretAuthenticationKeys = secretAuthenticationKeys;
            this.reservation = reservation;
        }

        @Override
        public List<SecretReservationKey> call() throws Exception {
            RS port = RSServiceHelper.getRSService(endpointUrl);
            List<SecretReservationKey> secretReservationKeyList;
            secretReservationKeyList = port.makeReservation(secretAuthenticationKeys, reservation);
            return secretReservationKeyList;
        }
    }

    private static class DeleteReservationCallable implements Callable<Void> {

        private List<SecretReservationKey> reservationsToBeDeleted;

        private String endpointUrl;

        private DeleteReservationCallable(String endpointUrl, List<SecretReservationKey> reservationsToBeDeleted) {
            this.reservationsToBeDeleted = reservationsToBeDeleted;
            this.endpointUrl = endpointUrl;
        }

        @Override
        public Void call() throws Exception {
            RSServiceHelper.getRSService(endpointUrl).deleteReservation(Collections.<SecretAuthenticationKey>emptyList(), reservationsToBeDeleted);
            return null;
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

        assertUrnsServed(reservation.getNodeURNs());

        // run a set of parallel jobs to make a reservation on the federated rs services
        BiMap<String, ConfidentialReservationData> reservationMap = constructEndpointUrlToReservationMap(reservation);
        BiMap<String, List<SecretAuthenticationKey>> authenticationMap =
                constructEndpointUrlToAuthenticationKeysMap(authenticationData);

        // TODO fix check
        // assertAuthenticationForReservation(reservationMap, authenticationMap);

        Map<Future<List<SecretReservationKey>>, MakeReservationCallable> futures =
                new HashMap<Future<List<SecretReservationKey>>, MakeReservationCallable>();

        // fork the parallel execution of reservations on federated services
        for (Map.Entry<String, ConfidentialReservationData> entry : reservationMap.entrySet()) {
            MakeReservationCallable callable = new MakeReservationCallable(
                    entry.getKey(),
                    authenticationMap.get(entry.getKey()),
                    entry.getValue()
            );
            futures.put(executorService.submit(callable), callable);
        }

        // join the parallel execution and check if one or more of the jobs failed
        boolean failed = false;
        Map<MakeReservationCallable, List<SecretReservationKey>> succeeded =
                new HashMap<MakeReservationCallable, List<SecretReservationKey>>();
        List<String> failMessages = new LinkedList<String>();

        for (Future<List<SecretReservationKey>> future : futures.keySet()) {
            try {
                List<SecretReservationKey> reservationKeys = future.get();
                succeeded.put(futures.get(future), reservationKeys);
            } catch (InterruptedException e) {
                failed = true;
            } catch (ExecutionException e) {
                failed = true;
                failMessages.add(Arrays.toString(futures.get(future).reservation.getNodeURNs().toArray()) + ": " + e
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
        List<SecretReservationKey> res = new LinkedList<SecretReservationKey>();
        for (List<SecretReservationKey> secretReservationKeyList : succeeded.values()) {
            res.addAll(secretReservationKeyList);
        }

        return res;
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

    /**
     * Makes sure that for every reservation request there's a matching secret authentication key
     *
     * @param reservationMap
     * @param authenticationMap
     */
    private void assertAuthenticationForReservation(
            BiMap<String, ConfidentialReservationData> reservationMap,
            BiMap<String, List<SecretAuthenticationKey>> authenticationMap)
            throws AuthorizationExceptionException {

        // TODO really check the matches
        boolean matches = reservationMap.size() == authenticationMap.size();

        if (!matches) {
            String msg = "Not for all reservation there's a matching secret authentication key!";
            AuthorizationException exception = new AuthorizationException();
            exception.setMessage(msg);
            throw new AuthorizationExceptionException(msg, exception);
        }
    }

    private void undoReservations(Map<MakeReservationCallable, List<SecretReservationKey>> succeeded) {

        String endpointUrl;
        List<SecretReservationKey> reservationKeys;
        List<Future> futures = new ArrayList<Future>(succeeded.size());

        // fork processes to delete reservations
        for (Map.Entry<MakeReservationCallable, List<SecretReservationKey>> entry : succeeded.entrySet()) {

            endpointUrl = entry.getKey().endpointUrl;
            reservationKeys = entry.getValue();

            DeleteReservationCallable callable = new DeleteReservationCallable(endpointUrl, reservationKeys);
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

    private BiMap<String, List<SecretAuthenticationKey>> constructEndpointUrlToAuthenticationKeysMap(
            List<SecretAuthenticationKey> authenticationData) {

        BiMap<String, List<SecretAuthenticationKey>> map = HashBiMap.create(prefixSet.size());

        for (SecretAuthenticationKey secretAuthenticationKey : authenticationData) {
            for (Map.Entry<String, Set<String>> entry : prefixSet.entrySet()) {
                for (String servedUrnPrefix : entry.getValue()) {

                    if (secretAuthenticationKey.getUrnPrefix().equals(servedUrnPrefix)) {
                        List<SecretAuthenticationKey> keys = map.get(entry.getKey());
                        if (keys == null) {
                            keys = new LinkedList<SecretAuthenticationKey>();
                            map.put(entry.getKey(), keys);
                        }
                        keys.add(secretAuthenticationKey);

                    }
                }
            }
        }

        return map;
    }

    private BiMap<String, ConfidentialReservationData> constructEndpointUrlToReservationMap(
            ConfidentialReservationData reservation) {

        BiMap<String, ConfidentialReservationData> map = HashBiMap.create(prefixSet.size());

        String endpointUrl;
        for (String nodeURN : reservation.getNodeURNs()) {
            endpointUrl = getEndpointUrlForNodeURN(nodeURN);
            ConfidentialReservationData data = map.get(endpointUrl);
            if (data == null) {
                data = new ConfidentialReservationData();
                map.put(endpointUrl, data);
            }
            data.getNodeURNs().add(nodeURN);
            data.setFrom(reservation.getFrom());
            data.setTo(reservation.getTo());
            data.setUserData(reservation.getUserData());
            data.getData().addAll(reservation.getData());
        }

        return map;

    }

    private String getEndpointUrlForNodeURN(String nodeURN) {

        for (Map.Entry<String, Set<String>> entry : prefixSet.entrySet()) {
            for (String prefix : entry.getValue()) {
                if (nodeURN.startsWith(prefix)) {
                    return entry.getKey();
                }
            }
        }

        return null;

    }

    /**
     * Checks if all nodes in {@code nodeURNs} are served by this federators federated rs services and throws an exception
     * if not
     *
     * @param nodeURNs
     * @throws RSExceptionException
     */
    private void assertUrnsServed(List<String> nodeURNs) throws RSExceptionException {

        List<String> notServed = new LinkedList<String>();

        for (String nodeURN : nodeURNs) {
            String endpointUrlForNodeURN = getEndpointUrlForNodeURN(nodeURN);
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

    private Map<String, List<SecretReservationKey>> constructEndpointUrlToReservationMap(
            List<SecretReservationKey> secretReservationKey) throws RSExceptionException {

        Map<String, List<SecretReservationKey>> map = new HashMap<String, List<SecretReservationKey>>();

        boolean found;
        for (SecretReservationKey reservationKey : secretReservationKey) {

            found = false;

            for (Map.Entry<String, Set<String>> entry : prefixSet.entrySet()) {
                for (String urnPrefix : entry.getValue()) {
                    if (urnPrefix.equals(reservationKey.getUrnPrefix())) {
                        List<SecretReservationKey> secretReservationKeyList = map.get(entry.getKey());
                        if (secretReservationKeyList == null) {
                            secretReservationKeyList = new LinkedList<SecretReservationKey>();
                            map.put(entry.getKey(), secretReservationKeyList);
                        }
                        secretReservationKeyList.add(reservationKey);
                        found = true;
                    }
                }
            }

            if (!found) {
                String msg =
                        "The node URN prefix " + reservationKey.getUrnPrefix() + " is not served by this RS instance!";
                RSException exception = new RSException();
                exception.setMessage(msg);
                throw new RSExceptionException(msg, exception);
            }
        }
        return map;
    }

    private Map<String, List<SecretAuthenticationKey>> constructEndpointUrlToAuthenticationMap(
            List<SecretAuthenticationKey> secretAuthenticationKey) throws RSExceptionException {

        Map<String, List<SecretAuthenticationKey>> map = new HashMap<String, List<SecretAuthenticationKey>>();

        boolean found;
        for (SecretAuthenticationKey authenticationKey : secretAuthenticationKey) {

            found = false;

            for (Map.Entry<String, Set<String>> entry : prefixSet.entrySet()) {
                for (String urnPrefix : entry.getValue()) {
                    if (urnPrefix.equals(authenticationKey.getUrnPrefix())) {
                        List<SecretAuthenticationKey> secretReservationKeyList = map.get(entry.getKey());
                        if (secretReservationKeyList == null) {
                            secretReservationKeyList = new LinkedList<SecretAuthenticationKey>();
                            map.put(entry.getKey(), secretReservationKeyList);
                        }
                        secretReservationKeyList.add(authenticationKey);
                        found = true;
                    }
                }
            }

            if (!found) {
                String msg =
                        "The node URN prefix " + authenticationKey.getUrnPrefix() + " is not served by this RS instance!";
                RSException exception = new RSException();
                exception.setMessage(msg);
                throw new RSExceptionException(msg, exception);
            }
        }
        return map;
    }

    private static class GetReservationsCallable implements Callable<List<PublicReservationData>> {

        private String endpointUrl;

        private XMLGregorianCalendar from;

        private XMLGregorianCalendar to;

        private GetReservationsCallable(String endpointUrl, XMLGregorianCalendar from, XMLGregorianCalendar to) {
            this.endpointUrl = endpointUrl;
            this.from = from;
            this.to = to;
        }

        @Override
        public List<PublicReservationData> call() throws Exception {
            return RSServiceHelper.getRSService(endpointUrl).getReservations(from, to);
        }
    }

    private static class GetConfidentialReservationsCallable implements Callable<List<ConfidentialReservationData>> {

        private String endpointUrl;

        private GetReservations period;
        private List<SecretAuthenticationKey> secretAuthenticationData;

        private GetConfidentialReservationsCallable(String endpointUrl, List<SecretAuthenticationKey> secretAuthenticationData, GetReservations period) {
            this.endpointUrl = endpointUrl;
            this.period = period;
            this.secretAuthenticationData = secretAuthenticationData;
        }

        @Override
        public List<ConfidentialReservationData> call() throws Exception {
            return RSServiceHelper.getRSService(endpointUrl).getConfidentialReservations(secretAuthenticationData, period);
        }
    }


    @Override
    public List<PublicReservationData> getReservations(
            @WebParam(name = "from", targetNamespace = "") XMLGregorianCalendar from,
            @WebParam(name = "to", targetNamespace = "") XMLGregorianCalendar to) throws RSExceptionException {

        assertNotNull(from, "from");
        assertNotNull(to, "to");

        // fork processes to collect reservations from federated services
        List<Future<List<PublicReservationData>>> futures = new LinkedList<Future<List<PublicReservationData>>>();
        for (String endpointUrl : prefixSet.keySet()) {
            GetReservationsCallable callable = new GetReservationsCallable(endpointUrl, from, to);
            futures.add(executorService.submit(callable));
        }

        // join processes and collect results
        List<PublicReservationData> res = new LinkedList<PublicReservationData>();
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
            List<SecretAuthenticationKey> secretAuthenticationKey,
            @WebParam(name = "period", targetNamespace = "")
            GetReservations period) throws RSExceptionException {

        //check for null
        if (period.getFrom() == null || period.getTo() == null)
            throw createRSExceptionException("could not validate period from: " + period.getFrom() + " to: " + period.getTo());

        List<Future<List<ConfidentialReservationData>>> futures = new LinkedList<Future<List<ConfidentialReservationData>>>();
        for (String endpointUrl : prefixSet.keySet()) {
            GetConfidentialReservationsCallable callable = new GetConfidentialReservationsCallable(endpointUrl, constructEndpointUrlToAuthenticationMap(secretAuthenticationKey).get(endpointUrl), period);
            futures.add(executorService.submit(callable));
        }

        List<ConfidentialReservationData> confidentialReservationData = new LinkedList<ConfidentialReservationData>();
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

    private RSExceptionException createRSExceptionException(String s) {
        RSException exception = new RSException();
        exception.setMessage(s);
        return new RSExceptionException(s, exception);
    }


    private static class GetReservationCallable implements Callable<List<ConfidentialReservationData>> {

        private String endpointUrl;

        private List<SecretReservationKey> secretReservationKeys;

        private GetReservationCallable(String endpointUrl, List<SecretReservationKey> secretReservationKeys) {
            this.secretReservationKeys = secretReservationKeys;
            this.endpointUrl = endpointUrl;
        }

        @Override
        public List<ConfidentialReservationData> call() throws Exception {
            return RSServiceHelper.getRSService(endpointUrl).getReservation(secretReservationKeys);
        }
    }

    @Override
    public List<ConfidentialReservationData> getReservation(
            @WebParam(name = "secretReservationKey", targetNamespace = "")
            List<SecretReservationKey> secretReservationKey)
            throws RSExceptionException, ReservervationNotFoundExceptionException {

        assertNotNull(secretReservationKey, "secretReservationKey");

        Map<String, List<SecretReservationKey>> map = constructEndpointUrlToReservationMap(secretReservationKey);

        // fork some processes to fetch the individual reservation data
        List<Future<List<ConfidentialReservationData>>> futures =
                new LinkedList<Future<List<ConfidentialReservationData>>>();
        for (Map.Entry<String, List<SecretReservationKey>> entry : map.entrySet()) {
            GetReservationCallable callable = new GetReservationCallable(entry.getKey(), entry.getValue());
            futures.add(executorService.submit(callable));
        }

        // join processes and unify their results
        List<ConfidentialReservationData> res = new LinkedList<ConfidentialReservationData>();
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

    @Override
    public void deleteReservation(
            @WebParam(name = "authenticationData", targetNamespace = "")
            List<SecretAuthenticationKey> authenticationData,
            @WebParam(name = "secretReservationKey", targetNamespace = "")
            List<SecretReservationKey> secretReservationKey)
            throws RSExceptionException, ReservervationNotFoundExceptionException {

        assertNotNull(authenticationData, "authenticationData");
        assertNotNull(secretReservationKey, "secretReservationKey");

        Map<String, List<SecretReservationKey>> map = constructEndpointUrlToReservationMap(secretReservationKey);

        // calling getReservation assures that every reservation exists
        getReservation(secretReservationKey);

        // fork some processes to delete in parallel
        List<Future<Void>> futures = new LinkedList<Future<Void>>();
        DeleteReservationCallable deleteReservationCallable;
        for (Map.Entry<String, List<SecretReservationKey>> entry : map.entrySet()) {
            deleteReservationCallable = new DeleteReservationCallable(entry.getKey(), entry.getValue());
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

    private void throwRSException(String msg, Throwable e) throws RSExceptionException {
        log.error(msg, e);
        RSException exception = new RSException();
        exception.setMessage(msg);
        throw new RSExceptionException(msg, exception, e);
    }

}
