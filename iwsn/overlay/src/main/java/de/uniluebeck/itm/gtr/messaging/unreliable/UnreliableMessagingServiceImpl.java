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

package de.uniluebeck.itm.gtr.messaging.unreliable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.gtr.LocalNodeNameManager;
import de.uniluebeck.itm.gtr.connection.Connection;
import de.uniluebeck.itm.gtr.connection.ConnectionInvalidAddressException;
import de.uniluebeck.itm.gtr.connection.ConnectionService;
import de.uniluebeck.itm.gtr.connection.ConnectionTypeUnavailableException;
import de.uniluebeck.itm.gtr.messaging.MessageTools;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.cache.MessageCache;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventService;
import de.uniluebeck.itm.gtr.routing.RoutingTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;


@Singleton
class UnreliableMessagingServiceImpl implements UnreliableMessagingService {

	private static final Logger log = LoggerFactory.getLogger(UnreliableMessagingService.class);

	public static final Comparator<UnreliableMessagingCacheEntry> MESSAGE_CACHE_COMPARATOR =
			new Comparator<UnreliableMessagingCacheEntry>() {

				public int compare(UnreliableMessagingCacheEntry e1, UnreliableMessagingCacheEntry e2) {

					// 1st comparison: priority
					if (e1.msg.getPriority() != e2.msg.getPriority()) {
						return e1.msg.getPriority() < e2.msg.getPriority() ? -1 : 1;
					}

					// 2nd comparison: age
					if (e1.timestamp != e2.timestamp) {
						return e1.timestamp < e2.timestamp ? -1 : 1;
					}

					// 3rd comparison: validUntil
					return e1.msg.getValidUntil() == e2.msg.getValidUntil() ? 0
							: e1.msg.getValidUntil() < e2.msg.getValidUntil() ? -1 : 1;

				}

			};

	private ConnectionService connectionService;

	private RoutingTableService routingTableService;

	private final LocalNodeNameManager localNodeNameManager;

	private MessageEventService messageEventService;

	private Runnable dequeuingRunnable = new Runnable() {

		@Override
		public void run() {

			while (!Thread.currentThread().isInterrupted()) {

				try {

					UnreliableMessagingCacheEntry messagingCacheEntry = messageCache.deq();
					DispatcherRunnable dispatcherRunnable = new DispatcherRunnable(messagingCacheEntry);
					dispatcherThreads.execute(dispatcherRunnable);

				} catch (InterruptedException e) {
					// ignore as this should only happen when shutting down
				}

			}
		}
	};

	private Thread dequeuingThread = new Thread(dequeuingRunnable, "UnreliableMessagingService-DequeuingThread");

	private ExecutorService dispatcherThreads = Executors.newFixedThreadPool(1,
			new ThreadFactoryBuilder().setNameFormat("UnreliableMessagingService-DispatcherThread %d").build()
	);

	/**
	 * Runnable that is used by the dispatcher threads. A dispatcher thread
	 * takes a message from the message queue and tries to send it to its
	 * recipient over a connection retrieved from the connection service.
	 */
	private class DispatcherRunnable implements Runnable {

		private UnreliableMessagingCacheEntry messageCacheEntry;

		private DispatcherRunnable(UnreliableMessagingCacheEntry messageCacheEntry) {
			this.messageCacheEntry = messageCacheEntry;
		}

		private void dispatchMessages() {

			long now = System.currentTimeMillis();

			if (messageCacheEntry != null) {

				final Messages.Msg msg = messageCacheEntry.msg;

				if (msg.getValidUntil() >= now) {

					// try to get a connection
					Connection connection = getConnection(msg);

					// try to send the message, fails silently if one of the
					// arguments is null. this results in a drop of the message.
					final String to = msg.getTo();

					if (connection != null) {

						try {

							log.trace("Writing message to connection output stream ({})", connection.getOutputStream());
							sendMessage(msg, connection);
							log.trace("Wrote message to connection output stream ({})", connection.getOutputStream());
							messageCacheEntry.future.set(null);
							messageEventService.sent(msg);

						} catch (IOException e) {

							log.trace("IOException when trying to send message to {}. Trying to reconnect...", to);

							connection.disconnect();

							// in case the remote host got e.g. restarted it may be that the current connection is
							// broken. give it a second try here by creating a new connection and only assume sending
							// failed if this doesn't work now
							connection = getConnection(msg);

							if (connection == null) {
								String warningMsg = "No connection to " + to + ". Dropping message " + msg;
								log.warn(warningMsg);
								messageCacheEntry.future.setException(new RuntimeException(warningMsg));
								messageEventService.dropped(msg);
								return;
							}

							try {

								sendMessage(msg, connection);
								messageCacheEntry.future.set(null);
								messageEventService.sent(msg);

							} catch (Exception e1) {

								String warningMsg =
										"Can't send message to " + to + " because the attempt threw an exception: " + e1;
								log.warn(warningMsg);
								messageCacheEntry.future.setException(new RuntimeException(warningMsg));
								messageEventService.dropped(msg);
							}

						} catch (Exception e) {

							String warningMsg =
									"Exception while serializing message to " + to + ". Dropping message: " + msg;
							log.warn(warningMsg);
							messageCacheEntry.future.setException(new RuntimeException(warningMsg));
							messageEventService.dropped(msg);
						}

					} else {

						String warningMsg = "No connection to " + to + ". Dropping message " + msg;
						log.warn(warningMsg);
						messageCacheEntry.future.setException(new RuntimeException(warningMsg));
						messageEventService.dropped(msg);
					}

				} else {
					messageCacheEntry.future.setException(new RuntimeException("Message validity timed out!"));
					messageEventService.dropped(msg);
				}

			}

		}

		/**
		 * Returns a connection to the next hop of the messages' recipient address.
		 *
		 * @param msg
		 * 		the message containing the recipients' address
		 *
		 * @return a {@link Connection} object for the message {@code msg} or
		 *         {@code null} if no connection can be established
		 */
		@Nullable
		private Connection getConnection(Messages.Msg msg) {

			try {

				return connectionService.getConnection(msg.getTo());

			} catch (ConnectionInvalidAddressException e1) {
				log.warn("Invalid address: {}. Dropping message: {}. Cause: {}",
						new Object[]{e1.getAddress(), msg, e1}
				);
			} catch (ConnectionTypeUnavailableException e1) {
				return null;
			} catch (IOException e1) {
				log.warn("IOException while creating connection to: {}. Dropping message: {}. Cause: {}",
						new Object[]{msg.getTo(), msg, e1}
				);
			}

			return null;

		}

		public void run() {
			dispatchMessages();
		}

		/**
		 * Sends the message {@code msg} over the connection {@code connection}
		 * if both are not {@code null}. Otherwise nothing is done.
		 *
		 * @param msg
		 * 		the message to be sent
		 * @param connection
		 * 		the connection the message shall be sent over
		 *
		 * @return {@code true} if the msg has been sent or {@code false} if not
		 */
		private void sendMessage(@Nonnull Messages.Msg msg, @Nonnull Connection connection) throws Exception {

			checkNotNull(msg);
			checkNotNull(connection);

			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (connection) {
				MessageTools.sendMessage(msg, connection.getOutputStream());
			}
		}

	}

	/**
	 * A reference to the message cache. It is used to process messages
	 * asynchronously. The calling thread of
	 * {@link UnreliableMessagingService#sendAsync(String, String, String, java.io.Serializable, int, long)}
	 * returns immediately after placing the message to be send into the message
	 * cache. The dispatcher threads will later on pick up messages from the
	 * cache and send them to the appropriate recipients.
	 */
	private MessageCache<UnreliableMessagingCacheEntry> messageCache;

	public ListenableFuture<Void> sendAsync(Messages.Msg message) {

		// assure that message priority contains a valid value
		if (message.getPriority() < 0 || message.getPriority() > 2) {
			throw new IllegalArgumentException("Invalid priority. Priority must be one of 0, 1, 2.");
		}

		// ensure that lifetime of the message is restricted to the maximum
		// which is allowed
		// (defined by maxValidity)
		long maxValidity = UnreliableMessagingService.DEFAULT_MAX_VALIDITY;

		// check if the messages lifetime's exceeded
		long maxValidUntil = System.currentTimeMillis() + maxValidity;

		if (message.getValidUntil() > maxValidUntil) {
			message = Messages.Msg.newBuilder(message).setValidUntil(maxValidUntil).build();
		}

		// if it's for this local node we can deliver it directly through message eventing
		if (localNodeNameManager.getLocalNodeNames().contains(message.getTo())) {
			messageEventService.received(message);
			final SettableFuture<Void> future = SettableFuture.create();
			future.set(null);
			return future;
		}

		// check if name is known, otherwise discard
		if (routingTableService.getNextHop(message.getTo()) == null) {
			final SettableFuture<Void> future = SettableFuture.create();
			future.setException(new UnknownNameException(message.getTo()));
			return future;
		}

		final SettableFuture<Void> future = SettableFuture.create();

		// otherwise put it into the message queue for asynchronous delivery
		UnreliableMessagingCacheEntry entry = new UnreliableMessagingCacheEntry(
				future,
				message,
				System.currentTimeMillis()
		);

		this.messageCache.enq(entry);

		return future;
	}

	public ListenableFuture<Void> sendAsync(String from, String to, String msgType, Serializable msg,
											int priority, long validUntil) {

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {

			ObjectOutputStream objectOutputStream = new ObjectOutputStream(out);
			objectOutputStream.writeObject(msg);
			objectOutputStream.close();

		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}

		return sendAsync(from, to, msgType, out.toByteArray(), priority, validUntil);

	}

	@Override
	public ListenableFuture<Void> sendAsync(String from, String to, String msgType, byte[] payload, int priority,
											long validUntil) {

		Messages.Msg.Builder builder = Messages.Msg.newBuilder()
				.setFrom(from)
				.setTo(to)
				.setMsgType(msgType)
				.setPriority(priority)
				.setValidUntil(validUntil);

		builder.setPayload(ByteString.copyFrom(payload));

		return sendAsync(builder.build());
	}

	@Inject
	public UnreliableMessagingServiceImpl(ConnectionService connectionService,
										  MessageEventService messageEventService,
										  @Unreliable MessageCache<UnreliableMessagingCacheEntry> messageCache,
										  final RoutingTableService routingTableService,
										  LocalNodeNameManager localNodeNameManager) {

		this.connectionService = connectionService;
		this.messageEventService = messageEventService;
		this.messageCache = messageCache;
		this.routingTableService = routingTableService;
		this.localNodeNameManager = localNodeNameManager;
	}

	@Override
	public void start() throws Exception {

		if (log.isTraceEnabled()) {
			messageEventService.addListener(new MessageEventListener() {
				@Override
				public void messageSent(final Messages.Msg msg) {
					log.trace("Message sent: {}", msg);
				}

				@Override
				public void messageDropped(final Messages.Msg msg) {
					log.trace("Message dropped: {}", msg);
				}

				@Override
				public void messageReceived(final Messages.Msg msg) {
					log.trace("Message received: {}", msg);
				}
			}
			);
		}

		dequeuingThread.start();
	}

	@Override
	public void stop() {
		dequeuingThread.interrupt();
	}

}