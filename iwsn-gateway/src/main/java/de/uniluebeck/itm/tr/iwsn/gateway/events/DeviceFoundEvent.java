package de.uniluebeck.itm.tr.iwsn.gateway.events;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * An event that is posted onto the local {@link de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus} if a device is
 * detected at the gateway. This could
 * mean that it was either physically attached to a USB port or (in case of a wireless device) came in communication
 * range. Another component is responsible to consume this event to actually connect to it and then post a
 * {@link DevicesConnectedEvent} afterwards.
 */
public class DeviceFoundEvent extends DeviceEvent {

	public DeviceFoundEvent(final String deviceType,
							final String devicePort,
							@Nullable final Map<String, String> deviceConfiguration,
							@Nullable final String reference,
							@Nullable final MacAddress macAddress,
							@Nullable final DeviceConfig deviceConfig) {
		super(deviceType, devicePort, deviceConfiguration, reference, macAddress, deviceConfig);
	}
}
