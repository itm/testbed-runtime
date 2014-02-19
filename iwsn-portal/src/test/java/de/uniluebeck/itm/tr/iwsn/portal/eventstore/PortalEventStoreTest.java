package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
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
    private PortalServerConfig portalServerConfig;

    @Mock
    private ReservationEventStoreFactory reservationEventStoreFactory;

    @Mock
    private PortalEventStoreHelper portalEventStoreHelper;

    private PortalEventStoreServiceImpl store;


    @Before
    public void setUp() throws Exception {
        store = new PortalEventStoreServiceImpl(portalEventBus, reservationEventStoreFactory, portalEventStoreHelper);
    }

    @Test
    public void testIfReservationStartedEventIsPersisted() throws Exception {

        final Reservation reservation = mock(Reservation.class);
        when(reservation.getSerializedKey()).thenReturn("abc");

        //when(portalEventStoreHelper.createAndConfigureEventStore("abc")).thenReturn(new ChronicleBasedEventStore())

        store.onReservationStarted(new ReservationStartedEvent(reservation));

        final ReservationStartedEvent loaded = ((ReservationStartedEvent) store.getEvents("abc").next().getEvent());

        assertNotNull(loaded);
    }
}
