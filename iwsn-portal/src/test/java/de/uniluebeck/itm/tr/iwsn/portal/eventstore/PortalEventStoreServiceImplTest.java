package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.eventstore.IEventStore;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PortalEventStoreServiceImplTest {

	private static final String SERIALIZED_KEY = "BLABLABLA";

	private static final ReservationEndedEvent RESERVATION_ENDED_EVENT = ReservationEndedEvent
			.newBuilder()
			.setSerializedKey(SERIALIZED_KEY)
			.build();

	private static final ReservationStartedEvent RESERVATION_STARTED_EVENT = ReservationStartedEvent
			.newBuilder()
			.setSerializedKey(SERIALIZED_KEY)
			.build();

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
        store = new PortalEventStoreServiceImpl(
				portalEventBus,
				reservationEventStoreFactory,
				portalEventStoreHelper,
				reservationManager
		);
    }

    @Test
    public void testIfReservationStartedEventIsPersisted() throws Exception {

        when(reservation.getSerializedKey()).thenReturn("abc");
        when(reservationManager.getReservation("abc")).thenReturn(reservation);
        when(portalEventStoreHelper.createAndConfigureEventStore("abc")).thenReturn(eventStore);
		when(reservationManager.getReservation(eq(SERIALIZED_KEY))).thenReturn(reservation);

        store.onReservationStarted(RESERVATION_STARTED_EVENT);

        verify(reservationEventStore).reservationStarted(RESERVATION_STARTED_EVENT);
    }


    @Test
    public void testIfReservationEndedEventIsPersisted() throws Exception {

        when(reservation.getSerializedKey()).thenReturn("1111");
        when(reservationManager.getReservation("1111")).thenReturn(reservation);
        when(portalEventStoreHelper.createAndConfigureEventStore("1111")).thenReturn(eventStore);

		when(reservationManager.getReservation(eq(SERIALIZED_KEY))).thenReturn(reservation);

		store.onReservationStarted(RESERVATION_STARTED_EVENT);

		when(reservationManager.getReservation(eq(SERIALIZED_KEY))).thenReturn(reservation);
        store.onReservationEnded(RESERVATION_ENDED_EVENT);

        verify(reservationEventStore).reservationEnded(RESERVATION_ENDED_EVENT);
    }

}
