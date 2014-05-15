package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.eventstore.IEventStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PortalEventStoreHelperImplTest {

    private PortalEventStoreHelperImpl helper;

    @Mock
    private ReservationManager reservationManager;

    @Mock
    private PortalServerConfig config;

    @Mock
    private Reservation reservation;

    @Before
    public void setUp() throws Exception {
        helper = new PortalEventStoreHelperImpl(reservationManager, config);
        when(config.getEventStorePath()).thenReturn(System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void testIfCreationIsSuccessful() throws Exception {
        when(reservationManager.getReservation("abc")).thenReturn(reservation);
        when(reservation.getSerializedKey()).thenReturn("abc");

        IEventStore store = helper.createAndConfigureEventStore("abc");
        assertNotNull(store);
    }


}
