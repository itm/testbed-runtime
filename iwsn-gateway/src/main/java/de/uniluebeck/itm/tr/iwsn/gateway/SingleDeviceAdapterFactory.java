package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;

public class SingleDeviceAdapterFactory implements DeviceAdapterFactory {

	private final GatewayEventBus gatewayEventBus;

	private final NodeApiFactory nodeApiFactory;

	private final DeviceFactory deviceFactory;

	@Inject
	public SingleDeviceAdapterFactory(final DeviceFactory deviceFactory,
									  final GatewayEventBus gatewayEventBus,
									  final NodeApiFactory nodeApiFactory) {
		this.deviceFactory = deviceFactory;
		this.gatewayEventBus = gatewayEventBus;
		this.nodeApiFactory = nodeApiFactory;
	}

	@Override
	public boolean canHandle(final DeviceConfig deviceConfig) {
		if (!deviceConfig.isGatewayNode()) {
			return false;
		}
		try {
			DeviceType.fromString(deviceConfig.getNodeType());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public DeviceAdapter create(final String port, final DeviceConfig deviceConfig) {
		return new SingleDeviceAdapter(port, deviceConfig, deviceFactory, nodeApiFactory, gatewayEventBus);
	}
}
