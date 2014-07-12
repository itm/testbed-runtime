package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.eventstore.IEventStore;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;
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
    private IEventStore eventStore;

    @Mock
    private ReservationEventBus reservationEventBus;

    private ReservationEventStoreImpl reservationEventStore;

    @Before
    public void setUp() throws Exception {

        when(reservation.getSerializedKey()).thenReturn(KEY);
        when(reservation.getEventBus()).thenReturn(reservationEventBus);
        when(portalServerConfig.getEventStorePath()).thenReturn(System.getProperty("java.io.tmpdir"));
        when(reservationManager.getReservation(KEY)).thenReturn(reservation);

		when(helper.eventStoreExistsForReservation(KEY)).thenReturn(false);
        when(helper.createAndConfigureEventStore(KEY)).thenReturn(eventStore);

        reservationEventStore = new ReservationEventStoreImpl(helper, reservation);
		reservationEventStore.startAndWait();
	}

	@After
	public void tearDown() throws Exception {
		reservationEventStore.stopAndWait();
	}

	@Test
    public void testIfBusObserversAreRegistered() throws Exception {

        reservationEventStore.startAndWait();
        verify(reservationEventBus).register(reservationEventStore);

        reservationEventStore.stopAndWait();
        verify(reservationEventBus).unregister(reservationEventStore);

        verify(eventStore).close();
    }

	@Test
	public void testIfReservationStartedEventIsPersisted() throws Exception {

		when(eventStore.isEmpty()).thenReturn(true, false);

		final ReservationStartedEvent event = ReservationStartedEvent.newBuilder().setSerializedKey(KEY).build();
		reservationEventStore.onEvent(event);
		//noinspection unchecked
		verify(eventStore).storeEvent(event, event.getClass());
	}

	@Test
	public void testIfReservationEndedEventIsPersisted() throws Exception {

		final ReservationEndedEvent event = ReservationEndedEvent.newBuilder().setSerializedKey(KEY).build();
		reservationEventStore.onEvent(event);
		//noinspection unchecked
		verify(eventStore).storeEvent(event, event.getClass());
	}
}
