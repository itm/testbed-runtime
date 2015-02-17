package de.uniluebeck.itm.tr.iwsn.gateway.events;

import com.google.common.base.Objects;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An event that is posted onto the local {@link de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus} if a device is
 * detected at the gateway. This could
 * mean that it was either physically attached to a USB port or (in case of a wireless device) came in communication
 * range. Another component is responsible to consume this event to actually connect to it and then post a
 * {@link de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent} afterwards.
 */
public abstract class DeviceEvent {

	protected final String deviceType;

	protected final String devicePort;

	@Nullable
	protected Map<String, String> deviceConfiguration;

	@Nullable
	protected String reference;

	@Nullable
	protected MacAddress macAddress;

	@Nullable
	protected DeviceConfig deviceConfig;

	public DeviceEvent(final String deviceType,
					   final String devicePort,
					   @Nullable final Map<String, String> deviceConfiguration,
					   @Nullable final String reference,
					   @Nullable final MacAddress macAddress,
					   @Nullable final DeviceConfig deviceConfig) {
		this.deviceType = checkNotNull(deviceType);
		this.devicePort = checkNotNull(devicePort);
		this.deviceConfiguration = deviceConfiguration;
		this.reference = reference;
		this.macAddress = macAddress;
		this.deviceConfig = deviceConfig;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public String getDevicePort() {
		return devicePort;
	}

	@Nullable
	public Map<String, String> getDeviceConfiguration() {
		return deviceConfiguration;
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
		return getClass().getSimpleName() + "{" +
				"deviceType='" + deviceType + '\'' +
				", devicePort='" + devicePort + '\'' +
				", deviceConfiguration=" + deviceConfiguration +
				", macAddress=" + macAddress +
				", reference='" + reference + '\'' +
				", deviceConfig=" + deviceConfig +
				'}';
	}

	public void setDeviceConfig(@Nullable final DeviceConfig deviceConfig) {
		this.deviceConfig = deviceConfig;
	}

	public void setDeviceConfiguration(@Nullable final Map<String, String> deviceConfiguration) {
		this.deviceConfiguration = deviceConfiguration;
	}

	public void setMacAddress(@Nullable final MacAddress macAddress) {
		this.macAddress = macAddress;
	}

	public void setReference(@Nullable final String reference) {
		this.reference = reference;
	}

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceType, devicePort, deviceConfiguration, reference, macAddress, deviceConfig);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DeviceEvent other = (DeviceEvent) obj;
        return Objects.equal(this.deviceType, other.deviceType)
                && Objects.equal(this.devicePort, other.devicePort)
                && Objects.equal(this.deviceConfiguration, other.deviceConfiguration)
                && Objects.equal(this.reference, other.reference)
                && Objects.equal(this.macAddress, other.macAddress)
                && Objects.equal(this.deviceConfig, other.deviceConfig);
    }
}
