package de.uniluebeck.itm.tr.iwsn.gateway;

public class DeviceAdapterFactoryRemovedEvent extends DeviceAdapterFactoryEvent {

	public DeviceAdapterFactoryRemovedEvent(final DeviceAdapterFactory deviceAdapterFactory,
											final Class<? extends DeviceAdapter> deviceAdapterClass) {
		super(deviceAdapterFactory, deviceAdapterClass);
	}
}
