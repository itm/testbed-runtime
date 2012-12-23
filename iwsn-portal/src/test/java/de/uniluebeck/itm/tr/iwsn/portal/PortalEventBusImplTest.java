package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.EventBus;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PortalEventBusImplTest {

	@Mock
	private PortalConfig config;

	@Mock
	private EventBus eventBus;

	@Mock
	private NettyServerFactory nettyServerFactory;

	@Mock
	private PortalChannelHandler portalChannelHandler;

	private PortalEventBusImpl portalEventBus;

	@Before
	public void setUp() throws Exception {
		portalEventBus = new PortalEventBusImpl(config, eventBus, nettyServerFactory, portalChannelHandler);
		portalEventBus.startAndWait();
	}

	@After
	public void tearDown() throws Exception {
		portalEventBus.stopAndWait();
	}
}
