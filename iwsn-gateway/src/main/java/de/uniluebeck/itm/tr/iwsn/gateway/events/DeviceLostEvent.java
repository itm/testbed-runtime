package de.uniluebeck.itm.tr.iwsn.gateway.events;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * An event that is posted onto the local {@link de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus} if a device is physically disconnected from the
 * gateway. Components that previously have been connected to this device are responsible to consume this event and
 * then post a {@link DevicesDisconnectedEvent} afterwards.
 */
public class DeviceLostEvent extends DeviceEvent {

	public DeviceLostEvent(final String deviceType,
						   final String devicePort,
						   @Nullable final Map<String, String> deviceConfiguration,
						   @Nullable final String reference,
						   @Nullable final MacAddress macAddress,
						   @Nullable final DeviceConfig deviceConfig) {
		super(deviceType, devicePort, deviceConfiguration, reference, macAddress, deviceConfig);
	}
}
