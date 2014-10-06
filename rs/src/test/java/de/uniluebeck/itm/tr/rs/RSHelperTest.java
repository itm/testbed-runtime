package de.uniluebeck.itm.tr.rs;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.rs.PublicReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.wiseml.Setup;
import eu.wisebed.wiseml.WiseMLHelper;
import eu.wisebed.wiseml.Wiseml;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RSHelperTest {

	private static final NodeUrn NODE_1 = new NodeUrn("urn:wisebed:uzl1:0x0001");

	private static final NodeUrn NODE_2 = new NodeUrn("urn:wisebed:uzl1:0x0002");

	private static final NodeUrn NODE_3 = new NodeUrn("urn:wisebed:uzl1:0x0003");

	private static final NodeUrn NODE_4 = new NodeUrn("urn:wisebed:uzl1:0x0004");

	private static final Set<NodeUrn> NODE_URNS = newHashSet(NODE_1, NODE_2, NODE_3, NODE_4);

	private static final DateTime NOW = DateTime.now();

	private static final Interval INTERVAL_1 = new Interval(NOW, NOW.plusHours(7));

	private static final Interval INTERVAL_2 = new Interval(NOW, NOW.plusHours(3));

	private static final Interval INTERVAL_3 = new Interval(NOW.plusHours(3), NOW.plusHours(4));

	private static final Interval RESERVATION_NODE_1 = new Interval(NOW.minusHours(1), NOW.plusHours(2));

	private static final Interval RESERVATION_NODE_2 = new Interval(NOW.plusHours(1), NOW.plusHours(5));

	private static final Interval RESERVATION_NODE_3 = new Interval(NOW.plusHours(6), NOW.plusHours(7));

	private static final PublicReservationData RESERVATION_DATA_1;

	private static final PublicReservationData RESERVATION_DATA_2;

	private static final PublicReservationData RESERVATION_DATA_3;

	static {

		RESERVATION_DATA_1 = new PublicReservationData();
		RESERVATION_DATA_1.setFrom(RESERVATION_NODE_1.getStart());
		RESERVATION_DATA_1.setTo(RESERVATION_NODE_1.getEnd());
		RESERVATION_DATA_1.getNodeUrns().add(NODE_1);

		RESERVATION_DATA_2 = new PublicReservationData();
		RESERVATION_DATA_2.setFrom(RESERVATION_NODE_2.getStart());
		RESERVATION_DATA_2.setTo(RESERVATION_NODE_2.getEnd());
		RESERVATION_DATA_2.getNodeUrns().add(NODE_2);

		RESERVATION_DATA_3 = new PublicReservationData();
		RESERVATION_DATA_3.setFrom(RESERVATION_NODE_3.getStart());
		RESERVATION_DATA_3.setTo(RESERVATION_NODE_3.getEnd());
		RESERVATION_DATA_3.getNodeUrns().add(NODE_3);
	}

	private static final ArrayList<PublicReservationData> ALL_RESERVATIONS =
			newArrayList(RESERVATION_DATA_1, RESERVATION_DATA_2, RESERVATION_DATA_3);

	@Mock
	private RS rs;

	@Mock
	private SessionManagement sm;

	private RSHelper rsHelper;

	private static final Instant INSTANT_1 = NOW.toInstant();

	private static final Instant INSTANT_2 = NOW.plusHours(4).toInstant();

	private static final Instant INSTANT_3 = NOW.plusHours(5).plusMinutes(30).toInstant();

	private static final Instant INSTANT_4 = NOW.plusHours(6).plusMinutes(30).toInstant();

	private static final Set<NodeUrn> EXPECTED_UNRESERVED_INSTANT_1 = newHashSet(NODE_2, NODE_3, NODE_4);

	private static final Set<NodeUrn> EXPECTED_RESERVED_INSTANT_1 =
			Sets.difference(NODE_URNS, EXPECTED_UNRESERVED_INSTANT_1);

	private static final Set<NodeUrn> EXPECTED_UNRESERVED_INSTANT_2 = newHashSet(NODE_1, NODE_3, NODE_4);

	private static final Set<NodeUrn> EXPECTED_RESERVED_INSTANT_2 =
			Sets.difference(NODE_URNS, EXPECTED_UNRESERVED_INSTANT_2);

	private static final Set<NodeUrn> EXPECTED_UNRESERVED_INSTANT_3 = NODE_URNS;

	private static final Set<NodeUrn> EXPECTED_RESERVED_INSTANT_3 = newHashSet();

	private static final Set<NodeUrn> EXPECTED_UNRESERVED_INSTANT_4 = newHashSet(NODE_1, NODE_2, NODE_4);

	private static final Set<NodeUrn> EXPECTED_RESERVED_INSTANT_4 =
			Sets.difference(NODE_URNS, EXPECTED_UNRESERVED_INSTANT_4);

	private static final Set<NodeUrn> EXPECTED_UNRESERVED_INTERVAL_1 = newHashSet(NODE_4);

	private static final Set<NodeUrn> EXPECTED_RESERVED_INTERVAL_1 =
			Sets.difference(NODE_URNS, EXPECTED_UNRESERVED_INTERVAL_1);

	private static final Set<NodeUrn> EXPECTED_UNRESERVED_INTERVAL_2 = newHashSet(NODE_3, NODE_4);

	private static final Set<NodeUrn> EXPECTED_RESERVED_INTERVAL_2 =
			Sets.difference(NODE_URNS, EXPECTED_UNRESERVED_INTERVAL_2);

	private static final Set<NodeUrn> EXPECTED_UNRESERVED_INTERVAL_3 = newHashSet(NODE_1, NODE_3, NODE_4);

	private static final Set<NodeUrn> EXPECTED_RESERVED_INTERVAL_3 =
			Sets.difference(NODE_URNS, EXPECTED_UNRESERVED_INTERVAL_3);

	@Before
	public void setUp() throws Exception {

		setUpRS(INTERVAL_1.getStart(), INTERVAL_1.getEnd(), ALL_RESERVATIONS);
		setUpRS(INTERVAL_2.getStart(), INTERVAL_2.getEnd(), newArrayList(RESERVATION_DATA_1, RESERVATION_DATA_2));
		setUpRS(INTERVAL_3.getStart(), INTERVAL_3.getEnd(), newArrayList(RESERVATION_DATA_2));

		setUpRS(INSTANT_1.toDateTime(), INSTANT_1.toDateTime(), newArrayList(RESERVATION_DATA_1));
		setUpRS(INSTANT_2.toDateTime(), INSTANT_2.toDateTime(), newArrayList(RESERVATION_DATA_2));
		setUpRS(INSTANT_3.toDateTime(), INSTANT_3.toDateTime(), Lists.<PublicReservationData>newArrayList());
		setUpRS(INSTANT_4.toDateTime(), INSTANT_4.toDateTime(), newArrayList(RESERVATION_DATA_3));

		final Wiseml wiseml = new Wiseml();
		final Setup.Node node1 = new Setup.Node();
		node1.setId(NODE_1.toString());
		final Setup.Node node2 = new Setup.Node();
		node2.setId(NODE_2.toString());
		final Setup.Node node3 = new Setup.Node();
		node3.setId(NODE_3.toString());
		final Setup.Node node4 = new Setup.Node();
		node4.setId(NODE_4.toString());
		wiseml.setSetup(new Setup());
		wiseml.getSetup().getNode().add(node1);
		wiseml.getSetup().getNode().add(node2);
		wiseml.getSetup().getNode().add(node3);
		wiseml.getSetup().getNode().add(node4);
		when(sm.getNetwork()).thenReturn(WiseMLHelper.serialize(wiseml));

		rsHelper = new RSHelperImpl(rs, sm);
	}

	private void setUpRS(final DateTime start, final DateTime end, final ArrayList<PublicReservationData> returnValue)
			throws RSFault_Exception {
		when(
				rs.getReservations(
						eq(start),
						eq(end),
						isNull(Integer.class),
						isNull(Integer.class),
						any(Boolean.class)
				)
		).thenReturn(returnValue);
	}

	@Test
	public void testIfCorrectForInstants() throws Exception {

		assertEquals(EXPECTED_UNRESERVED_INSTANT_1, rsHelper.getUnreservedNodes(INSTANT_1));
		assertEquals(EXPECTED_RESERVED_INSTANT_1, rsHelper.getReservedNodes(INSTANT_1));

		assertEquals(EXPECTED_UNRESERVED_INSTANT_2, rsHelper.getUnreservedNodes(INSTANT_2));
		assertEquals(EXPECTED_RESERVED_INSTANT_2, rsHelper.getReservedNodes(INSTANT_2));

		assertEquals(EXPECTED_UNRESERVED_INSTANT_3, rsHelper.getUnreservedNodes(INSTANT_3));
		assertEquals(EXPECTED_RESERVED_INSTANT_3, rsHelper.getReservedNodes(INSTANT_3));

		assertEquals(EXPECTED_UNRESERVED_INSTANT_4, rsHelper.getUnreservedNodes(INSTANT_4));
		assertEquals(EXPECTED_RESERVED_INSTANT_4, rsHelper.getReservedNodes(INSTANT_4));
	}

	@Test
	public void testIfCorrectForIntervals() throws Exception {

		assertEquals(EXPECTED_UNRESERVED_INTERVAL_1, rsHelper.getUnreservedNodes(INTERVAL_1));
		assertEquals(EXPECTED_RESERVED_INTERVAL_1, rsHelper.getReservedNodes(INTERVAL_1));

		assertEquals(EXPECTED_UNRESERVED_INTERVAL_2, rsHelper.getUnreservedNodes(INTERVAL_2));
		assertEquals(EXPECTED_RESERVED_INTERVAL_2, rsHelper.getReservedNodes(INTERVAL_2));

		assertEquals(EXPECTED_UNRESERVED_INTERVAL_3, rsHelper.getUnreservedNodes(INTERVAL_3));
		assertEquals(EXPECTED_RESERVED_INTERVAL_3, rsHelper.getReservedNodes(INTERVAL_3));
	}
}
