package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;


import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.inject.Inject;
import com.leansoft.bigqueue.IBigQueue;
import de.uniluebeck.itm.tr.iwsn.common.MessageWrapper;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.GatewayFailureEvent;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.serialization.MultiClassSerializationHelper;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.nio.channels.ClosedChannelException;
import java.util.Optional;
import java.util.concurrent.CancellationException;

import static com.google.common.util.concurrent.Futures.addCallback;

@SuppressWarnings("NullableProblems")
public class UpstreamMessageQueueImpl extends AbstractService implements UpstreamMessageQueue {

	private static Logger log = LoggerFactory.getLogger(UpstreamMessageQueueImpl.class);

	private final Object channelLock = new Object();
	private final UpstreamMessageQueueHelper queueHelper;
	private final GatewayEventBus gatewayEventBus;
	private final SchedulerService schedulerService;
	private final MessageFactory messageFactory;

	private Channel channel;
	private IBigQueue queue;
	private MultiClassSerializationHelper<Message> serializationHelper;

	@Inject
	public UpstreamMessageQueueImpl(final UpstreamMessageQueueHelper queueHelper,
									final GatewayEventBus gatewayEventBus,
									final SchedulerService schedulerService,
									final MessageFactory messageFactory) {
		this.queueHelper = queueHelper;
		this.gatewayEventBus = gatewayEventBus;
		this.schedulerService = schedulerService;
		this.messageFactory = messageFactory;
	}

	@Override
	protected void doStart() {

		log.trace("UpstreamMessageQueueImpl.doStart()");

		try {
			queue = queueHelper.createAndConfigureQueue();
		} catch (IOException e) {
			log.error("Failed to create event queue!", e);
			notifyFailed(e);
			return;
		}

		try {
			serializationHelper = queueHelper.configureEventSerializationHelper();
		} catch (Exception e) {
			log.error("Failed to configure serialization helper!", e);
			notifyFailed(e);
			return;
		}

		gatewayEventBus.register(this);
		notifyStarted();
	}

	@Override
	protected void doStop() {
		log.trace("UpstreamMessageQueueImpl.doStop()");
		gatewayEventBus.unregister(this);
		notifyStopped();
	}

	@Override
	public void channelConnected(final Channel channel) {
		log.trace("UpstreamMessageQueueImpl.channelConnected()");
		synchronized (channelLock) {
			this.channel = channel;
			addCallback(queue.dequeueAsync(), new DequeueCallback(), schedulerService);
		}
		log.trace("UpstreamMessageQueueImpl.channelConnected(): dequeue future callback added");
	}

	@Override
	public void channelDisconnected() {
		log.trace("UpstreamMessageQueueImpl.channelDisconnected()");
		synchronized (channelLock) {
			this.channel = null;
		}
	}

	@Override
	public void enqueue(MessageHeaderPair pair) {
		enqueue(MessageWrapper.WRAP_FUNCTION.apply(pair.message));
	}

	@Override
	public void enqueue(Message msg) {
		try {
			enqueue(serializationHelper.serialize(msg));
		} catch (NotSerializableException e) {
			log.error("NotSerializableException while trying to serialize. Exception: {}", e);
			sendFailureEvent("The message " + msg + " is not serializable. An appropriate serializer is missing!", e);
		}
	}

	private void enqueue(byte[] serializedMessage) {

		try {

			log.trace("UpstreamMessageQueueImpl.enqueue(byte[]): enqueuing {} bytes", serializedMessage.length);
			queue.enqueue(serializedMessage);
			log.trace("UpstreamMessageQueueImpl.enqueue(byte[]): successfully enqueued {} bytes", serializedMessage.length);

		} catch (IOException e) {

			log.error("UpstreamMessageQueueImpl.enqueue(byte[]): IOException while enqueuing: ", e);
			sendFailureEvent("Failed to enqueue message in upstream message queue.", e);
		}
	}

	private void sendFailureEvent(String message, @Nullable Throwable cause) {
		gatewayEventBus.post(new GatewayFailureEvent(message, cause));
	}

	private boolean isConnected() {
		return channel != null;
	}

	@Subscribe
	public void onEvent(Object obj) {

		log.trace("UpstreamMessageQueueImpl.onEvent(): {}", obj);

		if (obj instanceof DevicesConnectedEvent) {

			DevicesConnectedEvent event = (DevicesConnectedEvent) obj;
			log.info("Enqueuing DevicesAttachedEvent for {}", event.getNodeUrns());
			final DevicesAttachedEvent dae = messageFactory.devicesAttachedEvent(Optional.empty(), event.getNodeUrns());
			enqueue(MessageWrapper.wrap(dae));

		} else if (obj instanceof DevicesDisconnectedEvent) {

			DevicesDisconnectedEvent event = (DevicesDisconnectedEvent) obj;
			log.info("Enqueuing DevicesDetachedEvent for {}", event.getNodeUrns());
			DevicesDetachedEvent dde = messageFactory.devicesDetachedEvent(Optional.empty(), event.getNodeUrns());
			enqueue(MessageWrapper.wrap(dde));
		}

		if (MessageHeaderPair.isUnwrappedMessageEvent(obj)) {

			final MessageHeaderPair pair = MessageHeaderPair.fromUnwrapped(obj);

			if (pair.header.getUpstream()) {
				enqueue(pair);
			} else if (pair.header.getDownstream()) {
				gatewayEventBus.post(pair.message);
			}
		}
	}

	private class DequeueCallback implements FutureCallback<byte[]> {
		@SuppressWarnings("ConstantConditions")
		@Override
		public void onSuccess(final byte[] dequeuedBytes) {

			assert serializationHelper != null; // otherwise this service should not be running

			synchronized (channelLock) {

				if (!isConnected()) {

					log.trace("UpstreamMessageQueueImpl.dequeueCallback.onSuccess(): currently not connected; re-enqueuing.");
					enqueue(dequeuedBytes);

				} else {

					final Message message;
					try {
						message = serializationHelper.deserialize(dequeuedBytes);
					} catch (IllegalArgumentException e) {
						log.error("IllegalArgumentException while trying to deserialize message. Exception: {}", e);
						sendFailureEvent("A message is not deserializable. An appropriate deserializer is missing", e);
						// Ignore this message and continue with other messages in order to prevent the communication chain from blocking.
						addCallback(queue.dequeueAsync(), new DequeueCallback(), schedulerService);
						return;
					}

					MessageHeaderPair pair = MessageHeaderPair.fromWrapped(message);
					log.trace("Forwarding message to portal server: {}", pair.message);

					ChannelFutureListener listener = future -> {

						if (future.isSuccess()) {

							log.trace("Successfully forwarded message to portal, asynchronously dequeuing next message.");
							addCallback(queue.dequeueAsync(), new DequeueCallback(), schedulerService);

						} else if (!UpstreamMessageQueueImpl.this.isRunning()) {

							log.trace("UpstreamMessageQueueImpl.dequeueCallback: service is shutting down, ignoring failed deque operation and re-enqueuing it.");
							enqueue(dequeuedBytes);

						} else {

							log.error("UpstreamMessageQueueImpl.dequeueCallback: error forwarding message, re-enqueuing it and closing channel. Cause: ", future.getCause());

							if (future.getChannel() != null) {
								future.getChannel().close();
							}

							enqueue(dequeuedBytes);
						}
					};

					try {

						Channels.write(channel, pair).addListener(listener);

					} catch (Exception e) {

						if (e.getCause() instanceof ClosedChannelException) {

							log.trace("ClosedChannelException while trying to forward message to portal server. Re-enqueuing message. Reason: {}", e.getMessage());
							channelDisconnected();
						}

						try {

							log.error("Exception forwarding message to portal server. Closing channel. Cause: {}", e.getMessage());
							Channels.close(channel);

						} catch (Exception e1) {

							log.error("Exception closing channel, faking close event. Cause: {}", e);
							channelDisconnected();
						}
					}
				}

			}
		}

		@Override
		public void onFailure(Throwable t) {
			if (!(t instanceof CancellationException)) {
				log.error("Dequeue future failed with throwable", t);
			} else {
				log.info("Dequeue future was canceled");
			}
		}
	}
}
