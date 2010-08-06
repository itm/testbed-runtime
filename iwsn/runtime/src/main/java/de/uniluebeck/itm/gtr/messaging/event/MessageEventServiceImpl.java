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

package de.uniluebeck.itm.gtr.messaging.event;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Singleton;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

@Singleton
class MessageEventServiceImpl extends MessageEventService {

	private static final Logger log = LoggerFactory.getLogger(MessageEventService.class);

	private ExecutorService executorService;

	private class SentRunnable implements Runnable {

		private Messages.Msg msg;

		private SentRunnable(Messages.Msg msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			for (MessageEventListener listener : listeners) {
				listener.messageSent(msg);
			}
		}

	}

	private class DroppedRunnable implements Runnable {

		private Messages.Msg msg;

		private DroppedRunnable(Messages.Msg msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			for (MessageEventListener listener : listeners) {
				listener.messageDropped(msg);
			}
		}

	}

	private class ReceivedRunnable implements Runnable {

		private Messages.Msg msg;

		private ReceivedRunnable(Messages.Msg msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			for (MessageEventListener listener : listeners) {
				listener.messageReceived(msg);
			}
		}

	}

	@Override
	public void sent(Messages.Msg msg) {
		if (executorService != null && !executorService.isShutdown()) {
			executorService.execute(new SentRunnable(msg));
		} else {
			log.warn("Not delivering message since service is shut down. {}", msg);
		}
	}

	@Override
	public void dropped(Messages.Msg msg) {
		if (executorService != null && !executorService.isShutdown()) {
			executorService.execute(new DroppedRunnable(msg));
		} else {
			log.warn("Not delivering message since service is shut down. {}", msg);
		}
	}

	@Override
	public void received(Messages.Msg msg) {
		if (executorService != null && !executorService.isShutdown()) {
			executorService.execute(new ReceivedRunnable(msg));
		} else {
			log.warn("Not delivering message since service is shut down. {}", msg);
		}
	}

	@Override
	public void start() throws Exception {
		executorService = new ThreadPoolExecutor(
				1,
				3,
				60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(),
				new ThreadFactoryBuilder().setNameFormat("MessageEventService-Thread %d").build()
		);
	}

	@Override
	public void stop() {

		ExecutorUtils.shutdown(executorService, 1, TimeUnit.SECONDS);

	}
}
