package de.uniluebeck.itm.tr.runtime.wsndeviceobserver;

import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceRequest {


    private final DeviceType deviceType;

    @Nullable
    private final MacAddress macAddress;

    @Nullable
    private final String reference;
    
    private DeviceInfo response;

    public DeviceRequest(final DeviceType deviceType,
                         @Nullable final MacAddress macAddress,
                         @Nullable final String reference) {

        checkNotNull(deviceType);

        this.deviceType = deviceType;
        this.macAddress = macAddress;
        this.reference = reference;
    }

    public DeviceInfo getResponse() {
        return response;
    }

    public void setResponse(final DeviceInfo response) {
        this.response = response;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    @Nullable
    public MacAddress getMacAddress() {
        return macAddress;
    }

    @Nullable
    public String getReference() {
        return reference;
    }
}
