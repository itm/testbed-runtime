package de.uniluebeck.itm.tr.iwsn.gateway;

import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Set;

public class DevicesDetachedEvent {

	private final Set<NodeUrn> nodeUrns;

	private final DeviceAdapter deviceAdapter;

	public DevicesDetachedEvent(final DeviceAdapter deviceAdapter,
								final Set<NodeUrn> nodeUrns) {
		this.deviceAdapter = deviceAdapter;
		this.nodeUrns = nodeUrns;
	}

	public DeviceAdapter getDeviceAdapter() {
		return deviceAdapter;
	}

	public Set<NodeUrn> getNodeUrns() {
		return nodeUrns;
	}

	@Override
	public String toString() {
		return "DevicesDetachedEvent{" +
				"deviceAdapter=" + deviceAdapter +
				", nodeUrns=" + nodeUrns +
				'}';
	}
}
