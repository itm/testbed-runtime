package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.inject.internal.Nullable;
import de.uniluebeck.itm.gtr.common.Listenable;
import de.uniluebeck.itm.gtr.common.Service;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;


public interface WSNDeviceAppConnector extends Listenable<WSNDeviceAppConnector.NodeOutputListener>, Service {

	public static interface NodeOutputListener {

		void receivedPacket(MessagePacket p);
	}

	public static interface Callback {
		void success(@Nullable byte[] replyPayload);
		void failure(byte responseType, @Nullable byte[] replyPayload);
		void timeout();
	}

	public static interface FlashProgramCallback extends Callback {
		void progress(float percentage);
	}

	void enablePhysicalLink(long nodeB, Callback listener);

	void disablePhysicalLink(long nodeB, Callback listener);

	void enableNode(Callback listener);

	void disableNode(Callback listener);

	void destroyVirtualLink(long targetNode, Callback listener);

	void setVirtualLink(long targetNode, Callback listener);

	void sendMessage(byte binaryType, byte[] binaryMessage, Callback listener);

	void resetNode(Callback listener);

	void isNodeAlive(Callback listener);

	void flashProgram(WSNAppMessages.Program program, FlashProgramCallback listener);
}
