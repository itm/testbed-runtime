package de.uniluebeck.itm.tr.iwsn.portal;

import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationStartedEvent;
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

    @Before
    public void setUp() throws Exception {
        when(eventBusFactory.create(anyString())).thenReturn(eventBus);
        portalEventBus = new PortalEventBusImpl(portalServerConfig, eventBusFactory, nettyServerFactory, portalChannelHandler, schedulerService);
    }


    @Test
    public void testIfEventBusRegistersOnReservationStarted() throws Exception {

        ReservationEventBus reservationEventBus = mock(ReservationEventBus.class);
        ReservationStartedEvent event = mock(ReservationStartedEvent.class);
        Reservation reservation = mock(Reservation.class);
        when(reservation.getReservationEventBus()).thenReturn(reservationEventBus);
        when(event.getReservation()).thenReturn(reservation);

        portalEventBus.post(event);
        verify(reservationEventBus).register(portalEventBus);


    }

    @Test
    public void testIfEventBusUnregisteredOnReservationEnded() throws Exception {

        ReservationEventBus reservationEventBus = mock(ReservationEventBus.class);
        ReservationEndedEvent event = mock(ReservationEndedEvent.class);
        Reservation reservation = mock(Reservation.class);
        when(reservation.getReservationEventBus()).thenReturn(reservationEventBus);
        when(event.getReservation()).thenReturn(reservation);

        portalEventBus.post(event);
        verify(reservationEventBus).unregister(portalEventBus);


    }


}
