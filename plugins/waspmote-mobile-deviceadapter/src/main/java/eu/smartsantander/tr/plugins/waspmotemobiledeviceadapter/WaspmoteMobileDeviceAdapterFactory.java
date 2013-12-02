package eu.smartsantander.tr.plugins.waspmotemobiledeviceadapter;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapter;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapterFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class WaspmoteMobileDeviceAdapterFactory implements DeviceAdapterFactory {

    private final Map<String, WaspmoteMobileDeviceAdapter> waspmoteDeviceAdapters;
    private int id = 0;

    public WaspmoteMobileDeviceAdapterFactory() {
        this.waspmoteDeviceAdapters = new HashMap<String, WaspmoteMobileDeviceAdapter>();
    }

    @Override
    public boolean canHandle(String deviceType, String devicePort, @Nullable Map<String, String> deviceConfiguration, @Nullable DeviceConfig deviceConfig) {
        if (deviceType.equals("mobile-waspmote")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public DeviceAdapter create(String deviceType, String devicePort, @Nullable Map<String, String> deviceConfiguration, @Nullable DeviceConfig deviceConfig) {
        WaspmoteMobileDeviceAdapter waspmoteDeviceAdapter = waspmoteDeviceAdapters.get(devicePort);
        if (waspmoteDeviceAdapter == null) {
            String identity = "TestbedRuntime:" + id++;
            waspmoteDeviceAdapter = new WaspmoteMobileDeviceAdapter(deviceConfig, identity);
            waspmoteDeviceAdapters.put(devicePort, waspmoteDeviceAdapter);
        }
        waspmoteDeviceAdapter.registerDevice(deviceConfig);
        return waspmoteDeviceAdapter;
    }
}
