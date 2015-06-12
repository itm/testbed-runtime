package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;


import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.leansoft.bigqueue.IBigQueue;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

@SuppressWarnings("NullableProblems")
public class UpstreamMessageQueueImpl extends AbstractService implements UpstreamMessageQueue {

	private static Logger log = LoggerFactory.getLogger(UpstreamMessageQueueImpl.class);

	private final UpstreamMessageQueueFactory queueFactory;

	private IBigQueue queue;

	@Inject
	public UpstreamMessageQueueImpl(UpstreamMessageQueueFactory queueFactory) {
		this.queueFactory = queueFactory;
	}

	@Override
	protected void doStart() {
		log.trace("UpstreamMessageQueueImpl.doStart()");
		try {
			queue = queueFactory.create("tr-gateway-event-queue");
		} catch (IOException e) {
			log.error("Failed to create event queue!", e);
			notifyFailed(e);
			return;
		}
		notifyStarted();
	}

	@Override
	protected void doStop() {
		log.trace("UpstreamMessageQueueImpl.doStop()");
		notifyStopped();
	}

	@Override
	public Optional<Message> dequeue() {
		try {
			synchronized (queue) {
				if (queue.isEmpty()) {
					log.trace("UpstreamMessageQueueImpl.dequeue(): empty queue, returning empty Optional");
					return Optional.empty();
				}
				byte[] dequeued = queue.dequeue();
				log.trace("UpstreamMessageQueueImpl.dequeue(): dequeued {} bytes", dequeued.length);
				Message message = MessageUtils.DESERIALIZER.apply(dequeued);
				assert message != null;
				return Optional.of(message);
			}
		} catch (IOException e) {
			log.error("IOException while dequeuing message from the queue: ", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void enqueue(Message message) {
		try {
			byte[] bytes = MessageUtils.SERIALIZER.apply(message);
			synchronized (queue) {
				queue.enqueue(bytes);
			}
			log.trace("UpstreamMessageQueueImpl.enqueue(byte[]): successfully enqueued {} bytes", bytes.length);
		} catch (IOException e) {
			log.error("UpstreamMessageQueueImpl.enqueue(byte[]): IOException while enqueuing: ", e);
			throw new RuntimeException(e);
		}
	}
}
