package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Joiner;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceFoundEvent;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import eu.wisebed.api.v3.common.NodeUrn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeviceManagerImplTest {

    private static final NodeUrn NODE_URN_1 = new NodeUrn("urn:unit:test:0x1");
    private static final NodeUrn NODE_URN_2 = new NodeUrn("urn:unit:test:0x2");

    private static final String NODE_1_PORT = "/dev/ttyUSB1";
    private static final String NODE_2_PORT = "/dev/ttyUSB2";

    public static final String DEVICE_1_TYPE = "isense39";
    public static final String DEVICE_2_TYPE = "isense39";

    private static final DeviceConfig DEVICE_CONFIG_1 = new DeviceConfig(
            NODE_URN_1, DEVICE_1_TYPE, false, NODE_1_PORT, null, null, null, null, null, null, null, null, null, null
    );
    private static final DeviceConfig DEVICE_CONFIG_2 = new DeviceConfig(
            NODE_URN_2, DEVICE_2_TYPE, false, NODE_2_PORT, null, null, null, null, null, null, null, null, null, null
    );
    public static final DeviceFoundEvent DEVICE_FOUND_EVENT_1 = new DeviceFoundEvent(
            DEVICE_1_TYPE, NODE_1_PORT, DEVICE_CONFIG_1.getNodeConfiguration(), DEVICE_CONFIG_1.getNodeUSBChipID(),
            new MacAddress(NODE_URN_1.getSuffix()), DEVICE_CONFIG_1
    );
    public static final DeviceFoundEvent DEVICE_FOUND_EVENT_2 = new DeviceFoundEvent(
            DEVICE_2_TYPE, NODE_2_PORT, DEVICE_CONFIG_2.getNodeConfiguration(), DEVICE_CONFIG_2.getNodeUSBChipID(),
            new MacAddress(NODE_URN_2.getSuffix()), DEVICE_CONFIG_2
    );

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private GatewayEventBus gatewayEventBus;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private Set<DeviceAdapterFactory> builtInDeviceAdapterFactories;

    @Mock
    private DeviceAdapterRegistry deviceAdapterRegistry;

    @Mock
    private DeviceDBService deviceDBService;

    @Mock
    private Deque<DeviceFoundEvent> deviceFoundEvents;

    @Mock
    private Iterator<DeviceFoundEvent> deviceFoundEventsIterator;

    private DeviceManagerImpl deviceManager;

    @Before
    public void setUp() throws Exception {
        deviceManager = new DeviceManagerImpl(
                gatewayConfig,
                gatewayEventBus,
                schedulerService,
                builtInDeviceAdapterFactories,
                deviceAdapterRegistry,
                deviceDBService,
                deviceFoundEvents
        );
    }

    @Test
    public void testIfStaticDevicesWillBeScheduledForConnectionIfConfigAvailable() throws Exception {

        when(gatewayConfig.getStaticDevices()).thenReturn(Joiner.on(",").join(NODE_URN_1.toString(), NODE_URN_2.toString()));
        when(deviceDBService.getConfigByNodeUrn(eq(NODE_URN_1))).thenReturn(DEVICE_CONFIG_1);
        when(deviceDBService.getConfigByNodeUrn(eq(NODE_URN_2))).thenReturn(DEVICE_CONFIG_2);
        when(deviceFoundEvents.iterator()).thenReturn(deviceFoundEventsIterator);
        when(deviceFoundEventsIterator.hasNext()).thenReturn(false);

        deviceManager.startAsync().awaitRunning();

        verify(deviceManager.deviceFoundEvents).add(eq(DEVICE_FOUND_EVENT_1));
        verify(deviceManager.deviceFoundEvents).add(eq(DEVICE_FOUND_EVENT_2));
    }

    @Test
    public void testIfStaticDevicesWillBeScheduledForConnectionOnSecondAttemptIfConfigWasNotAvailableOnFirst() throws Exception {

        when(gatewayConfig.getStaticDevices()).thenReturn(Joiner.on(",").join(NODE_URN_1.toString(), NODE_URN_2.toString()));
        when(deviceDBService.getConfigByNodeUrn(eq(NODE_URN_1))).thenReturn(null);
        when(deviceDBService.getConfigByNodeUrn(eq(NODE_URN_2))).thenReturn(null);
        when(deviceFoundEvents.iterator()).thenReturn(deviceFoundEventsIterator);
        when(deviceFoundEventsIterator.hasNext()).thenReturn(false);
        when(deviceFoundEvents.isEmpty()).thenReturn(true);

        deviceManager.startAsync().awaitRunning();

        when(deviceDBService.getConfigByNodeUrn(eq(NODE_URN_1))).thenReturn(DEVICE_CONFIG_1);
        when(deviceDBService.getConfigByNodeUrn(eq(NODE_URN_2))).thenReturn(DEVICE_CONFIG_2);

        // this is executed periodically in production code
        deviceManager.tryToConnectToUnconnectedDevicesRunnable.run();

        verify(deviceManager.deviceFoundEvents).add(eq(DEVICE_FOUND_EVENT_1));
        verify(deviceManager.deviceFoundEvents).add(eq(DEVICE_FOUND_EVENT_2));
    }
}
