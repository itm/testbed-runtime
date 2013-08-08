package de.uniluebeck.itm.tr.iwsn.gateway;

import java.util.Set;

public interface DeviceAdapterRegistry {

	Set<DeviceAdapterFactory> getDeviceAdapterFactories();

	void addDeviceAdapterFactory(DeviceAdapterFactory deviceAdapterFactory, Class<? extends DeviceAdapter> deviceAdapterClass);

	void removeDeviceAdapterFactory(DeviceAdapterFactory deviceAdapterFactory, Class<? extends DeviceAdapter> deviceAdapterClass);

}
