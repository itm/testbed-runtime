package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.EventContainer;
import de.uniluebeck.itm.eventstore.EventStore;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReservationEventStoreImplTest {

	private static final String KEY = "abcd";

	@Mock
	private Reservation reservation;

	@Mock
	private PortalEventStoreHelper helper;

	@Mock
	private PortalServerConfig portalServerConfig;

	@Mock
	private ReservationManager reservationManager;

	@Mock
	private EventStore eventStore;

	@Mock
	private ReservationEventBus reservationEventBus;

	@Mock
	private CloseableIterator<EventContainer> storeIterator;

	private ReservationEventStoreImpl reservationEventStore;

	@Before
	public void setUp() throws Exception {

		when(reservation.getSerializedKey()).thenReturn(KEY);
		when(reservation.getEventBus()).thenReturn(reservationEventBus);
		when(portalServerConfig.getEventStorePath()).thenReturn(System.getProperty("java.io.tmpdir"));
		when(reservationManager.getReservation(KEY)).thenReturn(reservation);

		when(helper.eventStoreExistsForReservation(KEY)).thenReturn(false);
		when(helper.createAndConfigureEventStore(KEY)).thenReturn(eventStore);
		when(eventStore.getAllEvents()).thenReturn(storeIterator);
		when(eventStore.getEventsBetweenTimestamps(any(Long.class), any(Long.class))).thenReturn(storeIterator);
		when(eventStore.getEventsFromTimestamp(any(Long.class))).thenReturn(storeIterator);
		when(storeIterator.hasNext()).thenReturn(false);

		reservationEventStore = new ReservationEventStoreImpl(helper, reservation);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testIfBusObserversAreRegistered() throws Exception {

		reservationEventStore.startAsync().awaitRunning();
		verify(reservationEventBus).register(reservationEventStore);

		reservationEventStore.stopAsync().awaitTerminated();
		verify(reservationEventBus).unregister(reservationEventStore);

		verify(eventStore).close();
	}


}
