package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.inject.Inject;
import de.uniluebeck.itm.nettyprotocols.HandlerFactory;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class SingleDeviceAdapterFactory implements DeviceAdapterFactory {

	private final NodeApiFactory nodeApiFactory;

	private final DeviceFactory deviceFactory;

	private final Set<HandlerFactory> handlerFactories;

	private final GatewayEventBus gatewayEventBus;

	private final SchedulerService schedulerService;

	@Inject
	public SingleDeviceAdapterFactory(final DeviceFactory deviceFactory,
									  final NodeApiFactory nodeApiFactory,
									  final Set<HandlerFactory> handlerFactories,
									  final GatewayEventBus gatewayEventBus,
									  final SchedulerService schedulerService) {
		this.deviceFactory = deviceFactory;
		this.nodeApiFactory = nodeApiFactory;
		this.handlerFactories = handlerFactories;
		this.gatewayEventBus = gatewayEventBus;
		this.schedulerService = schedulerService;
	}

	@Override
	public boolean canHandle(final String nodeType,
							 final String nodePort,
							 @Nullable final Map<String, String> nodeConfiguration,
							 @Nullable final DeviceConfig deviceConfig) {

		if (deviceConfig == null) {
			return false;
		} else if (DeviceType.isKnown(nodeType) || DeviceType.isKnown(deviceConfig.getNodeType())) {
			return true;
		}

		return false;
	}

	@Override
	public DeviceAdapter create(final String deviceType,
								final String devicePort,
								@Nullable final Map<String, String> deviceConfiguration,
								@Nullable final DeviceConfig deviceConfig) {

		// allow to override detected type value in config
		final String type;
		if (deviceConfig != null && deviceConfig.getNodeType() != null) {
			type = deviceConfig.getNodeType();
		} else {
			type = deviceType;
		}

		return new SingleDeviceAdapter(
				type,
				devicePort,
				deviceConfiguration,
				gatewayEventBus,
				deviceConfig,
				deviceFactory,
				nodeApiFactory,
				handlerFactories,
				schedulerService
		);
	}
}
