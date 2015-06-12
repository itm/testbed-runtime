package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.eventstore.EventStore;
import de.uniluebeck.itm.tr.common.IncrementalIdProvider;
import de.uniluebeck.itm.tr.common.UnixTimestampProvider;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactory;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactoryImpl;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.adminui.EventStoreAdminService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PortalEventStoreImplTest {

	private static final MessageFactory MESSAGE_FACTORY = new MessageFactoryImpl(
			new IncrementalIdProvider(),
			new UnixTimestampProvider()
	);

	private static final String SERIALIZED_KEY = "BLABLABLA";

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

	@Mock
	private EventStoreAdminService eventStoreAdminService;

	@Mock
	private PortalEventBus portalEventBus;

	private PortalEventStoreImpl store;

	@Before
	public void setUp() throws Exception {

		when(eventStoreAdminService.startAsync()).thenReturn(eventStoreAdminService);
		when(portalServerConfig.getEventStorePath()).thenReturn(System.getProperty("java.io.tmpdir"));
		when(reservationEventStoreFactory.createOrLoad(reservation)).thenReturn(reservationEventStore);

		store = new PortalEventStoreImpl(
				portalEventBus,
				reservationManager,
				portalEventStoreHelper,
				portalServerConfig,
				eventStoreAdminService
		);
	}

	@Test
	public void testTodo() throws Exception {

	}
}
