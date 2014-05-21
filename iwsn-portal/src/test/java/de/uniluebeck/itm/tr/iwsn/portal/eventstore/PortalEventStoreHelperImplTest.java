package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.eventstore.IEventStore;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
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
	private PortalServerConfig config;

	@Before
	public void setUp() throws Exception {
		helper = new PortalEventStoreHelperImpl(config);
		when(config.getEventStorePath())
				.thenReturn(System.getProperty("java.io.tmpdir") + "/portal_event_store_helper_test");
	}

	@Test
	public void testIfCreationIsSuccessful() throws Exception {
		IEventStore store = helper.createAndConfigureEventStore("abc");
		assertNotNull(store);
	}
}
