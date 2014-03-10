package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.portal.*;
import eventstore.IEventContainer;
import eventstore.IEventStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Iterator;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PortalEventStoreServiceImplTest {

    @Mock
    private PortalEventBus portalEventBus;

    @Mock
    private ReservationEventStoreFactory reservationEventStoreFactory;

    @Mock
    private ReservationManager reservationManager;

    @Mock
    private Reservation reservation;

    @Mock
    private ReservationEventStore reservationEventStore;

    @Mock
    private PortalServerConfig portalServerConfig;

    @Mock
    private PortalEventStoreHelper portalEventStoreHelper;

    @Mock
    private IEventStore eventStore;

    private PortalEventStoreServiceImpl store;


    @Before
    public void setUp() throws Exception {
        when(portalServerConfig.getEventStorePath()).thenReturn(System.getProperty("java.io.tmpdir"));

        when(reservationEventStoreFactory.create(reservation)).thenReturn(reservationEventStore);
        store = new PortalEventStoreServiceImpl(portalEventBus, reservationEventStoreFactory, portalEventStoreHelper);

    }

    @Test
    public void testIfReservationStartedEventIsPersisted() throws Exception {
        when(reservation.getSerializedKey()).thenReturn("abc");
        when(reservationManager.getReservation("abc")).thenReturn(reservation);
        when(portalEventStoreHelper.createAndConfigureEventStore("abc")).thenReturn(eventStore);

        final ReservationStartedEvent event = mock(ReservationStartedEvent.class);
        when(event.getReservation()).thenReturn(reservation);

        store.onReservationStarted(event);
        verify(reservationEventStore).reservationStarted(event);

    }


    @Test
    public void testIfReservationEndedEventIsPersisted() throws Exception {
        when(reservation.getSerializedKey()).thenReturn("1111");
        when(reservationManager.getReservation("1111")).thenReturn(reservation);
        when(portalEventStoreHelper.createAndConfigureEventStore("1111")).thenReturn(eventStore);

        final ReservationStartedEvent startedEvent = mock(ReservationStartedEvent.class);
        when(startedEvent.getReservation()).thenReturn(reservation);
        store.onReservationStarted(startedEvent);

        final ReservationEndedEvent endedEvent = mock(ReservationEndedEvent.class);
        when(endedEvent.getReservation()).thenReturn(reservation);
        store.onReservationEnded(endedEvent);

        verify(reservationEventStore).reservationEnded(endedEvent);
    }

    @Test
    public void testIfFinishedReservationCanBeLoaded() throws Exception {
        when(reservation.getSerializedKey()).thenReturn("123");
        when(reservationManager.getReservation("123")).thenReturn(reservation);
        when(portalEventStoreHelper.createAndConfigureEventStore("123")).thenReturn(eventStore);

        final ReservationStartedEvent startedEvent = mock(ReservationStartedEvent.class);
        when(startedEvent.getReservation()).thenReturn(reservation);
        store.onReservationStarted(startedEvent);

        final ReservationEndedEvent endedEvent = mock(ReservationEndedEvent.class);
        when(endedEvent.getReservation()).thenReturn(reservation);
        store.onReservationEnded(endedEvent);

        Iterator<IEventContainer> events =  store.getEvents("123");
        verify(eventStore).getAllEvents();


    }
}
