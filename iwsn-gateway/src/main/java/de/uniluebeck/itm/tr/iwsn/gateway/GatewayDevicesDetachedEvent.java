package de.uniluebeck.itm.tr.iwsn.gateway;

import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Set;

public class GatewayDevicesDetachedEvent {

	private final Set<NodeUrn> nodeUrns;

	private final GatewayDeviceAdapter gatewayDeviceAdapter;

	public GatewayDevicesDetachedEvent(final GatewayDeviceAdapter gatewayDeviceAdapter,
									   final Set<NodeUrn> nodeUrns) {
		this.gatewayDeviceAdapter = gatewayDeviceAdapter;
		this.nodeUrns = nodeUrns;
	}

	public GatewayDeviceAdapter getGatewayDeviceAdapter() {
		return gatewayDeviceAdapter;
	}

	public Set<NodeUrn> getNodeUrns() {
		return nodeUrns;
	}
}
