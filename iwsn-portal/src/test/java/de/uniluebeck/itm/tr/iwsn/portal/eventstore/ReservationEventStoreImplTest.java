package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.IEventContainer;
import de.uniluebeck.itm.eventstore.IEventStore;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Iterator;

import static junit.framework.Assert.*;
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
    private IEventStore eventStore;

    @Mock
    private ReservationEventBus reservationEventBus;

    @Mock
    private CloseableIterator<IEventContainer> storeIterator;

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
    public void testEventIteratorNotReturnsReservationEndedEventBeforeActualEnd() throws Exception {
        Interval reservationInterval = new Interval(DateTime.now().minusHours(2), DateTime.now().plusHours(1));
        when(reservation.getInterval()).thenReturn(reservationInterval);

        Iterator<IEventContainer> iterator = reservationEventStore.getAllEvents();
        verify(eventStore).getAllEvents();
        assertTrue(iterator.hasNext());
        IEventContainer container = iterator.next();
        assertNotNull(container);
        assertTrue(container.getEvent() instanceof ReservationStartedEvent);
        assertEquals(KEY, ((ReservationStartedEvent) container.getEvent()).getSerializedKey());

        assertFalse(iterator.hasNext());

    }

    @Test
    public void testEventIteratorReturnsReservationEndedEventAfterActualEnd() throws Exception {
        Interval reservationInterval = new Interval(DateTime.now().minusHours(2), DateTime.now().minusMinutes(10));
        when(reservation.getInterval()).thenReturn(reservationInterval);

        Iterator<IEventContainer> iterator = reservationEventStore.getAllEvents();
        verify(eventStore).getAllEvents();

        testNextEventAvailableAndIsReservationStartedEvent(iterator);
        testNextEventAvailableAndIsReservationEndedEvent(iterator);

    }

    @Test
    public void testEventIteratorDidNotReturnsReservationEndedEventIfOutsideRange() throws Exception {
        Interval reservationInterval = new Interval(DateTime.now().minusHours(2), DateTime.now().minusMinutes(10));
        when(reservation.getInterval()).thenReturn(reservationInterval);
        long from = reservationInterval.getStartMillis();
        long to = reservationInterval.getEnd().minusMinutes(10).getMillis();

        Iterator<IEventContainer> iterator = reservationEventStore.getEventsBetween(from, to);
        verify(eventStore).getEventsBetweenTimestamps(from, to);

        testNextEventAvailableAndIsReservationStartedEvent(iterator);

        assertFalse(iterator.hasNext());

    }

    @Test
    public void testReservationStartedEventNotGeneratedIfOutsideRange() throws Exception {
        Interval reservationInterval = new Interval(DateTime.now().minusHours(1), DateTime.now().minusMinutes(10));
        when(reservation.getInterval()).thenReturn(reservationInterval);
        long from = reservationInterval.getStart().plusMinutes(10).getMillis();
        long to = reservationInterval.getEnd().minusMinutes(10).getMillis();

        Iterator<IEventContainer> iterator = reservationEventStore.getEventsBetween(from, to);
        verify(eventStore).getEventsBetweenTimestamps(from, to);
        assertFalse(iterator.hasNext());
    }

    private void testNextEventAvailableAndIsReservationStartedEvent(Iterator<IEventContainer> iterator) {
        assertTrue(iterator.hasNext());
        IEventContainer container = iterator.next();
        assertNotNull(container);
        assertTrue(container.getEvent() instanceof ReservationStartedEvent);
        assertEquals(KEY, ((ReservationStartedEvent) container.getEvent()).getSerializedKey());
    }

    private void testNextEventAvailableAndIsReservationEndedEvent(Iterator<IEventContainer> iterator) {
        assertTrue(iterator.hasNext());
        IEventContainer container = iterator.next();
        assertNotNull(container);
        assertTrue(container.getEvent() instanceof ReservationEndedEvent);
        assertEquals(KEY, ((ReservationEndedEvent) container.getEvent()).getSerializedKey());
    }


}
