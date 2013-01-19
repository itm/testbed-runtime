package de.uniluebeck.itm.tr.iwsn.gateway;

import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Set;

public class DevicesAttachedEvent {

	private final Set<NodeUrn> nodeUrns;

	private final DeviceAdapter deviceAdapter;

	public DevicesAttachedEvent(final DeviceAdapter deviceAdapter,
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
}
