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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.uniluebeck.itm.gtr.common.SchedulerService;
import de.uniluebeck.itm.gtr.messaging.MessageTools;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventService;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnreliableMessagingService;
import de.uniluebeck.itm.tr.util.TimedCache;
import de.uniluebeck.itm.tr.util.TimedCacheListener;
import de.uniluebeck.itm.tr.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


@Singleton
class ReliableMessagingServiceImpl implements ReliableMessagingService {

	private static final Logger log = LoggerFactory.getLogger(ReliableMessagingService.class);

	private final TimedCache<String, SynchronousJob> synchronousCache;

	private final TimedCache<String, AsynchronousJob> asynchronousCache;

	private UnreliableMessagingService unreliableMessagingService;

	private MessageEventService messageEventService;

	private final MessageEventListener messageEventListener = new MessageEventAdapter() {

		@Override
		public void messageReceived(Messages.Msg message) {

			if (message.hasReplyTo()) {

				final String messageId = message.getReplyTo();

				// check if there's a corresponding request inside the cache for synchronous messages
				final SynchronousJob synchronousJob = synchronousCache.get(messageId);
				if (synchronousJob != null) {
					log.trace("Received reply for synchronous job with message ID {}", messageId);
					synchronousJob.receivedReply(message);
					synchronousCache.remove(messageId);
					return;
				}

				// check if there's a corresponding request inside the cache for asynchronous messages
				final AsynchronousJob asynchronousJob = asynchronousCache.get(messageId);
				if (asynchronousJob != null) {
					log.trace("Received reply for asynchronous job with message ID {}", messageId);
					asynchronousJob.receivedReply(message);
					asynchronousCache.remove(messageId);
					return;
				}

				// if the message was neither in the synchronous or asynchronous cache it means it must be outdated
				log.trace("Received reply for unknown / forgotten messaging job with message ID {}", messageId);

			}
		}
	};

	private final TimedCacheListener<String, AsynchronousJob> asynchronousCacheListener =
			new TimedCacheListener<String, AsynchronousJob>() {

				@Override
				public Tuple<Long, TimeUnit> timeout(final String key, final AsynchronousJob value) {
					value.timedOut();
					return null;
				}
			};

	@Inject
	public ReliableMessagingServiceImpl(UnreliableMessagingService unreliableMessagingService,
										MessageEventService messageEventService,
										SchedulerService schedulerService) {

		this.unreliableMessagingService = unreliableMessagingService;
		this.messageEventService = messageEventService;

		this.synchronousCache = new TimedCache<String, SynchronousJob>(schedulerService);
		this.asynchronousCache = new TimedCache<String, AsynchronousJob>(schedulerService);

	}

	@Override
	public void start() throws Exception {

		log.debug("Starting overlay reliable messaging service...");
		messageEventService.addListener(messageEventListener);
		asynchronousCache.setListener(asynchronousCacheListener);

	}

	@Override
	public void stop() {

		log.debug("Stopping overlay reliable messaging service...");
		messageEventService.removeListener(messageEventListener);

	}

	@Override
	public byte[] send(final Messages.Msg message) throws ReliableMessagingTimeoutException {

		checkNotNull(message);
		checkNotNull(message.getReplyWith());

		final long now = System.currentTimeMillis();
		final long messageValidUntil = message.getValidUntil();

		checkArgument(messageValidUntil > now);

		final long timeout = message.getValidUntil() - now;

		SynchronousJob job = new SynchronousJob(unreliableMessagingService, message, timeout, TimeUnit.MILLISECONDS);
		synchronousCache.put(message.getReplyWith(), job, messageValidUntil, TimeUnit.MILLISECONDS);
		return job.run().getPayload().toByteArray();

	}

	@Override
	public byte[] send(final String from, final String to, final String app, final byte[] payload, final int priority,
					   final long validUntil)
			throws ReliableMessagingTimeoutException {

		checkNotNull(from);
		checkNotNull(to);
		checkNotNull(app);
		checkArgument(priority > 0);

		return send(MessageTools.buildReliableTransportMessage(from, to, app, payload, priority, validUntil));
	}

	@Override
	public void sendAsync(final Messages.Msg message, final AsyncCallback callback) {

		checkNotNull(message);
		checkNotNull(message.getReplyWith());

		final long now = System.currentTimeMillis();
		final long messageValidUntil = message.getValidUntil();

		checkArgument(messageValidUntil > now);

		final long timeout = message.getValidUntil() - now;

		AsynchronousJob job = new AsynchronousJob(unreliableMessagingService, message, timeout, TimeUnit.MILLISECONDS, callback);
		asynchronousCache.put(message.getReplyWith(), job, timeout, TimeUnit.MILLISECONDS);
		job.send();

	}

	@Override
	public void sendAsync(final String from, final String to, final String app, final byte[] payload,
						  final int priority, final long validUntil,
						  final AsyncCallback callback) {

		checkNotNull(from);
		checkNotNull(to);
		checkNotNull(app);
		checkArgument(priority > 0);

		sendAsync(MessageTools.buildReliableTransportMessage(from, to, app, payload, priority, validUntil), callback);

	}
}