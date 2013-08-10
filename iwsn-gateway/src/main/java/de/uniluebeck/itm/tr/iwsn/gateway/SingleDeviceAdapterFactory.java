package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.inject.Inject;
import de.uniluebeck.itm.nettyprotocols.HandlerFactory;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;

import java.util.Set;

public class SingleDeviceAdapterFactory implements DeviceAdapterFactory {


	private final NodeApiFactory nodeApiFactory;

	private final DeviceFactory deviceFactory;

	private final Set<HandlerFactory> handlerFactories;

	@Inject
	public SingleDeviceAdapterFactory(final DeviceFactory deviceFactory,
									  final NodeApiFactory nodeApiFactory,
									  final Set<HandlerFactory> handlerFactories) {
		this.deviceFactory = deviceFactory;
		this.nodeApiFactory = nodeApiFactory;
		this.handlerFactories = handlerFactories;
	}

	@Override
	public boolean canHandle(final DeviceConfig deviceConfig) {
		if (deviceConfig.isGatewayNode()) {
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
		return new SingleDeviceAdapter(port, deviceConfig, deviceFactory, nodeApiFactory, handlerFactories);
	}
}
