package de.uniluebeck.itm.tr.iwsn.gateway;

public abstract class DeviceAdapterFactoryEvent {

	protected final DeviceAdapterFactory deviceAdapterFactory;

	protected final Class<? extends DeviceAdapter> deviceAdapterClass;

	protected DeviceAdapterFactoryEvent(final DeviceAdapterFactory deviceAdapterFactory,
										final Class<? extends DeviceAdapter> deviceAdapterClass) {
		this.deviceAdapterFactory = deviceAdapterFactory;
		this.deviceAdapterClass = deviceAdapterClass;
	}

	public DeviceAdapterFactory getDeviceAdapterFactory() {
		return deviceAdapterFactory;
	}

	public Class<? extends DeviceAdapter> getDeviceAdapterClass() {
		return deviceAdapterClass;
	}
}
