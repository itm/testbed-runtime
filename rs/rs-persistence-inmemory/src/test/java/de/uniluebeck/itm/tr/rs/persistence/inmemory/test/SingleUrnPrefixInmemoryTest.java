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

package de.uniluebeck.itm.tr.rs.persistence.inmemory.test;

import de.uniluebeck.itm.tr.rs.persistence.EndpointPropertiesTestMap;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.inmemory.InMemoryRSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.test.Comparison;
import de.uniluebeck.itm.tr.rs.singleurnprefix.SingleUrnPrefixRS;
import de.uniluebeck.itm.tr.snaa.cmdline.server.Server;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.testbed.api.rs.v1.*;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.*;

import static org.junit.Assert.*;

public class SingleUrnPrefixInmemoryTest {

    private static String snaaURL = "http://localhost:8080/snaa/dummy1";
    private static String urnPrefix = "urn:wisebed:dummy1";
    private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();
    private RSPersistence rsPersistence = new InMemoryRSPersistence();
    private SingleUrnPrefixRS singleUrnPrefixRS = new SingleUrnPrefixRS(urnPrefix, snaaURL, rsPersistence);
    private Map<Integer, ConfidentialReservationData> reservationDataMap = new HashMap<Integer, ConfidentialReservationData>();
    private Map<Integer, SecretReservationKey> reservationKeyMap = new HashMap<Integer, SecretReservationKey>();
    private static long from = System.currentTimeMillis() + 200000;
    private static long to = System.currentTimeMillis() + 200000 + 1000;
    private List<SecretAuthenticationKey> secretAuthenticationKeyList = null;
    private GregorianCalendar gregorianCalendarFrom = new GregorianCalendar();
    private GregorianCalendar gregorianCalendarTo = new GregorianCalendar();
    private Map<String, String> endpointPropertiesMap = EndpointPropertiesTestMap.SNAAPropertiesMapWisebed1;
    
    @Before
    public void setUp() throws Exception {
        //PropertiesMap
        Properties props = new Properties();
        for (Object key : endpointPropertiesMap.keySet()){
            props.setProperty((String) key, endpointPropertiesMap.get(key));
        }

        //starting endpoint
        Server.startFromProperties(props);

        //creating SecretAuthenticationKey
        secretAuthenticationKeyList = new LinkedList<SecretAuthenticationKey>();
        SecretAuthenticationKey key = new SecretAuthenticationKey();
        key.setUsername("Nils Rohwedder");
        key.setSecretAuthenticationKey(secureIdGenerator.getNextId());
        key.setUrnPrefix(urnPrefix);
        secretAuthenticationKeyList.add(key);

        //creating ConfidentialReservationData
        ConfidentialReservationData data = new ConfidentialReservationData();
		User user = new User();
		user.setUrnPrefix(urnPrefix);
		user.setUsername("Nils Rohwedder");
        data.getUsers().add(user);

        gregorianCalendarFrom.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        gregorianCalendarFrom.setTimeInMillis(from);
        
        data.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarFrom));

        gregorianCalendarTo.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        gregorianCalendarTo.setTimeInMillis(to);
        data.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarTo));

        //Creating testDataMap
        for (int i = 0; i < 10; i++) {
            reservationDataMap.put(i, data);
        }

    }

    @Test
    public void test() throws Throwable {
        makeReservation();
        getReservations();
        getReservationBeforeDeletion();
        deleteReservationBeforeDeletion();
        getReservationAfterDeletion();
        deleteReservationAfterDeletion();
    }

    public void makeReservation() throws Throwable {
        for (int i = 0; i < reservationDataMap.size(); i++) {
            reservationKeyMap.put(i, singleUrnPrefixRS.makeReservation(secretAuthenticationKeyList, reservationDataMap.get(i)).get(0));
        }
    }

    public void getReservationBeforeDeletion() throws RSExceptionException, ReservervationNotFoundExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++){
            List<SecretReservationKey> tempKeyList = new LinkedList<SecretReservationKey>();
            tempKeyList.add(reservationKeyMap.get(i));
            assertTrue(equals(reservationDataMap.get(i), singleUrnPrefixRS.getReservation(tempKeyList).get(0)));
        }
    }

	private boolean equals(ConfidentialReservationData reservationData, ConfidentialReservationData reservationDataOther) {
		// TODO implement (im moment noch unschoen!!)
        //if (!reservationData.getUsers().get(0).getUsername().equals(reservationDataOther.getUsers().get(0).getUsername())) return false;
        //if (reservationData.getFrom().toGregorianCalendar().getTimeInMillis() != reservationDataOther.getFrom().toGregorianCalendar().getTimeInMillis()) return false;
        //if (reservationData.getTo().toGregorianCalendar().getTimeInMillis() != reservationDataOther.getTo().toGregorianCalendar().getTimeInMillis()) return false;
        return Comparison.equals(reservationData, reservationDataOther);
    }

    

	public void getReservationAfterDeletion() throws RSExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++){
            List<SecretReservationKey> tempKeyList = new LinkedList<SecretReservationKey>();
            tempKeyList.add(reservationKeyMap.get(i));
            try {
                singleUrnPrefixRS.getReservation(tempKeyList).get(0);
                fail("Should have raised an ReservervationNotFoundExceptionException");
            }
            catch (ReservervationNotFoundExceptionException e){}
        }
    }


    public void getReservations() throws RSExceptionException, DatatypeConfigurationException {
        //first interval : no overlap first direction
        GregorianCalendar testFrom = new GregorianCalendar();
        GregorianCalendar testTo = new GregorianCalendar();

        testFrom.setTimeInMillis(from - 20000);
        testTo.setTimeInMillis(to - 20000);
        assertSame(singleUrnPrefixRS.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 0);

        //second interval : small overlap first direction
        testFrom.setTimeInMillis(from - 20000);
        testTo.setTimeInMillis(to - 500);
        assertSame(singleUrnPrefixRS.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 10);

//        //second interval : small overlap second direction
        testFrom.setTimeInMillis(from + 500);
        testTo.setTimeInMillis(to + 20000);
        assertSame(singleUrnPrefixRS.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 10);
        
        //third interval : overlap on the same timeline first direction
        testFrom.setTimeInMillis(from - 20000);
        testTo.setTimeInMillis(from);
        assertSame(singleUrnPrefixRS.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 0);

        //third interval : overlap on the same timeline second direction
        testFrom.setTimeInMillis(to);
        testTo.setTimeInMillis(to + 20000);
        assertSame(singleUrnPrefixRS.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 0);

        //fourth interval : absolute overlap first direction
        testFrom.setTimeInMillis(from + 5);
        testTo.setTimeInMillis(to - 5);
        assertSame(singleUrnPrefixRS.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 10);

        //fourth interval : absolute overlap second direction
        testFrom.setTimeInMillis(from - 5);
        testTo.setTimeInMillis(to + 5);
        assertSame(singleUrnPrefixRS.getReservations(DatatypeFactory.newInstance().newXMLGregorianCalendar(testFrom), DatatypeFactory.newInstance().newXMLGregorianCalendar(testTo)).size(), 10);
    }

    public void deleteReservationBeforeDeletion() throws RSExceptionException, ReservervationNotFoundExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++){
            List<SecretReservationKey> tempKeyList = new LinkedList<SecretReservationKey>();
            tempKeyList.add(reservationKeyMap.get(i));
            singleUrnPrefixRS.deleteReservation(Collections.<SecretAuthenticationKey>emptyList(), tempKeyList);
        }
    }

    public void deleteReservationAfterDeletion() throws RSExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++){
            List<SecretReservationKey> tempKeyList = new LinkedList<SecretReservationKey>();
            tempKeyList.add(reservationKeyMap.get(i));
            try {
                singleUrnPrefixRS.deleteReservation(Collections.<SecretAuthenticationKey>emptyList(), tempKeyList);
                fail("Should have raised an ReservervationNotFoundExceptionException");
            }
            catch (ReservervationNotFoundExceptionException e){;}
        }
    }

}