package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class DeviceAdapterRegistryImpl implements DeviceAdapterRegistry {

	private static final Logger log = LoggerFactory.getLogger(DeviceAdapterRegistry.class);

	private final Set<DeviceAdapterFactory> deviceAdapterFactories = newHashSet();

	private final GatewayEventBus gatewayEventBus;

	@Inject
	public DeviceAdapterRegistryImpl(final GatewayEventBus gatewayEventBus) {
		this.gatewayEventBus = gatewayEventBus;
	}

	@Override
	public Set<DeviceAdapterFactory> getDeviceAdapterFactories() {

		log.trace("DeviceAdapterRegistry.getDeviceAdapterFactories()");

		synchronized (deviceAdapterFactories) {
			return newHashSet(deviceAdapterFactories);
		}
	}

	@Override
	public void addDeviceAdapterFactory(final DeviceAdapterFactory deviceAdapterFactory, final Class<? extends DeviceAdapter> deviceAdapterClass) {

		log.trace("DeviceAdapterRegistry.addDeviceAdapterFactory({}, {})", deviceAdapterFactory, deviceAdapterClass);

		synchronized (deviceAdapterFactories) {
			deviceAdapterFactories.add(deviceAdapterFactory);
		}

		gatewayEventBus.post(new DeviceAdapterFactoryAddedEvent(deviceAdapterFactory, deviceAdapterClass));
	}

	@Override
	public void removeDeviceAdapterFactory(final DeviceAdapterFactory deviceAdapterFactory, final Class<? extends DeviceAdapter> deviceAdapterClass) {

		log.trace("DeviceAdapterRegistry.removeDeviceAdapterFactory({}, {})", deviceAdapterFactory, deviceAdapterClass);

		synchronized (deviceAdapterFactories) {
			deviceAdapterFactories.remove(deviceAdapterFactory);
		}

		gatewayEventBus.post(new DeviceAdapterFactoryRemovedEvent(deviceAdapterFactory, deviceAdapterClass));
	}
}
