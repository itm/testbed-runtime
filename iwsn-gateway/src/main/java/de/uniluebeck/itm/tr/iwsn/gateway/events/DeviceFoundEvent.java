package de.uniluebeck.itm.tr.iwsn.gateway.events;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An event that is posted onto the local {@link de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus} if a device is detected at the gateway. This could
 * mean that it was either physically attached to a USB port or (in case of a wireless device) came in communication
 * range. Another component is responsible to consume this event to actually connect to it and then post a
 * {@link DevicesConnectedEvent} afterwards.
 */
public class DeviceFoundEvent {

	private final String type;

	private final String port;

	@Nullable
	private final String reference;

	@Nullable
	private final MacAddress macAddress;

	@Nullable
	private final DeviceConfig deviceConfig;

	public DeviceFoundEvent(final String type,
							final String port,
							@Nullable final String reference,
							@Nullable final MacAddress macAddress,
							@Nullable final DeviceConfig deviceConfig) {
		this.type = checkNotNull(type);
		this.port = checkNotNull(port);
		this.reference = reference;
		this.macAddress = macAddress;
		this.deviceConfig = deviceConfig;
	}

	public String getType() {
		return type;
	}

	public String getPort() {
		return port;
	}

	@Nullable
	public String getReference() {
		return reference;
	}

	@Nullable
	public MacAddress getMacAddress() {
		return macAddress;
	}

	@Nullable
	public DeviceConfig getDeviceConfig() {
		return deviceConfig;
	}

	@Override
	public String toString() {
		return "DeviceFoundEvent{" +
				"type='" + type + '\'' +
				", port='" + port + '\'' +
				", reference='" + reference + '\'' +
				", macAddress=" + macAddress +
				", deviceConfig=" + deviceConfig +
				'}';
	}
}
