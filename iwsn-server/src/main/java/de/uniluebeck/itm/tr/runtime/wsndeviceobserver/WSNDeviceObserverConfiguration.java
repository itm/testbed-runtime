package de.uniluebeck.itm.tr.runtime.wsndeviceobserver;

import de.uniluebeck.itm.wsn.deviceutils.macreader.DeviceMacReferenceMap;

import java.util.Map;

public class WSNDeviceObserverConfiguration {

	private final DeviceMacReferenceMap deviceMacReferenceMap;

	private final Map<String, String> properties;

	public WSNDeviceObserverConfiguration(final DeviceMacReferenceMap deviceMacReferenceMap,
										  final Map<String, String> properties) {
		this.deviceMacReferenceMap = deviceMacReferenceMap;
		this.properties = properties;
	}

	public DeviceMacReferenceMap getDeviceMacReferenceMap() {
		return deviceMacReferenceMap;
	}

	public Map<String, String> getProperties() {
		return properties;
	}
}
