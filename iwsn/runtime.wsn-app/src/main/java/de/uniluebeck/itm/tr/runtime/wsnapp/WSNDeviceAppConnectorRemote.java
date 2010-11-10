package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.gtr.common.AbstractListenable;
import de.uniluebeck.itm.tr.nodeapi.NodeApiCallback;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;


public class WSNDeviceAppConnectorRemote extends AbstractListenable<WSNDeviceAppConnector.NodeOutputListener>
		implements WSNDeviceAppConnector {

	private String nodeUrn;

	private String nodeType;

	private Integer nodeAPITimeout;

	public WSNDeviceAppConnectorRemote(final String nodeUrn, final String nodeType, final Integer nodeAPITimeout) {
		this.nodeUrn = nodeUrn;
		this.nodeType = nodeType;
		this.nodeAPITimeout = nodeAPITimeout;
	}

	@Override
	public void enablePhysicalLink(final long nodeB, final NodeApiCallback listener) {
		// TODO implement
	}

	@Override
	public void disablePhysicalLink(final long nodeB, final NodeApiCallback listener) {
		// TODO implement
	}

	@Override
	public void enableNode(final NodeApiCallback listener) {
		// TODO implement
	}

	@Override
	public void disableNode(final NodeApiCallback listener) {
		// TODO implement
	}

	@Override
	public void destroyVirtualLink(final long targetNode, final NodeApiCallback listener) {
		// TODO implement
	}

	@Override
	public void setVirtualLink(final long targetNode, final NodeApiCallback listener) {
		// TODO implement
	}

	@Override
	public void sendMessage(final byte binaryType, final byte[] binaryMessage, final NodeApiCallback listener) {
		// TODO implement
	}

	@Override
	public void resetNode(final NodeApiCallback listener) {
		// TODO implement
	}

	@Override
	public void isNodeAlive(final NodeApiCallback listener) {
		// TODO implement
	}

	@Override
	public void flashProgram(final WSNAppMessages.Program program, final FlashProgramCallback listener) {
		// TODO implement
	}

	private void notifyListener(MessagePacket p) {
		for (NodeOutputListener listener : listeners) {
			listener.receivedPacket(p);
		}
	}

	@Override
	public void start() throws Exception {
		// TODO implement
	}

	@Override
	public void stop() {
		// TODO implement
	}
}
