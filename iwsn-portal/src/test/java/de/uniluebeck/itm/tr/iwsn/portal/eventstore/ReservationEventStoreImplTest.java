package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.tr.iwsn.portal.*;
import de.uniluebeck.itm.eventstore.IEventStore;
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

        store = new ReservationEventStoreImpl(helper, reservation);
    }


    @Test
    public void testIfBusObserversAreRegistered() throws Exception {

        store.startAndWait();
        verify(reservationEventBus).register(store);

        store.stopAndWait();
        verify(reservationEventBus).unregister(store);

        verify(eventStore).close();
    }


}
