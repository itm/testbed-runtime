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

package de.uniluebeck.itm.tr.rs.persistence;

import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.RSExceptionException;
import eu.wisebed.api.rs.ReservervationNotFoundExceptionException;
import eu.wisebed.api.rs.SecretReservationKey;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static org.junit.Assert.*;

public abstract class RSPersistenceTest {

	private RSPersistence persistence;

	private Map<Integer, ConfidentialReservationData> reservationDataMap =
			new HashMap<Integer, ConfidentialReservationData>();

	private Map<Integer, SecretReservationKey> reservationKeyMap = new HashMap<Integer, SecretReservationKey>();

	/**
	 * The point in time that all reservations of this unit test will start from.
	 */
	protected static DateTime reservationStartingTime = new DateTime().plusHours(1);

	/**
	 * The point in time that all reservation of this unit test will end on.
	 */
	protected static DateTime reservationEndingTime = reservationStartingTime.plusMinutes(30);

	protected static final int RESERVATION_COUNT = 5;

	private static final String URN_PREFIX = "urn:unittest:testbed1:";

	protected static class IntervalData {

		public DateTime from;

		public DateTime until;

		public Integer expectedReservationCount;

		public String description;

		public IntervalData(final DateTime from, final DateTime until, final Integer expectedReservationCount,
							final String description) {
			this.from = from;
			this.until = until;
			this.expectedReservationCount = expectedReservationCount;
			this.description = description;
		}
	}

	public void setPersistence(RSPersistence persistence) {
		this.persistence = persistence;
	}

	/**
	 * Map that contains a mapping between tuples of {@link org.joda.time.DateTime} instances (start, end) that stand for
	 * intervals and an {@link Integer} value indicating how many reservations there should be in the interval. Used in
	 * {@link RSPersistenceTest#testGetReservations()}.
	 */
	private static final List<IntervalData> intervals = new ArrayList<IntervalData>();

	private static final DatatypeFactory datatypeFactory;

	static {

		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}

		String description;

		description =
				"query interval overlaps, ranging from the exact starting point until the exact ending point in time";
		intervals.add(new IntervalData(reservationStartingTime, reservationEndingTime, RESERVATION_COUNT, description));

		description = "query interval does not overlap, since it lies before reservation interval";
		intervals.add(new IntervalData(reservationStartingTime.minusMillis(20000),
				reservationStartingTime.minusMillis(1), 0, description
		)
		);

		description = "query interval does not overlap, since it lies after reservation interval";
		intervals.add(new IntervalData(reservationEndingTime.plusMillis(1), reservationEndingTime.plusMillis(20000), 0,
				description
		)
		);

		description = "query interval overlaps on the end of the reservation interval";
		intervals.add(new IntervalData(reservationEndingTime.minusMillis(1), reservationEndingTime.plusMillis(20000),
				RESERVATION_COUNT, description
		)
		);

		description = "query interval overlaps on the start of the reservation interval";
		intervals
				.add(new IntervalData(reservationStartingTime.minusMillis(20000), reservationStartingTime.plusMillis(1),
						RESERVATION_COUNT, description
				)
				);

		description = "query interval overlaps on the exact millisecond on reservation start";
		intervals.add(new IntervalData(reservationStartingTime.minusMillis(20000), reservationStartingTime, 0,
				description
		)
		);

		description = "query interval overlaps on the exact millisecond on reservation end";
		intervals.add(new IntervalData(reservationEndingTime, reservationEndingTime.plusMillis(20000), 0, description));

		description =
				"query interval fully overlaps, ranging from a point after reservation start until before reservation end";
		intervals.add(new IntervalData(reservationStartingTime.plusMillis(5), reservationEndingTime.minusMillis(5),
				RESERVATION_COUNT, description
		)
		);

		description =
				"query interval fully overlaps, ranging from a point before reservation start until after reservation interval";
		intervals.add(new IntervalData(reservationStartingTime.minusMillis(5), reservationEndingTime.plusMillis(5),
				RESERVATION_COUNT, description
		)
		);

	}

	@Before
	public void setUp() throws RSExceptionException {

		for (int i = 0; i < RESERVATION_COUNT; i++) {
			XMLGregorianCalendar from = datatypeFactory.newXMLGregorianCalendar(reservationStartingTime.toGregorianCalendar());
			XMLGregorianCalendar to = datatypeFactory.newXMLGregorianCalendar(reservationEndingTime.toGregorianCalendar());
			ConfidentialReservationData confidentialReservationData = new ConfidentialReservationData();
			confidentialReservationData.setFrom(from);
			confidentialReservationData.setTo(to);
			reservationDataMap.put(i, confidentialReservationData);
		}
	}

	@After
	public void tearDown() throws Exception {
		for (int i = 0; i < reservationKeyMap.size(); i++) {
			try {
				persistence.deleteReservation(reservationKeyMap.get(i));
			} catch (ReservervationNotFoundExceptionException e) {
			}
		}
		reservationDataMap = null;
	}

	@Test
	public void test() throws Throwable {
		makeReservations();
		checkGetReservationBeforeDeletion();
		checkDeleteReservation();
		checkGetReservationAfterDeletion();
		checkDeleteReservationAfterDeletion();
	}

	/**
	 * Makes {@link RSPersistenceTest#RESERVATION_COUNT}
	 * reservations, each for different node URNs, starting at {@link RSPersistenceTest#reservationStartingTime}
	 * and stopping at {@link RSPersistenceTest#reservationEndingTime}.
	 *
	 * @throws Exception
	 */
	protected void makeReservations() throws Exception {
		for (int i = 0; i < reservationDataMap.size(); i++) {
			reservationKeyMap.put(i, persistence.addReservation(reservationDataMap.get(i), URN_PREFIX));
		}
	}

	public void checkGetReservationBeforeDeletion() throws RSExceptionException, ReservervationNotFoundExceptionException {
		for (int i = 0; i < reservationDataMap.size(); i++) {

			List<SecretReservationKey> tempKeyList = new LinkedList<SecretReservationKey>();
			tempKeyList.add(reservationKeyMap.get(i));

			ConfidentialReservationData rememberedCRD = reservationDataMap.get(i);
			ConfidentialReservationData receivedCRD = persistence.getReservation(reservationKeyMap.get(i));

			assertEquals(rememberedCRD.getUserData(), receivedCRD.getUserData());
			assertEquals(rememberedCRD.getNodeURNs(), receivedCRD.getNodeURNs());
			assertEquals(rememberedCRD.getFrom(), receivedCRD.getFrom());
			assertEquals(rememberedCRD.getTo(), receivedCRD.getTo());
		}
	}

	public void checkGetReservationAfterDeletion() throws RSExceptionException {
		for (int i = 0; i < reservationKeyMap.size(); i++) {
			try {
				persistence.getReservation(reservationKeyMap.get(i));
				fail("Should have raised an ReservervationNotFoundExceptionException");
			} catch (ReservervationNotFoundExceptionException e) {
			}
		}
	}

	/**
	 * @throws RSExceptionException
	 * @throws DatatypeConfigurationException
	 */
	@Test
	public void testGetReservations() throws Exception {
		makeReservations();

		for (IntervalData id : intervals) {
			Interval period = new Interval(id.from.getMillis(), id.until.getMillis());
			int persistenceReservationCount = persistence.getReservations(period).size();
			assertSame(persistenceReservationCount, id.expectedReservationCount);
		}
	}

	public void checkDeleteReservation()
			throws RSExceptionException, ReservervationNotFoundExceptionException {
		for (int i = 0; i < reservationKeyMap.size(); i++) {
			ConfidentialReservationData actualData = persistence.deleteReservation(reservationKeyMap.get(i));
			ConfidentialReservationData expectedData = reservationDataMap.get(i);
			assertEquals(actualData.getFrom(), expectedData.getFrom());
			assertEquals(actualData.getTo(), expectedData.getTo());
			assertEquals(actualData.getNodeURNs(), expectedData.getNodeURNs());
			assertEquals(actualData.getUserData(), expectedData.getUserData());
		}
	}

	public void checkDeleteReservationAfterDeletion() throws RSExceptionException {
		for (int i = 0; i < reservationKeyMap.size(); i++) {
			try {
				persistence.deleteReservation(reservationKeyMap.get(i));
				fail("Should have raised an ReservervationNotFoundExceptionException");
			} catch (ReservervationNotFoundExceptionException expected) {
			}
		}
	}

}
