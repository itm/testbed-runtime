package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayDevice;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.WSNDeviceAppConnectorImpl;
import de.uniluebeck.itm.tr.util.ListenerManager;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WSNDeviceAppConnectorImplTest {

	private WSNDeviceAppConnectorImpl connector;

	@Mock
	private DeviceConfig deviceConfig;

	@Mock
	private DeviceFactory deviceFactory;

	@Mock
	private EventBus deviceObserverEventBus;

	@Mock
	private AsyncEventBus deviceObserverAsyncEventBus;

	@Mock
	private ListenerManager<GatewayDevice.NodeOutputListener> listenerManager;

	@Mock
	private GatewayDevice.NodeOutputListener listener;

	@Before
	public void setUp() throws Exception {

		when(deviceConfig.getNodeUrn()).thenReturn("urn:local:0x1234");
		when(deviceConfig.getTimeoutNodeApiMillis()).thenReturn(100);
		when(deviceConfig.getMaximumMessageRate()).thenReturn(Integer.MAX_VALUE);
		when(listenerManager.getListeners()).thenReturn(ImmutableList.of(listener));

		connector = new WSNDeviceAppConnectorImpl(
				deviceConfig,
				deviceFactory,
				deviceObserverEventBus,
				deviceObserverAsyncEventBus,
				listenerManager
		);
	}

	@Test
	public void testIfNoNodeAPIPacketIsReceived() throws Exception {

		final byte[] packet = {
				0x12, 0xa, 0x23
		};

		connector.onBytesReceivedFromDevice(ChannelBuffers.copiedBuffer(packet));

		assertListenerReceived(packet);
	}

	@Test
	public void testIfVirtualLinkPacketIsReceivedIfNoVLinksAreSet() throws Exception {

		final byte[] packet = {
				WSNDeviceAppConnectorImpl.MESSAGE_TYPE_WISELIB_UPSTREAM,
				WSNDeviceAppConnectorImpl.NODE_OUTPUT_VIRTUAL_LINK,
				0x12
		};

		connector.onBytesReceivedFromDevice(ChannelBuffers.copiedBuffer(packet));

		assertListenerReceived(packet);
	}

	@Test
	public void testIfNodeApiSwallowsPacketIfItAwaitsIt() throws Exception {
		// TODO implement
	}

	private void assertListenerReceived(byte... packet) {

		final ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass(byte[].class);
		verify(listener).receivedPacket(argumentCaptor.capture());

		assertEquals(packet.length, argumentCaptor.getValue().length);

		for (int i = 0; i < packet.length; i++) {
			assertEquals(packet[i], argumentCaptor.getValue()[i]);

		}
	}
}
