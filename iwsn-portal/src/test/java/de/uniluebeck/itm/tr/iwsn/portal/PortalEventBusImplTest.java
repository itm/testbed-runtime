package de.uniluebeck.itm.tr.iwsn.portal;

import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PortalEventBusImplTest {


	private static final String SERIALIZED_KEY = "BLABLABLA";

	private static final ReservationEndedEvent RESERVATION_ENDED_EVENT = ReservationEndedEvent
			.newBuilder()
			.setSerializedKey(SERIALIZED_KEY)
			.build();

	private static final ReservationStartedEvent RESERVATION_STARTED_EVENT = ReservationStartedEvent
			.newBuilder()
			.setSerializedKey(SERIALIZED_KEY)
			.build();

	private PortalEventBusImpl portalEventBus;

	@Mock
	private SchedulerService schedulerService;

	@Mock
	private PortalChannelHandler portalChannelHandler;

	@Mock
	private NettyServerFactory nettyServerFactory;

	@Mock
	private EventBusFactory eventBusFactory;

	@Mock
	private PortalServerConfig portalServerConfig;

	@Mock
	private com.google.common.eventbus.EventBus eventBus;

	@Mock
	private ReservationManager reservationManager;

	@Before
	public void setUp() throws Exception {
		when(eventBusFactory.create(anyString())).thenReturn(eventBus);
		portalEventBus = new PortalEventBusImpl(
				portalServerConfig,
				eventBusFactory,
				nettyServerFactory,
				portalChannelHandler,
				schedulerService,
				reservationManager
		);
	}

	@Test
	public void testIfEventBusRegistersOnReservationStarted() throws Exception {

		ReservationEventBus reservationEventBus = mock(ReservationEventBus.class);
		Reservation reservation = mock(Reservation.class);

		when(reservationManager.getReservation(eq(SERIALIZED_KEY))).thenReturn(reservation);
		when(reservation.getEventBus()).thenReturn(reservationEventBus);

		portalEventBus.post(RESERVATION_STARTED_EVENT);

		verify(reservationEventBus).register(portalEventBus);
	}

	@Test
	public void testIfEventBusUnregisteredOnReservationEnded() throws Exception {

		ReservationEventBus reservationEventBus = mock(ReservationEventBus.class);
		Reservation reservation = mock(Reservation.class);

		when(reservationManager.getReservation(SERIALIZED_KEY)).thenReturn(reservation);
		when(reservation.getEventBus()).thenReturn(reservationEventBus);

		portalEventBus.post(RESERVATION_ENDED_EVENT);

		verify(reservationEventBus).unregister(portalEventBus);
	}
}
