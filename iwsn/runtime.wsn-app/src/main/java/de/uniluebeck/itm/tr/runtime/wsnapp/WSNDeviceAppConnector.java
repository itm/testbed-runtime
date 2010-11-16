package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.gtr.common.Listenable;
import de.uniluebeck.itm.gtr.common.Service;
import de.uniluebeck.itm.tr.nodeapi.NodeApiCallback;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;


public interface WSNDeviceAppConnector extends Listenable<WSNDeviceAppConnector.NodeOutputListener>, Service {

	public static interface NodeOutputListener {

		void receivedPacket(MessagePacket p);
	}

	public static interface FlashProgramCallback extends NodeApiCallback {

		void progress(float percentage);
	}

	void enablePhysicalLink(long nodeB, NodeApiCallback listener);

	void disablePhysicalLink(long nodeB, NodeApiCallback listener);

	void enableNode(NodeApiCallback listener);

	void disableNode(NodeApiCallback listener);

	void destroyVirtualLink(long targetNode, NodeApiCallback listener);

	void setVirtualLink(long targetNode, NodeApiCallback listener);

	void sendMessage(byte binaryType, byte[] binaryMessage, NodeApiCallback listener);

	void resetNode(NodeApiCallback listener);

	void isNodeAlive(NodeApiCallback listener);

	void flashProgram(WSNAppMessages.Program program, FlashProgramCallback listener);
}
