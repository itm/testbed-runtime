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

package de.uniluebeck.itm.tr.rs.cmdline;

import de.uniluebeck.itm.tr.rs.federator.FederatorRS;
import de.uniluebeck.itm.tr.rs.persistence.EndpointPropertiesTestMap;
import de.uniluebeck.itm.tr.rs.persistence.Comparison;
import de.uniluebeck.itm.tr.snaa.cmdline.server.Server;
import de.uniluebeck.itm.tr.snaa.federator.FederatorSNAA;
import eu.wisebed.testbed.api.rs.v1.*;
import eu.wisebed.testbed.api.snaa.v1.Action;
import eu.wisebed.testbed.api.snaa.v1.AuthenticationTriple;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.*;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class FederatorInmemoryTest {
    private Map<String, Set<String>> rsPrefixSet = null;
    private Map<String, Set<String>> snaaPrefixSet = null;
    private RS rsFederator = null;
    private FederatorSNAA snaaFederator = null;
    private Map<String, String> snaaEndpointPropertiesMapWisebed1 = EndpointPropertiesTestMap.SNAAPropertiesMapWisebed1;
    private Map<String, String> snaaEndpointPropertiesMapWisebed2 = EndpointPropertiesTestMap.SNAAPropertiesMapWisebed2;
    private Map<String, String> rsEndpointPropertiesMap = EndpointPropertiesTestMap.RSPropertiesMap;
    private List<eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey> rsSecretAuthenticationKeyList = new LinkedList<eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey>();
    private List<eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey> snaaSecretAuthenticationKeyList = new LinkedList<eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey>();
    private GregorianCalendar gregorianCalendarFrom = null;
    private GregorianCalendar gregorianCalendarTo = null;
    private Map<Integer, ConfidentialReservationData> reservationDataMap = new HashMap<Integer, ConfidentialReservationData>();
    private Map<Integer, List<SecretReservationKey>> reservationKeyMap = null;

    @Before
    public void setUp() throws Exception {
        //PropertiesMap SNAA1
        Properties SNAAProps1 = new Properties();
        for (String key : snaaEndpointPropertiesMapWisebed1.keySet()){
            SNAAProps1.setProperty(key, snaaEndpointPropertiesMapWisebed1.get(key));
        }
        //PropertiesMap SNAA2
        Properties SNAAProps2 = new Properties();
        for (String key : snaaEndpointPropertiesMapWisebed2.keySet()){
            SNAAProps2.setProperty(key, snaaEndpointPropertiesMapWisebed2.get(key));
        }

        //Properties RS
        Properties RSProps = new Properties();
        for (String key : rsEndpointPropertiesMap.keySet()){
            RSProps.setProperty(key, rsEndpointPropertiesMap.get(key));
        }

        //starting SNAA-Server
        Server.startFromProperties(SNAAProps1);
        Server.startFromProperties(SNAAProps2);
        //starting RS-Server
        Main.startFromProperties(RSProps);

        List<String> urnPrefixe = new LinkedList<String>();
        urnPrefixe.add("urn:wisebed1:testbed1");
        urnPrefixe.add("urn:wisebed1:testbed2");
        urnPrefixe.add("urn:wisebed2:testbed1");
        urnPrefixe.add("urn:wisebed2:testbed2");

        Set<String> testbed1 = new HashSet<String>();
        testbed1.add(urnPrefixe.get(0));
        Set<String> testbed2 = new HashSet<String>();
        testbed2.add(urnPrefixe.get(1));
        Set<String> testbed3 = new HashSet<String>();
        testbed3.add(urnPrefixe.get(2));
        Set<String> testbed4 = new HashSet<String>();
        testbed4.add(urnPrefixe.get(3));

        snaaPrefixSet = new HashMap<String, Set<String>>();
        snaaPrefixSet.put("http://localhost:8080/snaa/dummy1", testbed1);
        snaaPrefixSet.put("http://localhost:8080/snaa/dummy2", testbed2);
        snaaPrefixSet.put("http://localhost:8090/snaa/dummy1", testbed3);
        snaaPrefixSet.put("http://localhost:8090/snaa/dummy2", testbed4);
        snaaFederator = new FederatorSNAA(snaaPrefixSet);

        rsPrefixSet = new HashMap<String, Set<String>>();
        rsPrefixSet.put("http://localhost:9090/rs/inmemory1", testbed1);
        rsPrefixSet.put("http://localhost:9090/rs/inmemory2", testbed2);
        rsPrefixSet.put("http://localhost:9090/rs/inmemory3", testbed3);
        rsPrefixSet.put("http://localhost:9090/rs/inmemory4", testbed4);
        rsFederator = new FederatorRS(rsPrefixSet);
        

        //creating SNAA-Authentication-Data
        LinkedList<AuthenticationTriple> snaaAuthenticationData = null;
        for (String prefix : urnPrefixe){
            AuthenticationTriple snaaAuthenticationTriple = new AuthenticationTriple();
            snaaAuthenticationTriple.setUsername("Nils Rohwedder");
            snaaAuthenticationTriple.setPassword("");
            snaaAuthenticationTriple.setUrnPrefix(prefix);
            snaaAuthenticationData = new LinkedList<AuthenticationTriple>();
            snaaAuthenticationData.add(snaaAuthenticationTriple);
            snaaSecretAuthenticationKeyList.addAll(snaaFederator.authenticate(snaaAuthenticationData));
            Action action = new Action();
            action.setAction("reserve");
            assertTrue(snaaFederator.isAuthorized(snaaSecretAuthenticationKeyList, action));
        }

        //creating SecretAuthenticationKey
        for (eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey snaaKey : snaaSecretAuthenticationKeyList){
            eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey rsKey = new eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey();
            rsKey.setUsername(snaaKey.getUsername());
            rsKey.setSecretAuthenticationKey(snaaKey.getSecretAuthenticationKey());
            rsKey.setUrnPrefix(snaaKey.getUrnPrefix());
            rsSecretAuthenticationKeyList.add(rsKey);
        }


        List<User> users = new LinkedList<User>();
        User user = new User();
        user.setUrnPrefix("urn:wisebed1:testbed1");
        user.setUrnPrefix("urn:wisebed1:testbed2");
        user.setUrnPrefix("urn:wisebed2:testbed1");
        user.setUrnPrefix("urn:wisebed2:testbed2");
        user.setUsername("Nils Rohwedder");
        users.add(user);

        List<String> urns = new LinkedList<String>();
        urns.add("urn:wisebed1:testbed1");
        urns.add("urn:wisebed1:testbed2");
        urns.add("urn:wisebed2:testbed1");
        urns.add("urn:wisebed2:testbed2");
        //Creating confi-data
        for (int i = 0; i < 12; i++){
            Thread.sleep(100);
            ConfidentialReservationData data = new ConfidentialReservationData();
            data.getUsers().addAll(users);
            data.getNodeURNs().addAll(urns);

            long millis = System.currentTimeMillis() + 200000;
            gregorianCalendarFrom = new GregorianCalendar();
            gregorianCalendarFrom.setTimeZone(TimeZone.getTimeZone("GMT+2"));
            gregorianCalendarFrom.setTimeInMillis(millis);
            data.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarFrom));

            gregorianCalendarTo = new GregorianCalendar();
            gregorianCalendarTo.setTimeZone(TimeZone.getTimeZone("GMT+2"));
            gregorianCalendarTo.setTimeInMillis(millis + 50);
            data.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarTo));

            reservationDataMap.put(i, data);
        }
    }

    private boolean equals(ConfidentialReservationData reservationData, ConfidentialReservationData reservationDataOther) {
        return Comparison.equals(reservationData, reservationDataOther);
    }

    @Test
    public void test() throws ReservervationConflictExceptionException, RSExceptionException, AuthorizationExceptionException, ReservervationNotFoundExceptionException, DatatypeConfigurationException {
        makeReservation();
        getReservationBeforeDeletion();
        getReservations();
        deleteReservationBeforeDeletion();

        //Error-testing
        getReservationAfterDeletion();
        deleteReservationAfterDeletion();
        //Exception-Testing
        makeReservationErrorTesting();
        getReservationErrorTesting();
        getReservationsErrorTesting();
    }

    private void getReservationsErrorTesting() {
        //testing on null
        try {
            rsFederator.getReservations(null, null);
            fail("Should have raised RSException");
        }
        catch (RSExceptionException e){}
    }

    private void getReservationErrorTesting() throws ReservervationNotFoundExceptionException, RSExceptionException {
        //testing on null
        try {
            rsFederator.getReservation(null);
            fail("Should have raised RSException");
        }
        catch (RSExceptionException e){}
        //testing on empty SecretReservationKey-List
        try {
            rsFederator.getReservation(Arrays.asList(new SecretReservationKey()));
            fail("Should have raised RSException");
        }
        catch (RSExceptionException e){}
        //testing on wrong SecretReservationKey
        try {
            SecretReservationKey key = new SecretReservationKey();
            key.setSecretReservationKey("a");
            key.setUrnPrefix("urn:wisebed1:testbed1");
            rsFederator.getReservation(Arrays.asList(key));
            fail("Should have raised ReservationNotFoundException");
        }
        catch (ReservervationNotFoundExceptionException e){System.out.println(e);}
    }

    private void makeReservationErrorTesting() throws RSExceptionException, ReservervationConflictExceptionException, AuthorizationExceptionException {
        //testing on null
        try {
            rsFederator.makeReservation(null, null);
            fail("Should have raised an RSExceptionException");
        }
        catch(RSExceptionException e){}
        try {
            List<SecretAuthenticationKey> data = new LinkedList<SecretAuthenticationKey>();
            rsFederator.makeReservation(data, null);
            fail("Should have raised an RSExceptionException");
        }
        catch(RSExceptionException e){}
        //testing on not-served urn
        try {
            List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();
            ConfidentialReservationData resData = new ConfidentialReservationData();
            resData.getNodeURNs().add("urn:not:served");
            rsFederator.makeReservation(authData, resData);
            fail("Should have raised an RSExceptionException");
        }
        catch (RSExceptionException e){}
        //testing on not valid authentication-data
        try {
            List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();
            ConfidentialReservationData resData = new ConfidentialReservationData();
            resData.getNodeURNs().add("urn:wisebed1:testbed1");
            rsFederator.makeReservation(authData, resData);
            fail("Should have raised an AuthorizationExceptionException");
        }
        catch (AuthorizationExceptionException e){}
        //testing if makeReservation on empty authenticationData and reservation-data returns empty SecretReservationKey-list
        List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();
        ConfidentialReservationData resData = new ConfidentialReservationData();
        resData.getNodeURNs();
        List<SecretReservationKey> list = rsFederator.makeReservation(authData, resData);
        assertSame(list.size(), 0);
    }

    public void makeReservation() throws RSExceptionException, ReservervationConflictExceptionException, AuthorizationExceptionException {
        reservationKeyMap = new HashMap<Integer, List<SecretReservationKey>>();
        for (int i = 0; i < reservationDataMap.size(); i++) {
            reservationKeyMap.put(i, rsFederator.makeReservation(rsSecretAuthenticationKeyList, reservationDataMap.get(i)));
        }
    }

    public void getReservationBeforeDeletion() throws RSExceptionException, ReservervationNotFoundExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++){
            List<SecretReservationKey> tempKeyList = reservationKeyMap.get(i);
            List<ConfidentialReservationData> reservationData = rsFederator.getReservation(tempKeyList);
            for (ConfidentialReservationData data : reservationData){
                for (User user : data.getUsers()){
                    ConfidentialReservationData testData = reservationDataMap.get(i);

                    testData.getNodeURNs().clear();
                    testData.getNodeURNs().addAll(data.getNodeURNs());

                    testData.getUsers().clear();
                    testData.getUsers().addAll(data.getUsers());

                    assertTrue(equals(testData, data));
                }
            }
        }
    }

    public void getReservationAfterDeletion() throws RSExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++){
            List<SecretReservationKey> tempKeyList = reservationKeyMap.get(i);
            try {
                rsFederator.getReservation(tempKeyList);
                fail("Should have raised an ReservervationNotFoundExceptionException");
            }
            catch (ReservervationNotFoundExceptionException e){}
        }
    }

    public void deleteReservationBeforeDeletion() throws RSExceptionException, ReservervationNotFoundExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++){
            List<SecretReservationKey> tempKeyList = reservationKeyMap.get(i);
            rsFederator.deleteReservation(Collections.<SecretAuthenticationKey>emptyList(), tempKeyList);
        }
    }

    public void deleteReservationAfterDeletion() throws RSExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++){
            List<SecretReservationKey> tempKeyList = reservationKeyMap.get(i);
            try {
                rsFederator.deleteReservation(Collections.<SecretAuthenticationKey>emptyList(), tempKeyList);
                fail("Should have raised an ReservervationNotFoundExceptionException");
            }
            catch (ReservervationNotFoundExceptionException e){;}
        }
    }

    public void getReservations() throws RSExceptionException, DatatypeConfigurationException {
        for (Integer i : reservationDataMap.keySet()){

            GregorianCalendar testFrom = new GregorianCalendar();
            GregorianCalendar testTo = new GregorianCalendar();

            long from = reservationDataMap.get(i).getFrom().toGregorianCalendar().getTimeInMillis();
            long to = reservationDataMap.get(i).getTo().toGregorianCalendar().getTimeInMillis();

            //first interval : no overlap first direction
            testFrom.setTimeInMillis(from - 20);
            testTo.setTimeInMillis(to - 60);
            assertSame(rsFederator.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 0);

            //second interval : small overlap first direction
            testFrom.setTimeInMillis(from - 20);
            testTo.setTimeInMillis(to - 30);
            assertSame(rsFederator.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 4);

        //        //second interval : small overlap second direction
            testFrom.setTimeInMillis(from + 30);
            testTo.setTimeInMillis(to + 20);
            assertSame(rsFederator.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 4);

            //third interval : overlap on the same timeline first direction
            testFrom.setTimeInMillis(from - 20);
            testTo.setTimeInMillis(from);
            assertSame(rsFederator.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 0);

            //third interval : overlap on the same timeline second direction
            testFrom.setTimeInMillis(to);
            testTo.setTimeInMillis(to + 20);
            assertSame(rsFederator.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 0);

            //fourth interval : absolute overlap first direction
            testFrom.setTimeInMillis(from + 5);
            testTo.setTimeInMillis(to - 5);
            assertSame(rsFederator.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 4);

            //fourth interval : absolute overlap second direction
            testFrom.setTimeInMillis(from - 5);
            testTo.setTimeInMillis(to + 5);
            assertSame(rsFederator.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 4);
        }
    }
}
