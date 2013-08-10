package de.uniluebeck.itm.tr.iwsn.gateway.events;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An event that is posted onto the local {@link de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus} if a device is physically disconnected from the
 * gateway. Components that previously have been connected to this device are responsible to consume this event and
 * then post a {@link DevicesDisconnectedEvent} afterwards.
 */
public class DeviceLostEvent {

	private final String type;

	private final String port;

	@Nullable
	private final String reference;

	@Nullable
	private final MacAddress macAddress;

	@Nullable
	private final DeviceConfig deviceConfig;

	public DeviceLostEvent(final String type,
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
		return "DeviceLostEvent{" +
				"type='" + type + '\'' +
				", port='" + port + '\'' +
				", reference='" + reference + '\'' +
				", macAddress=" + macAddress +
				", deviceConfig=" + deviceConfig +
				'}';
	}
}
