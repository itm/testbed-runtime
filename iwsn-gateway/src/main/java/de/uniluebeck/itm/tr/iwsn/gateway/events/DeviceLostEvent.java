package de.uniluebeck.itm.tr.iwsn.gateway.events;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.annotation.Nullable;

/**
 * An event that is posted onto the local {@link de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus} if a device is physically disconnected from the
 * gateway. Components that previously have been connected to this device are responsible to consume this event and
 * then post a {@link DevicesDisconnectedEvent} afterwards.
 */
public class DeviceLostEvent {

	@Nullable
	private final String port;

	private final NodeUrn nodeUrn;

	public DeviceLostEvent(@Nullable final String port, final NodeUrn nodeUrn) {
		this.port = port;
		this.nodeUrn = nodeUrn;
	}

	@Nullable
	public String getPort() {
		return port;
	}

	public NodeUrn getNodeUrn() {
		return nodeUrn;
	}
}
