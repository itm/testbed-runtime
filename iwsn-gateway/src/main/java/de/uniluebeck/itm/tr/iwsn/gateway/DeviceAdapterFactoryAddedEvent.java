package de.uniluebeck.itm.tr.iwsn.gateway;

public class DeviceAdapterFactoryAddedEvent extends DeviceAdapterFactoryEvent {

	public DeviceAdapterFactoryAddedEvent(final DeviceAdapterFactory deviceAdapterFactory,
										  final Class<? extends DeviceAdapter> deviceAdapterClass) {
		super(deviceAdapterFactory, deviceAdapterClass);
	}
}
