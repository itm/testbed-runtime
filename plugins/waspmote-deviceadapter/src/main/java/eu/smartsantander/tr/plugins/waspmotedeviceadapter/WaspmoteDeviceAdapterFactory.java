package eu.smartsantander.tr.plugins.waspmotedeviceadapter;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapter;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class WaspmoteDeviceAdapterFactory implements DeviceAdapterFactory {
    private static final Logger log = LoggerFactory.getLogger(WaspmoteDeviceAdapterFactory.class);

    private final Map<String, WaspmoteDeviceAdapter> waspmoteDeviceAdapters;
    private int id = 0;

    public WaspmoteDeviceAdapterFactory() {
        this.waspmoteDeviceAdapters = new HashMap<String, WaspmoteDeviceAdapter>();
    }

    @Override
    public boolean canHandle(String deviceType, String devicePort, @Nullable Map<String, String> deviceConfiguration, @Nullable DeviceConfig deviceConfig) {
        if (deviceType.equals("waspmote")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized DeviceAdapter create(String deviceType, String devicePort, @Nullable Map<String, String> deviceConfiguration, @Nullable DeviceConfig deviceConfig) {
        log.trace("WaspmoteDeviceAdapterFactory.create()");
        WaspmoteDeviceAdapter waspmoteDeviceAdapter = waspmoteDeviceAdapters.get(devicePort);
        if (waspmoteDeviceAdapter == null) {
            String identity = "TestbedRuntime:" + id++;
            waspmoteDeviceAdapter = new WaspmoteDeviceAdapter(deviceConfig, identity);
            waspmoteDeviceAdapters.put(devicePort, waspmoteDeviceAdapter);
        }
        waspmoteDeviceAdapter.registerDevice(deviceConfig);
        return waspmoteDeviceAdapter;
    }
}
