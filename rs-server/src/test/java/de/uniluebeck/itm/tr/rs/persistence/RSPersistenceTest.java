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

import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.junit.Assert.*;

public abstract class RSPersistenceTest {

	static {
		Logging.setLoggingDefaults();
	}

	protected RSPersistence persistence;

	protected Map<Integer, ConfidentialReservationData> reservationDataMap = newHashMap();

	protected Map<Integer, SecretReservationKey> reservationKeyMap = newHashMap();

	/**
	 * The point in time that all reservations of this unit test will start from.
	 */
	protected static DateTime reservationStartingTime = new DateTime().plusHours(1);

	/**
	 * The point in time that all reservation of this unit test will end on.
	 */
	protected static DateTime reservationEndingTime = reservationStartingTime.plusMinutes(30);

	protected static final NodeUrnPrefix NODE_URN_PREFIX = new NodeUrnPrefix("urn:wisebed:uzl1:");

	protected static final int PARALLEL_RESERVATIONS = 5;

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

	static {

		String description;

		description =
				"query interval overlaps, ranging from the exact starting point until the exact ending point in time";
		intervals.add(new IntervalData(reservationStartingTime, reservationEndingTime, PARALLEL_RESERVATIONS, description));

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
				PARALLEL_RESERVATIONS, description
		)
		);

		description = "query interval overlaps on the start of the reservation interval";
		intervals
				.add(new IntervalData(reservationStartingTime.minusMillis(20000), reservationStartingTime.plusMillis(1),
						PARALLEL_RESERVATIONS, description
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
				PARALLEL_RESERVATIONS, description
		)
		);

		description =
				"query interval fully overlaps, ranging from a point before reservation start until after reservation interval";
		intervals.add(new IntervalData(reservationStartingTime.minusMillis(5), reservationEndingTime.plusMillis(5),
				PARALLEL_RESERVATIONS, description
		)
		);

	}

	@Before
	public void setUp() throws RSFault_Exception {
		for (int i = 0; i < PARALLEL_RESERVATIONS; i++) {
			ConfidentialReservationData crd = new ConfidentialReservationData();
			crd.setFrom(reservationStartingTime);
			crd.setTo(reservationEndingTime);
			crd.setDescription("description_" + i);
			crd.getNodeUrns().add(new NodeUrn(NODE_URN_PREFIX, Integer.toString(i)));
			final SecretReservationKey srk = new SecretReservationKey();
			srk.setKey("srk_" + Integer.toString(i));
			srk.setUrnPrefix(NODE_URN_PREFIX);
			crd.setSecretReservationKey(srk);
			final KeyValuePair pair = new KeyValuePair();
			pair.setKey("key_" + i);
			pair.setValue("value_" + i);
			crd.getOptions().add(pair);
			reservationDataMap.put(i, crd);
		}
	}

	@After
	public void tearDown() throws Exception {
		for (int i = 0; i < reservationKeyMap.size(); i++) {
			try {
				persistence.deleteReservation(reservationKeyMap.get(i));
			} catch (UnknownSecretReservationKeyFault ignored) {
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
	 * Makes {@link RSPersistenceTest#PARALLEL_RESERVATIONS}
	 * reservations, each for different node URNs, starting at {@link RSPersistenceTest#reservationStartingTime}
	 * and stopping at {@link RSPersistenceTest#reservationEndingTime}.
	 *
	 * @throws Exception
	 */
	protected void makeReservations() throws Exception {
		for (int i = 0; i < reservationDataMap.size(); i++) {
			final ConfidentialReservationData crd = reservationDataMap.get(i);
			reservationKeyMap.put(
					i,
					persistence.addReservation(
							crd.getNodeUrns(),
							crd.getFrom(),
							crd.getTo(),
							crd.getUsername(),
							crd.getSecretReservationKey().getUrnPrefix(),
							crd.getDescription(),
							crd.getOptions()
					).getSecretReservationKey()
			);
		}
	}

	public void checkGetReservationBeforeDeletion()
			throws RSFault_Exception, UnknownSecretReservationKeyFault {
		for (int i = 0; i < reservationDataMap.size(); i++) {

			ConfidentialReservationData rememberedCRD = reservationDataMap.get(i);
			ConfidentialReservationData receivedCRD = persistence.getReservation(reservationKeyMap.get(i));

			assertEquals(rememberedCRD.getNodeUrns(), receivedCRD.getNodeUrns());
			assertEquals(rememberedCRD.getFrom(), receivedCRD.getFrom());
			assertEquals(rememberedCRD.getTo(), receivedCRD.getTo());
			assertEquals(rememberedCRD.getDescription(), receivedCRD.getDescription());
			assertEqualOptions(rememberedCRD, receivedCRD);
		}
	}

	public void checkGetReservationAfterDeletion() throws RSFault_Exception {
		for (int i = 0; i < reservationKeyMap.size(); i++) {
			try {
				persistence.getReservation(reservationKeyMap.get(i));
				fail("Should have raised an ReservationNotFoundFault_Exception");
			} catch (UnknownSecretReservationKeyFault ignored) {
			}
		}
	}

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
			throws RSFault_Exception, UnknownSecretReservationKeyFault {
		for (int i = 0; i < reservationKeyMap.size(); i++) {
			ConfidentialReservationData actual = persistence.deleteReservation(reservationKeyMap.get(i));
			ConfidentialReservationData expected = reservationDataMap.get(i);
			assertEquals(actual.getFrom(), expected.getFrom());
			assertEquals(actual.getTo(), expected.getTo());
			assertEquals(actual.getNodeUrns(), expected.getNodeUrns());
			assertEquals(actual.getDescription(), expected.getDescription());
			assertEqualOptions(actual, expected);
		}
	}

	private void assertEqualOptions(final ConfidentialReservationData actual,
									final ConfidentialReservationData expected) {
		for (KeyValuePair expectedPair : expected.getOptions()) {
			boolean foundPair = false;
			for (KeyValuePair actualPair : actual.getOptions()) {
				if (expectedPair.getKey().equals(actualPair.getKey()) && expectedPair.getValue()
						.equals(actualPair.getValue())) {
					foundPair = true;
				}
			}
			assertTrue(foundPair);
		}
	}

	public void checkDeleteReservationAfterDeletion() throws RSFault_Exception {
		for (int i = 0; i < reservationKeyMap.size(); i++) {
			try {
				persistence.deleteReservation(reservationKeyMap.get(i));
				fail("Should have raised an ReservationNotFoundFault_Exception");
			} catch (UnknownSecretReservationKeyFault ignored) {
			}
		}
	}

}
