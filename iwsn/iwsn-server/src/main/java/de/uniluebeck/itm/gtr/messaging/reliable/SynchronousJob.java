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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class SynchronousJob {

	private static final Logger log = LoggerFactory.getLogger(SynchronousJob.class);

	private final Lock lock = new ReentrantLock();

	private final Condition receivedReply = lock.newCondition();

	private final UnreliableMessagingService unreliableMessagingService;

	private final Messages.Msg message;

	private final long timeout;

	private final TimeUnit timeUnit;

	private Messages.Msg reply = null;

	public SynchronousJob(final UnreliableMessagingService unreliableMessagingService, final Messages.Msg message,
						  final long timeout, final TimeUnit timeUnit) {

		this.unreliableMessagingService = unreliableMessagingService;
		this.message = message;
		this.timeout = timeout;
		this.timeUnit = timeUnit;
	}

	public Messages.Msg run() throws ReliableMessagingTimeoutException {

		lock.lock();

		try {

			unreliableMessagingService.sendAsync(message);
			receivedReply.await(timeout, timeUnit);

			if (reply == null) {
				throw new ReliableMessagingTimeoutException("No reply was received in time!");
			}

		} catch (InterruptedException e) {
			log.error("" + e, e);
			throw new ReliableMessagingException(e);
		} finally {
			lock.unlock();
		}

		return reply;

	}

	public void receivedReply(Messages.Msg reply) {
		this.reply = reply;
		lock.lock();
		try {
			receivedReply.signal();
		} finally {
			lock.unlock();
		}
	}

}
