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

package de.uniluebeck.itm.tr.iwsn.overlay.messaging.reliable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.Messages;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.unreliable.UnreliableMessagingService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class AsynchronousJob {

	private final Lock lock = new ReentrantLock();

	private final UnreliableMessagingService unreliableMessagingService;

	private final Messages.Msg message;

	private final SettableFuture<byte[]> future;

	private boolean done = false;

	public AsynchronousJob(final UnreliableMessagingService unreliableMessagingService, final Messages.Msg message,
						   final SettableFuture<byte[]> future) {

		this.unreliableMessagingService = unreliableMessagingService;
		this.message = message;
		this.future = future;
	}

	public void send() {

		// lock to be sure that a callback is invoked for both success and failure in parallel
		lock.lock();
		try {

			final ListenableFuture<Void> sendFuture = unreliableMessagingService.sendAsync(message);
			sendFuture.addListener(new Runnable() {
				@Override
				public void run() {
					try {
						sendFuture.get();
					} catch (Exception e) {
						failed(e);
					}
				}
			}, MoreExecutors.sameThreadExecutor());

		} finally {
			lock.unlock();
		}
	}

	public void receivedReply(Messages.Msg reply) {

		// lock to be sure that a callback is invoked for both success and failure in parallel
		lock.lock();
		try {

			// if we're not done it means that the callback wasn't informed of either the reply or a timeout
			if (!done) {

				future.set(reply.getPayload().toByteArray());
				done = true;
			}

		} finally {
			lock.unlock();
		}
	}

	public void timedOut() {

		lock.lock();
		try {

			// send the message of enough and there was no reply until now, so pass the callback an exception
			if (!done) {
				future.setException(new ReliableMessagingTimeoutException());
				done = true;
			}

		} finally {
			lock.unlock();
		}
	}

	public void failed(Exception reason) {

		lock.lock();
		try {

			future.setException(reason);
			done = true;

		} finally {
			lock.unlock();
		}
	}

}
