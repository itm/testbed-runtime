package de.uniluebeck.itm.tr.iwsn.gateway.events;

import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapter;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Set;

/**
 * An event that is fired on the local {@link de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus} if a device has been connected to this gateway and
 * Testbed Runtime has connected to it. This event is then forwarded to the portal. The portal will update its internal
 * status accordingly and forward it to clients if any currently connected.
 */
public class DevicesConnectedEvent {

	private final Set<NodeUrn> nodeUrns;

	private final DeviceAdapter deviceAdapter;

	public DevicesConnectedEvent(final DeviceAdapter deviceAdapter,
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
		return "DevicesConnectedEvent{" +
				"deviceAdapter=" + deviceAdapter +
				", nodeUrns=" + nodeUrns +
				'}';
	}
}
