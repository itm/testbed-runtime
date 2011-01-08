/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

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
