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

package de.uniluebeck.itm.gtr.messaging.reliable;

import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnreliableMessagingService;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class AsynchronousJob {

	private final Lock lock = new ReentrantLock();

	private final UnreliableMessagingService unreliableMessagingService;

	private final Messages.Msg message;

	private final long timeout;

	private final TimeUnit timeUnit;

	private final ReliableMessagingService.AsyncCallback callback;

	private boolean done = false;

	public AsynchronousJob(final UnreliableMessagingService unreliableMessagingService, final Messages.Msg message,
						   final long timeout, final TimeUnit timeUnit,
						   final ReliableMessagingService.AsyncCallback callback) {

		this.unreliableMessagingService = unreliableMessagingService;
		this.message = message;
		this.timeout = timeout;
		this.timeUnit = timeUnit;
		this.callback = callback;
	}

	public void send() {

		// lock to be sure that a callback is invoked for both success and failure in parallel
		lock.lock();
		try {
			unreliableMessagingService.sendAsync(message);
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

				callback.success(reply.getPayload().toByteArray());
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
				callback.failure(new ReliableMessagingTimeoutException());
				done = true;
			}

		} finally {
			lock.unlock();
		}
	}

}
