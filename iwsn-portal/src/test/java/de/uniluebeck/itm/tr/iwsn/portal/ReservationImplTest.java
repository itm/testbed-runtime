package de.uniluebeck.itm.tr.iwsn.portal;

import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReservationImplTest {

	private static final Set<NodeUrn> NODE_URNS = newHashSet(new NodeUrn("urn:unit-test:0x0001"));

	private static final Interval INTERVAL = new Interval(DateTime.now(), DateTime.now().plusHours(1));

	@Mock
	private ReservationEventBusFactory reservationEventBusFactory;

	@Mock
	private PortalEventBus portalEventBus;

	@Mock
	private ReservationEventBus reservationEventBus;

	private ReservationImpl reservation;

	@Before
	public void setUp() throws Exception {
		when(reservation.getNodeUrns()).thenReturn(NODE_URNS);
		when(reservationEventBusFactory.create(Matchers.<Reservation>any())).thenReturn(reservationEventBus);
		reservation = new ReservationImpl(reservationEventBusFactory, portalEventBus, NODE_URNS, INTERVAL);
	}

	@Test
	public void testThatReservationEventBusIsCreatedAndStartedWhenStartingReservation() throws Exception {
		reservation.startAndWait();
		verify(reservationEventBus).startAndWait();
	}

	@Test
	public void testThatReservationEventBusIsStoppedWhenStoppingReservation() throws Exception {
		reservation.startAndWait();
		reservation.stopAndWait();
		verify(reservationEventBus).stopAndWait();
	}

	@Test
	public void testThatReservationStartedEventIsPostedOnPortalEventBusWhenStartingReservation() throws Exception {
		reservation.startAndWait();
		verify(portalEventBus).post(eq(new ReservationStartedEvent(reservation)));
	}

	@Test
	public void testThatReservationEndedEventIsPostedOnPortalEventBusWhenStoppingReservation() throws Exception {
		reservation.startAndWait();
		reservation.stopAndWait();
		verify(portalEventBus).post(eq(new ReservationEndedEvent(reservation)));
	}
}
