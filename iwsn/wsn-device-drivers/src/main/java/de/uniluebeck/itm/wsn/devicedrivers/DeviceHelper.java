package de.uniluebeck.itm.wsn.devicedrivers;


import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ValueFuture;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;

import java.nio.ByteBuffer;

public class DeviceHelper {

	public static ListenableFuture<Long> getMacAddress(iSenseDevice device) {
		final ValueFuture<Long> future = ValueFuture.create();
		device.registerListener(new iSenseDeviceListener() {
			@Override
			public void receivePacket(final MessagePacket messagePacket) {
				// ignore
			}

			@Override
			public void receivePlainText(final MessagePlainText messagePlainText) {
				// ignore
			}

			@Override
			public void operationCanceled(final Operation operation) {
				if (operation == Operation.READ_MAC) {
					future.setException(new RuntimeException("Operation READ_MAC was cancelled"));
				}
			}

			@Override
			public void operationDone(final Operation operation, final Object o) {
				if (operation == Operation.READ_MAC && o instanceof Exception) {
					future.setException((Exception) o);
				} else if (operation == Operation.READ_MAC && o instanceof MacAddress) {
					ByteBuffer buffer = ByteBuffer.allocate(8).put(((MacAddress) o).getMacBytes());
					future.set(buffer.getLong(0));
				}
			}

			@Override
			public void operationProgress(final Operation operation, final float v) {
				// ignore
			}
		}
		);
		try {
			device.triggerGetMacAddress(false);
		} catch (Exception e) {
			future.setException(e);
		}
		return future;
	}

}
