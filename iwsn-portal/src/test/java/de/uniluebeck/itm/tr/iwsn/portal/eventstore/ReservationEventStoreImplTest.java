package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.MessageOrBuilder;
import de.uniluebeck.itm.tr.iwsn.portal.*;
import eventstore.IEventStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReservationEventStoreImplTest {

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

    private ReservationEventStoreImpl store;

    @Before
    public void setUp() throws Exception {
        when(reservation.getSerializedKey()).thenReturn("abcd");
        when(reservation.getReservationEventBus()).thenReturn(reservationEventBus);
        when(portalServerConfig.getEventStorePath()).thenReturn(System.getProperty("java.io.tmpdir"));
        when(reservationManager.getReservation("abcd")).thenReturn(reservation);
        when(helper.createAndConfigureEventStore("abcd")).thenReturn(eventStore);

        store = new ReservationEventStoreImpl(reservation, helper);
    }


    @Test
    public void testIfBusObserversAreRegistered() throws Exception {
        final ReservationStartedEvent reservationStartedEvent = mock(ReservationStartedEvent.class);
        when(reservationStartedEvent.getReservation()).thenReturn(reservation);

        store.reservationStarted(reservationStartedEvent);
        verify(reservationEventBus).register(store);

        final ReservationEndedEvent reservationEndedEvent = mock(ReservationEndedEvent.class);
        when(reservationEndedEvent.getReservation()).thenReturn(reservation);

        store.reservationEnded(reservationEndedEvent);
        verify(reservationEventBus).unregister(store);

        verify(eventStore).close();
    }

    @Test
    public void testIfCustomEventCanBeStored() throws Exception {

        final MessageOrBuilder message = mock(MessageOrBuilder.class);
        when(message.getType()).thenReturn(Message.Type.RESPONSE);
        store.on(message);
        verify(eventStore).storeEvent(message, Message.class);

    }


}
