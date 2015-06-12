package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.EventBus;
import de.uniluebeck.itm.tr.iwsn.gateway.netty.NettyClientFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GatewayEventBusTest {

	@Mock
	private GatewayChannelHandler gatewayChannelHandler;

	@Mock
	private GatewayConfig config;

	@Mock
	private SchedulerService schedulerService;

	@Mock
	private EventBus eventBus;

	@Mock
	private NettyClientFactory nettyClientFactory;

	private GatewayEventBusImpl geb;

	@Before
	public void setUp() throws Exception {
		geb = new GatewayEventBusImpl(config, schedulerService, eventBus, nettyClientFactory, gatewayChannelHandler);
	}

	@Test
	public void testStartsGatewayChannelHandlerOnStart() throws Exception {
		geb.startAsync().awaitRunning();
		verify(gatewayChannelHandler).start();
	}

	@Test
	public void testStopsGatewayChannelHandlerOnStop() throws Exception {
		geb.startAsync().awaitRunning();
		geb.stopAsync().awaitTerminated();
		verify(gatewayChannelHandler).stop();
	}
}
