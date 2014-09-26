package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.eventstore.EventStore;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PortalEventStoreImplTest {

    private static final String SERIALIZED_KEY = "BLABLABLA";

    private static final ReservationEndedEvent RESERVATION_ENDED_EVENT = ReservationEndedEvent
            .newBuilder()
            .setSerializedKey(SERIALIZED_KEY)
            .setTimestamp(DateTime.now().getMillis())
            .build();

    private static final ReservationStartedEvent RESERVATION_STARTED_EVENT = ReservationStartedEvent
            .newBuilder()
            .setSerializedKey(SERIALIZED_KEY)
            .setTimestamp(DateTime.now().getMillis())
            .build();


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
    private EventStore eventStore;

    private PortalEventStoreImpl store;


    @Before
    public void setUp() throws Exception {

        when(portalServerConfig.getEventStorePath()).thenReturn(System.getProperty("java.io.tmpdir"));
        when(reservationEventStoreFactory.createOrLoad(reservation)).thenReturn(reservationEventStore);

        store = new PortalEventStoreImpl(portalEventStoreHelper, portalServerConfig);
    }

    @Test
    public void testTodo() throws Exception {

    }
}
