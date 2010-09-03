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

package de.uniluebeck.itm.tr.rs.persistence.inmemory.test;

import de.uniluebeck.itm.tr.rs.persistence.Comparison;
import de.uniluebeck.itm.tr.rs.persistence.EndpointPropertiesTestMap;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.inmemory.InMemoryRSPersistence;
import de.uniluebeck.itm.tr.rs.singleurnprefix.SingleUrnPrefixRS;
import de.uniluebeck.itm.tr.snaa.cmdline.server.Server;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.testbed.api.rs.v1.*;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static org.junit.Assert.*;

public class SingleUrnPrefixInmemoryTest {

    private static String snaaURL = "http://localhost:8080/snaa/dummy1";
    private static String urnPrefix = "urn:wisebed:dummy1";
    private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();
    private RSPersistence rsPersistence = new InMemoryRSPersistence();
    private SingleUrnPrefixRS singleUrnPrefixRS = new SingleUrnPrefixRS(urnPrefix, snaaURL, null, rsPersistence);
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
        for (Object key : endpointPropertiesMap.keySet()) {
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
        ConfidentialReservationData confiData = new ConfidentialReservationData();
        Data data = new Data();
        data.setUrnPrefix(urnPrefix);
        data.setUsername("Nils Rohwedder");
        confiData.getData().add(data);

        gregorianCalendarFrom.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        gregorianCalendarFrom.setTimeInMillis(from);

        confiData.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarFrom));

        gregorianCalendarTo.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        gregorianCalendarTo.setTimeInMillis(to);
        confiData.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarTo));

        //Creating testDataMap
        for (int i = 0; i < 10; i++) {
            reservationDataMap.put(i, confiData);
        }

    }

    @Test
    public void test() throws Throwable {
        makeReservation();
        getReservations();
        getConfindentialReservations();
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
        for (int i = 0; i < reservationDataMap.size(); i++) {

            List<SecretReservationKey> tempKeyList = new LinkedList<SecretReservationKey>();
            tempKeyList.add(reservationKeyMap.get(i));

            ConfidentialReservationData rememberedCRD = reservationDataMap.get(i);
            ConfidentialReservationData receivedCRD = singleUrnPrefixRS.getReservation(tempKeyList).get(0);

            assertEquals(rememberedCRD.getUserData(), receivedCRD.getUserData());
            assertEquals(rememberedCRD.getNodeURNs(), receivedCRD.getNodeURNs());
            assertEquals(rememberedCRD.getFrom(), receivedCRD.getFrom());
            assertEquals(rememberedCRD.getTo(), receivedCRD.getTo());

            //assertTrue(equals(reservationDataMap.get(i), singleUrnPrefixRS.getReservation(tempKeyList).get(0)));
        }
    }

    private boolean equals(ConfidentialReservationData rd1, ConfidentialReservationData rd2) {
        // TODO implement (im moment noch unschoen!!)
        //if (!reservationData.getUsers().get(0).getUsername().equals(reservationDataOther.getUsers().get(0).getUsername())) return false;
        //if (reservationData.getFrom().toGregorianCalendar().getTimeInMillis() != reservationDataOther.getFrom().toGregorianCalendar().getTimeInMillis()) return false;
        //if (reservationData.getTo().toGregorianCalendar().getTimeInMillis() != reservationDataOther.getTo().toGregorianCalendar().getTimeInMillis()) return false;
        return Comparison.equals(rd1, rd2);
    }


    public void getReservationAfterDeletion() throws RSExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++) {
            List<SecretReservationKey> tempKeyList = new LinkedList<SecretReservationKey>();
            tempKeyList.add(reservationKeyMap.get(i));
            try {
                singleUrnPrefixRS.getReservation(tempKeyList).get(0);
                fail("Should have raised an ReservervationNotFoundExceptionException");
            }
            catch (ReservervationNotFoundExceptionException e) {
            }
        }
    }


    public void getReservations() throws RSExceptionException, DatatypeConfigurationException {
        //first interval : no overlap first direction
        XMLGregorianCalendar testFrom = createGregorianCalendar(from - 20000);
        XMLGregorianCalendar testTo = createGregorianCalendar(from - 20000);

        assertSame(singleUrnPrefixRS.getReservations(testFrom, testTo).size(), 0);

        //second interval : small overlap first direction
        testFrom = createGregorianCalendar(from - 20000);
        testTo = createGregorianCalendar(to - 500);
        assertSame(singleUrnPrefixRS.getReservations(testFrom, testTo).size(), 10);

//        //second interval : small overlap second direction
        testFrom = createGregorianCalendar(from + 500);
        testTo = createGregorianCalendar(to + 20000);
        assertSame(singleUrnPrefixRS.getReservations(testFrom, testTo).size(), 10);

        //third interval : overlap on the same timeline first direction
        testFrom = createGregorianCalendar(from - 20000);
        testTo = createGregorianCalendar(from);
        assertSame(singleUrnPrefixRS.getReservations(testFrom, testTo).size(), 0);

        //third interval : overlap on the same timeline second direction
        testFrom = createGregorianCalendar(to);
        testTo = createGregorianCalendar(to + 20000);
        assertSame(singleUrnPrefixRS.getReservations(testFrom, testTo).size(), 0);

        //fourth interval : absolute overlap first direction
        testFrom = createGregorianCalendar(from + 5);
        testTo = createGregorianCalendar(to - 5);
        assertSame(singleUrnPrefixRS.getReservations(testFrom, testTo).size(), 10);

        //fourth interval : absolute overlap second direction
        testFrom = createGregorianCalendar(from - 5);
        testTo = createGregorianCalendar(to + 5);
        assertSame(singleUrnPrefixRS.getReservations(testFrom, testTo).size(), 10);
    }

    private XMLGregorianCalendar createGregorianCalendar(long from) throws DatatypeConfigurationException {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(from);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }

    public void deleteReservationBeforeDeletion() throws RSExceptionException, ReservervationNotFoundExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++) {
            List<SecretReservationKey> tempKeyList = new LinkedList<SecretReservationKey>();
            tempKeyList.add(reservationKeyMap.get(i));
            singleUrnPrefixRS.deleteReservation(Collections.<SecretAuthenticationKey>emptyList(), tempKeyList);
        }
    }

    public void deleteReservationAfterDeletion() throws RSExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++) {
            List<SecretReservationKey> tempKeyList = new LinkedList<SecretReservationKey>();
            tempKeyList.add(reservationKeyMap.get(i));
            try {
                singleUrnPrefixRS.deleteReservation(Collections.<SecretAuthenticationKey>emptyList(), tempKeyList);
                fail("Should have raised an ReservervationNotFoundExceptionException");
            }
            catch (ReservervationNotFoundExceptionException e) {
                ;
            }
        }
    }

    public void getConfindentialReservations() throws RSExceptionException, DatatypeConfigurationException {
        GetReservations period = createPeriod(from, to);
        assertSame(singleUrnPrefixRS.getConfidentialReservations(secretAuthenticationKeyList, period).size(), 10);

        period = createPeriod(from - 20000, to - 20000);
        assertSame(singleUrnPrefixRS.getConfidentialReservations(secretAuthenticationKeyList, period).size(), 0);

        //second interval : small overlap first direction
        period = createPeriod(from - 20000, to - 500);
        assertSame(singleUrnPrefixRS.getConfidentialReservations(secretAuthenticationKeyList, period).size(), 10);

//        //second interval : small overlap second direction
        period = createPeriod(from + 500, to + 20000);
        assertSame(singleUrnPrefixRS.getConfidentialReservations(secretAuthenticationKeyList, period).size(), 10);

        //third interval : overlap on the same timeline first direction
        period = createPeriod(from - 20000, from);
        assertSame(singleUrnPrefixRS.getConfidentialReservations(secretAuthenticationKeyList, period).size(), 0);

        //third interval : overlap on the same timeline second direction
        period = createPeriod(to, to + 20000);
        assertSame(singleUrnPrefixRS.getConfidentialReservations(secretAuthenticationKeyList, period).size(), 0);

        //fourth interval : absolute overlap first direction
        period = createPeriod(from + 5, to - 5);
        assertSame(singleUrnPrefixRS.getConfidentialReservations(secretAuthenticationKeyList, period).size(), 10);

        //fourth interval : absolute overlap second direction
        period = createPeriod(from - 5, to + 5);
        assertSame(singleUrnPrefixRS.getConfidentialReservations(secretAuthenticationKeyList, period).size(), 10);

    }

    private GetReservations createPeriod(long from, long to) throws DatatypeConfigurationException {
        GetReservations period = new GetReservations();
        GregorianCalendar gregFrom = new GregorianCalendar();
        GregorianCalendar gregTo = new GregorianCalendar();
        gregFrom.setTimeInMillis(from);
        gregTo.setTimeInMillis(to);
        period.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregFrom));
        period.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregTo));
        return period;
    }
}
