package de.uniluebeck.itm.tr.rs.persistence;

import com.google.common.collect.Sets;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;

public abstract class RSPersistenceOffsetAmountTest {

	private static final NodeUrnPrefix PREFIX = new NodeUrnPrefix("urn:wisebed:uzl1:");

	private static final NodeUrn NODE_URN_0 = new NodeUrn(PREFIX, "0x0000");

	private static final NodeUrn NODE_URN_1 = new NodeUrn(PREFIX, "0x0001");

	private static final NodeUrn NODE_URN_2 = new NodeUrn(PREFIX, "0x0002");

	private static final NodeUrn NODE_URN_3 = new NodeUrn(PREFIX, "0x0003");

	private static final NodeUrn NODE_URN_4 = new NodeUrn(PREFIX, "0x0004");

	private static final NodeUrn NODE_URN_5 = new NodeUrn(PREFIX, "0x0005");

	private static final NodeUrn NODE_URN_6 = new NodeUrn(PREFIX, "0x0006");

	private static final DateTime EVEN_EARLIER = DateTime.now().minusHours(2);

	private static final DateTime EARLIER = DateTime.now().minusHours(1);

	private static final DateTime NOW = DateTime.now();

	private static final DateTime THEN = DateTime.now().plusHours(1);

	private static final Interval INTERVAL = new Interval(NOW, THEN);

	private static final DateTime LATER = DateTime.now().plusHours(2);

	private static final DateTime EVEN_LATER = DateTime.now().plusHours(3);

	private static final List<KeyValuePair> OPT = newArrayList();

	private static final String USERNAME = "username";

	private static final String DESC = "description";

	private Set<ConfidentialReservationData> reservations;

	private Set<ConfidentialReservationData> matchingReservations;

	private RSPersistence persistence;

	private ConfidentialReservationData res0;

	private ConfidentialReservationData res1;

	private ConfidentialReservationData res2;

	private ConfidentialReservationData res3;

	private ConfidentialReservationData res4;

	private ConfidentialReservationData res5;

	private ConfidentialReservationData res6;

	protected void setUp(final RSPersistence persistence) throws Exception {

		this.persistence = persistence;

		res0 = persistence.addReservation(newArrayList(NODE_URN_0), EVEN_EARLIER, EARLIER, USERNAME, PREFIX, DESC, OPT);
		res1 = persistence.addReservation(newArrayList(NODE_URN_1), NOW.plusMinutes(0), THEN, USERNAME, PREFIX, DESC, OPT);
		res2 = persistence.addReservation(newArrayList(NODE_URN_2), NOW.plusMinutes(1), THEN, USERNAME, PREFIX, DESC, OPT);
		res3 = persistence.addReservation(newArrayList(NODE_URN_3), NOW.plusMinutes(2), THEN, USERNAME, PREFIX, DESC, OPT);
		res4 = persistence.addReservation(newArrayList(NODE_URN_4), NOW.plusMinutes(3), THEN, USERNAME, PREFIX, DESC, OPT);
		res5 = persistence.addReservation(newArrayList(NODE_URN_5), NOW.plusMinutes(4), THEN, USERNAME, PREFIX, DESC, OPT);
		res6 = persistence.addReservation(newArrayList(NODE_URN_6), LATER, EVEN_LATER, USERNAME, PREFIX, DESC, OPT);

		reservations = newHashSet();
		reservations.add(res0);
		reservations.add(res1);
		reservations.add(res2);
		reservations.add(res3);
		reservations.add(res4);
		reservations.add(res5);
		reservations.add(res6);

		matchingReservations = newHashSet();
		matchingReservations.add(res1);
		matchingReservations.add(res2);
		matchingReservations.add(res3);
		matchingReservations.add(res4);
		matchingReservations.add(res5);
	}

	@Test
	public void test() throws Exception {
		assertMatch(query(0, 5), matchingReservations);
		assertMatch(query(0, 6), matchingReservations);
		assertMatch(query(0, 4), newHashSet(res5, res4, res3, res2));
		assertMatch(query(1, 4), newHashSet(res4, res3, res2, res1));
		assertMatch(query(1, 3), newHashSet(res4, res3, res2));
		assertMatch(query(1, 6), newHashSet(res4, res3, res2, res1));
		assertMatch(query(4, 1), newHashSet(res1));
		assertMatch(query(5, 1), Sets.<ConfidentialReservationData>newHashSet());
	}

	private void assertMatch(final Set<ConfidentialReservationData> actualSet,
							 final Set<ConfidentialReservationData> expectedSet) throws RSFault_Exception {
		assertEquals(expectedSet.size(), actualSet.size());
		assertEquals(expectedSet, actualSet);
	}

	private Set<ConfidentialReservationData> query(final int offset, final int amount) throws RSFault_Exception {
		return newHashSet(persistence.getReservations(INTERVAL.getStart(), INTERVAL.getEnd(), offset, amount, null));
	}
}
