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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.uniluebeck.itm.gtr.messaging.MessageTools;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventService;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnreliableMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Singleton
class ReliableMessagingServiceImpl implements ReliableMessagingService {

	private static final Logger log = LoggerFactory.getLogger(ReliableMessagingService.class);

	private abstract class ReliableMessagingJob implements Runnable {

		protected final SyncMapEntry syncMapEntry;

		protected ReliableMessagingJob(SyncMapEntry syncMapEntry) {
			this.syncMapEntry = syncMapEntry;
		}

	}

	private class WakeUpJob extends ReliableMessagingJob {

		public WakeUpJob(SyncMapEntry syncMapEntry) {
			super(syncMapEntry);
		}

		@Override
		public void run() {

			log.debug("ReliableMessagingServiceImpl$WakeUpJob.run");

			synchronized (syncMapEntry) {

				if (!syncMapEntry.done) {

					// depending on whether this is the reply of an asynchronous invocation or to a synchronous
					// invocation there's a different sync object in the map and we'll have to act accordingly

					boolean asyncJob = syncMapEntry.syncObj instanceof AsyncCallback;
					boolean reply = syncMapEntry.reply != null;

					if (asyncJob) {

						log.trace("*** Invoking asynchronous callback for message ID: {}", syncMapEntry.message.getReplyWith());
						if (reply) {
							((AsyncCallback) syncMapEntry.syncObj).success(syncMapEntry.reply.getPayload().toByteArray());
						} else {
							//noinspection ThrowableInstanceNeverThrown
							((AsyncCallback) syncMapEntry.syncObj).failure(
									new ReliableMessagingTimeoutException("No reply was received in time!"));
						}

					} else {

						log.trace("*** Notifying sleeping thread for message ID: {}", syncMapEntry.message.getReplyWith());
						// wake up thread
						syncMapEntry.awakeOnReply();

					}

					syncMapEntry.done = true;

				}
			}
		}
	}

	private class SendJob extends ReliableMessagingJob {

		public SendJob(SyncMapEntry syncMapEntry) {
			super(syncMapEntry);
		}

		@Override
		public void run() {
			log.debug("ReliableMessagingServiceImpl$SendJob.run");
			ReliableMessagingServiceImpl.this.unreliableMessagingService.sendAsync(syncMapEntry.message);
		}

	}

	private static class SyncMapEntry {

		public final List<Future> futures;

		public final Messages.Msg message;

		public Messages.Msg reply;

		public final Object syncObj;

		public boolean done = false;

		public SyncMapEntry(Messages.Msg message, Object syncObj) {
			this.message = message;
			this.futures = new LinkedList<Future>();
			this.syncObj = syncObj;
		}

		public synchronized void waitForReply() {
			log.debug("ReliableMessagingServiceImpl$SynchronousJob.waitForReply");
			try {
				this.wait();
			} catch (InterruptedException e) {
				log.error("This should not occur", e);
			}
		}

		public synchronized void awakeOnReply() {
			log.debug("ReliableMessagingServiceImpl$SynchronousJob.awakeOnReply");
			this.notify();
		}

	}

	private ScheduledExecutorService scheduler;

	private UnreliableMessagingService unreliableMessagingService;

	private MessageEventService messageEventService;

	private Map<String, SyncMapEntry> syncMap = Collections.synchronizedMap(new HashMap<String, SyncMapEntry>());

	private final MessageEventListener messageEventListener = new MessageEventAdapter() {
		@Override
		public void messageReceived(Messages.Msg msg) {
			if (msg.hasReplyTo()) {
				receivedReply(msg);
			}
		}
	};

	private void receivedReply(Messages.Msg reply) {

		String messageId = reply.getReplyTo();
		log.trace("*** Received reply for message ID: {}", messageId);

		// check if there's a corresponding request inside the syncMap
		final SyncMapEntry syncMapEntry = syncMap.get(messageId);

		if (syncMapEntry == null) {
			log.warn("Received reply to unknown message ID ({}). Ignoring...", messageId);
			return;
		}

		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (syncMapEntry) {

			if (!syncMapEntry.done) {

				// put reply into reply map so that awoken or asynchronous callback thread can get them
				syncMapEntry.reply = reply;

				// cancel all outstanding operations (in case there are any left)
				for (Future future : syncMapEntry.futures) {
					if (!future.isCancelled()) {
						future.cancel(true);
					}
				}

				scheduler.execute(new WakeUpJob(syncMapEntry));

			}

		}
	}

	@Inject
	public ReliableMessagingServiceImpl(UnreliableMessagingService unreliableMessagingService,
										MessageEventService messageEventService) {

		this.unreliableMessagingService = unreliableMessagingService;
		this.messageEventService = messageEventService;
	}

	private byte[] sendInternal(Messages.Msg message) throws ReliableMessagingTimeoutException {

		SyncMapEntry syncMapEntry = createSyncMapEntryAndScheduleJobs(message, new Object());

		// let caller thread sleep on the syncObj of the syncMapEntry
		syncMapEntry.waitForReply();

		// if we're here it means that the caller thread was woken up and is the current thread executing this code
		log.trace("*** Woke up caller thread of message ID: {}", message.getReplyWith());

		// if there's no reply inside the reply map it means no asynchronous reply was received so we'll throw a
		// timeout exception
		if (syncMapEntry.reply == null) {
			log.trace("*** Timeout for message ID: {}", message.getReplyWith());
			throw new ReliableMessagingTimeoutException("No reply for message received");
		}

		return syncMapEntry.reply.getPayload().toByteArray();
	}

	@Override
	public byte[] send(Messages.Msg message) throws ReliableMessagingTimeoutException {

		// TODO check preconditions

		return sendInternal(message);
	}

	@Override
	public byte[] send(final String from, final String to, final String app, final byte[] payload, final int priority,
					   final long validUntil)
			throws ReliableMessagingTimeoutException {

		// TODO check preconditions

		return sendInternal(MessageTools.buildReliableTransportMessage(from, to, app, payload, priority, validUntil));
	}

	@Override
	public void sendAsync(Messages.Msg message, AsyncCallback callback) {

		// TODO check preconditions

		sendAsyncInternal(message, callback);
	}

	private void sendAsyncInternal(Messages.Msg message, AsyncCallback callback) {
		createSyncMapEntryAndScheduleJobs(message, callback);
	}

	private SyncMapEntry createSyncMapEntryAndScheduleJobs(Messages.Msg message, Object syncObj) {

		final String messageId = message.getReplyWith();

		final SyncMapEntry mapEntry = new SyncMapEntry(message, syncObj);

		// remember all the Future objects we created for this message so we can cancel them if the reply or use the
		// wake up runnable to wake up the callers thread
		final long lifetime = message.getValidUntil() - System.currentTimeMillis();

		mapEntry.futures.add(scheduler.schedule(new SendJob(mapEntry), 0 * (lifetime / 3), TimeUnit.MILLISECONDS));
		mapEntry.futures.add(scheduler.schedule(new SendJob(mapEntry), 1 * (lifetime / 3), TimeUnit.MILLISECONDS));
		mapEntry.futures.add(scheduler.schedule(new SendJob(mapEntry), 2 * (lifetime / 3), TimeUnit.MILLISECONDS));
		mapEntry.futures.add(scheduler.schedule(new WakeUpJob(mapEntry), 3 * (lifetime / 3), TimeUnit.MILLISECONDS));

		syncMap.put(messageId, mapEntry);

		return mapEntry;

	}

	@Override
	public void sendAsync(String from, String to, String app, byte[] payload, int priority, long validUntil, AsyncCallback callback) {

		// TODO check preconditions

		sendAsyncInternal(MessageTools.buildReliableTransportMessage(from, to, app, payload, priority, validUntil), callback);
	}

	@Override
	public void start() throws Exception {

		log.debug("ReliableMessagingServiceImpl.start");
		scheduler = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("ReliableMessagingService-Thread %d").build());
		messageEventService.addListener(messageEventListener);

	}

	@Override
	public void stop() {

		log.debug("ReliableMessagingServiceImpl.stop");

		messageEventService.removeListener(messageEventListener);

		// wake up all sleeping threads with exceptions
		scheduler.shutdown();

		try {
			scheduler.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// silently catch
		}

		List<Runnable> failedJobs = scheduler.shutdownNow();
		for (Runnable job : failedJobs) {

			// if this service shuts down while some of the caller threads are either sleeping or waiting for an
			// asynchronous result we should still awake or invoke them...
			if (job instanceof WakeUpJob) {
				job.run();
			}

		}
	}
}
