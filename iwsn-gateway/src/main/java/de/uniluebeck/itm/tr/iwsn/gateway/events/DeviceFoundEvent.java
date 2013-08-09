package de.uniluebeck.itm.tr.iwsn.gateway.events;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An event that is posted onto the local {@link de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus} if a device is detected at the gateway. This could
 * mean that it was either physically attached to a USB port or (in case of a wireless device) came in communication
 * range. Another component is responsible to consume this event to actually connect to it and then post a
 * {@link DevicesConnectedEvent} afterwards.
 */
public class DeviceFoundEvent {

	@Nullable
	private final String port;

	private final DeviceConfig deviceConfig;

	public DeviceFoundEvent(@Nullable final String port, final DeviceConfig deviceConfig) {
		this.port = port;
		this.deviceConfig = checkNotNull(deviceConfig);
	}

	public DeviceConfig getDeviceConfig() {
		return deviceConfig;
	}

	@Nullable
	public String getPort() {
		return port;
	}

	@Override
	public String toString() {
		return "DeviceFoundEvent{" +
				"port='" + port + '\'' +
				", deviceConfig=" + deviceConfig +
				"} " + super.toString();
	}
}
