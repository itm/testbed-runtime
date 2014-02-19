package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.tr.iwsn.portal.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class PortalEventStoreTest {

    @Mock
    private PortalEventBus portalEventBus;

    @Mock
    private ReservationEventStoreFactory reservationEventStoreFactory;

    @Mock
    private ReservationManager reservationManager;

    @Mock
    private PortalServerConfig portalServerConfig;

    private PortalEventStoreHelper portalEventStoreHelper;
    private PortalEventStoreServiceImpl store;


    @Before
    public void setUp() throws Exception {
        portalEventStoreHelper = new PortalEventStoreHelperImpl(reservationManager, portalServerConfig);
        store = new PortalEventStoreServiceImpl(portalEventBus, reservationEventStoreFactory, portalEventStoreHelper);
    }

    @Test
    public void testIfReservationStartedEventIsPersisted() throws Exception {

        final Reservation reservation = mock(Reservation.class);
        when(reservation.getSerializedKey()).thenReturn("abc");
        when(portalServerConfig.getEventStorePath()).thenReturn(System.getProperty("java.io.tmpdir"));
        when(reservationManager.getReservation("abc")).thenReturn(reservation);

        store.onReservationStarted(new ReservationStartedEvent(reservation));

        final ReservationStartedEvent loaded = ((ReservationStartedEvent) store.getEvents("abc").next().getEvent());

        assertNotNull(loaded);
    }
}
