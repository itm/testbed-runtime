///**********************************************************************************************************************
// * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
// * All rights reserved.                                                                                               *
// *                                                                                                                    *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
// * following conditions are met:                                                                                      *
// *                                                                                                                    *
// * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
// *   disclaimer.                                                                                                      *
// * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
// *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
// * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
// *   products derived from this software without specific prior written permission.                                   *
// *                                                                                                                    *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
// **********************************************************************************************************************/
//
//package de.uniluebeck.itm.tr.rs.persistence.test;
//
//import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
//import eu.wisebed.api.rs.*;
//import org.joda.time.Interval;
//import org.junit.Before;
//import org.junit.Test;
//
//import javax.xml.datatype.DatatypeConfigurationException;
//import javax.xml.datatype.DatatypeFactory;
//import java.util.GregorianCalendar;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.TimeZone;
//
//import static org.junit.Assert.*;
//
//public abstract class RSPersistenceTest {
//
//    public Map<Integer, ConfidentialReservationData> reservationDataList = null;
//    public Map<Integer, SecretReservationKey> reservationKeyList = null;
//    private static long from = System.currentTimeMillis();
//    private static long to = System.currentTimeMillis() + 1000;
//    public static String urnPrefix = "urn:UZL";
//    private RSPersistence persistence = null;
//
//    public void setPersistence(RSPersistence persistence) {
//        this.persistence = persistence;
//    }
//
//    public RSPersistence getPersistence() {
//        return this.persistence;
//    }
//
//    @Before
//    public void setUp() throws RSExceptionException, DatatypeConfigurationException {
//        reservationDataList = new HashMap<Integer, ConfidentialReservationData>();
//        //Creating testdata
//        for (int i = 0; i < 10; i++) {
//            ConfidentialReservationData data = new ConfidentialReservationData();
//
//            GregorianCalendar gregorianCalendarFrom = new GregorianCalendar();
//            gregorianCalendarFrom.setTimeInMillis(from);
//            gregorianCalendarFrom.setTimeZone(TimeZone.getTimeZone("GMT"));
//            data.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarFrom));
//
//            GregorianCalendar gregorianCalendarTo = new GregorianCalendar();
//            gregorianCalendarTo.setTimeInMillis(to);
//            gregorianCalendarTo.setTimeZone(TimeZone.getTimeZone("GMT"));
//            data.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarTo));
//
//            User user = new User();
//			user.setUrnPrefix(urnPrefix);
//			user.setUsername("User "+ i);
//			data.getUsers().add(user);
//
//            reservationDataList.put(i, data);
//        }
//    }
//
//
//    @Test
//    public void test() throws Throwable {
//        this.addReservations();
//        this.getReservations();
//        this.getReservationBeforeDeletion();
//        this.deleteReservationBeforeDeletion();
//        this.getReservationAfterDeletion();
//        this.deleteReservationAfterDeletion();
//    }
//
//    public void addReservations() throws Throwable {
//        this.reservationKeyList = new HashMap<Integer, SecretReservationKey>();
//        for (int i = 0; i < reservationDataList.size(); i++) {
//            this.reservationKeyList.put(i, persistence.addReservation(reservationDataList.get(i), urnPrefix));
//        }
//    }
//
//    public void getReservationBeforeDeletion() throws RSExceptionException, ReservervationNotFoundExceptionException {
//        for (int i = 0; i < reservationKeyList.size(); i++) {
//            assertTrue(equals(persistence.getReservation(reservationKeyList.get(i)), reservationDataList.get(i)));
//        }
//    }
//
//    public void getReservationAfterDeletion() throws RSExceptionException {
//        for (int i = 0; i < reservationKeyList.size(); i++) {
//            try {
//                persistence.getReservation(reservationKeyList.get(i));
//                fail("Should have raised an ReservervationNotFoundExceptionException");
//            }catch (ReservervationNotFoundExceptionException e){
//
//            }
//        }
//    }
//
//
//    public void getReservations() throws RSExceptionException {
//        //first interval : no overlap first direction
//        long testFrom = 0;
//        long testTo = 0;
//
//        testFrom = from - 20000;
//        testTo = to - 20000;
//        Interval testInterval = new Interval(testFrom, testTo);
//        assertSame (persistence.getReservations(testInterval).size(), 0);
//
//        //first interval : no overlap second direction
//        testFrom = from + 20000;
//        testTo = to + 20000;
//        testInterval = new Interval(testFrom, testTo);
//        assertSame (persistence.getReservations(testInterval).size(), 0);
//
//        //second interval : small overlap first direction
//        //TODO for GCAL not working
//        testFrom = from - 20000;
//        testTo = to - 500;
//        testInterval = new Interval(testFrom, testTo);
//        persistence.getReservations(testInterval);
//        assertSame (persistence.getReservations(testInterval).size(), 10);
//
//        //second interval : small overlap second direction
//        //TODO for GCAL not working
//        testFrom = from + 500;
//        testTo = to + 20000;
//        testInterval = new Interval(testFrom, testTo);
//        persistence.getReservations(testInterval);
//        assertSame (persistence.getReservations(testInterval).size(), 10);
//
//        //third interval : overlap on the same timeline first direction
//        testFrom = from - 20000;
//        testTo = from;
//        testInterval = new Interval(testFrom, testTo);
//        assertSame (persistence.getReservations(testInterval).size(), 0);
//
//        //third interval : overlap on the same timeline second direction
//        testFrom = to;
//        testTo = to + 20000;
//        testInterval = new Interval(testFrom, testTo);
//        assertSame (persistence.getReservations(testInterval).size(), 0);
//
//        //fourth interval : absolute overlap first direction
//        //TODO for GCAL not working
//        testFrom = from + 5;
//        testTo = to - 5;
//        testInterval = new Interval(testFrom, testTo);
//        assertSame (persistence.getReservations(testInterval).size(), 10);
//
//        //fourth interval : absolute overlap second direction
//        //TODO for GCAL not working
//        testFrom = from - 5;
//        testTo = to + 5;
//        testInterval = new Interval(testFrom, testTo);
//        assertSame (persistence.getReservations(testInterval).size(), 10);
//
//    }
//
//    public void deleteReservationBeforeDeletion() throws RSExceptionException, ReservervationNotFoundExceptionException {
//        for (int i = 0; i < this.reservationKeyList.size(); i++) {
//            assertTrue(equals(persistence.deleteReservation(reservationKeyList.get(i)), reservationDataList.get(i)));
//        }
//    }
//
//	private boolean equals(ConfidentialReservationData reservationData, ConfidentialReservationData reservationDataOther) {
//		// TODO implement (im moment noch unschoen!!)
//        //if (!reservationData.getUsers().get(0).getUsername().equals(reservationDataOther.getUsers().get(0).getUsername())) return false;
//        //if (reservationData.getFrom().toGregorianCalendar().getTimeInMillis() != reservationDataOther.getFrom().toGregorianCalendar().getTimeInMillis()) return false;
//        //if (reservationData.getTo().toGregorianCalendar().getTimeInMillis() != reservationDataOther.getTo().toGregorianCalendar().getTimeInMillis()) return false;
//
//        return Comparison.equals(reservationData, reservationDataOther);
//    }
//
//    public void deleteReservationAfterDeletion() throws RSExceptionException {
//        for (int i = 0; i < this.reservationKeyList.size(); i++) {
//            try {
//                persistence.deleteReservation(reservationKeyList.get(i));
//                fail("Should have raised an ReservervationNotFoundExceptionException");
//            }catch (ReservervationNotFoundExceptionException e){;}
//        }
//    }
//}
