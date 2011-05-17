package eu.wisebed.testbed.api.wsn.deliverymanager;


import com.google.common.collect.Lists;
import de.uniluebeck.itm.tr.util.TimeDiff;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;

class DeliveryWorker implements Runnable {

	private final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);

	private static final int MAXIMUM_NOTIFICATION_LIST_SIZE = 20;

	private static final int MAXIMUM_REQUEST_STATUS_LIST_SIZE = 20;

	private static final int MAXIMUM_MESSAGE_LIST_SIZE = 20;

	private final DeliveryManager deliveryManager;

	/**
	 * The endpoint URL to deliver the messages to.
	 */
	private final String endpointUrl;

	/**
	 * The endpoint to deliver the messages to.
	 */
	private final Controller endpoint;

	/**
	 * The queue that contains all {@link Message} objects to be delivered to {@link DeliveryWorker#endpoint}.
	 */
	private final Queue<Message> messageQueue;

	/**
	 * The queue that contains all {@link RequestStatus} to be delivered to {@link DeliveryWorker#endpoint}.
	 */
	private final Queue<RequestStatus> statusQueue;

	/**
	 * The queue that contains all notifications to be delivered to {@link DeliveryWorker#endpoint}.
	 */
	private final Queue<String> notificationQueue;

	/**
	 * A {@link de.uniluebeck.itm.tr.util.TimeDiff} instance that is used to determine if a notification should be sent to
	 * the controller that informs him of messages being dropped due to messageQueue congestion.
	 */
	private final TimeDiff lastMessageDropNotificationTimeDiff = new TimeDiff(1000);

	/**
	 * The number of messages that have been dropped since the controller was notified of dropped messages the last time.
	 */
	private int messagesDroppedSinceLastNotification = 0;

	private volatile boolean stopDelivery = false;

	private volatile boolean experimentEnded = false;

	private final Lock lock = new ReentrantLock();

	private final Condition workAvailable = lock.newCondition();

	public DeliveryWorker(final DeliveryManager deliveryManager, final String endpointUrl,
						  final Controller endpoint,
						  final Queue<Message> messageQueue,
						  final Queue<RequestStatus> statusQueue,
						  final Queue<String> notificationQueue) {

		checkNotNull(deliveryManager);
		checkNotNull(endpointUrl);
		checkNotNull(endpoint);
		checkNotNull(messageQueue);
		checkNotNull(statusQueue);
		checkNotNull(notificationQueue);

		this.deliveryManager = deliveryManager;
		this.endpointUrl = endpointUrl;
		this.endpoint = endpoint;
		this.messageQueue = messageQueue;
		this.statusQueue = statusQueue;
		this.notificationQueue = notificationQueue;
	}

	@Override
	public void run() {

		while (!stopDelivery) {

			// check if signal 'experimentEnded' has been given
			lock.lock();
			try {

				if (experimentEnded && deliveryQueuesEmpty()) {
					log.debug(
							"{} => Calling Controller.experimentEnded() as experiment ended and all messages have been delivered.",
							endpointUrl
					);
					endpoint.experimentEnded();
					deliveryManager.removeController(endpointUrl);
					return;
				}

			} finally {
				lock.unlock();
			}

			// deliver notifications, statuses and messages according to their priority one after another
			if (!notificationQueue.isEmpty()) {
				deliverNotifications();
				continue;
			}

			if (!statusQueue.isEmpty()) {
				deliverStatuses();
				continue;
			}

			if (!messageQueue.isEmpty()) {
				deliverMessages();
			}

			// wait for new work to arrive if queues are empty
			lock.lock();
			try {
				if (deliveryQueuesEmpty()) {
					workAvailable.await();
				}
			} catch (InterruptedException e) {
				log.error("Interrupted while waiting for work to arrive: " + e, e);
				stopDelivery();
			} finally {
				lock.unlock();
			}

		}

		log.debug("{} => Stopping message delivery.", endpointUrl);

	}

	private void deliverMessages() {

		final List<Message> messageList = Lists.newArrayListWithExpectedSize(MAXIMUM_MESSAGE_LIST_SIZE);

		lock.lock();
		try {
			while (!messageQueue.isEmpty()) {
				messageList.add(messageQueue.poll());
			}
		} finally {
			lock.unlock();
		}

		endpoint.receive(messageList);
	}

	private void deliverStatuses() {

		final List<RequestStatus> statusList = Lists.newArrayListWithExpectedSize(MAXIMUM_REQUEST_STATUS_LIST_SIZE);

		lock.lock();
		try {
			while (!statusQueue.isEmpty() && statusList.size() < MAXIMUM_REQUEST_STATUS_LIST_SIZE) {
				statusList.add(statusQueue.poll());
			}
		} finally {
			lock.unlock();
		}

		endpoint.receiveStatus(statusList);
	}

	private void deliverNotifications() {
		final List<String> notificationList = Lists.newArrayList();

		lock.lock();
		try {
			while (!notificationQueue.isEmpty() && notificationList.size() < MAXIMUM_NOTIFICATION_LIST_SIZE) {
				notificationList.add(notificationQueue.poll());
			}
		} finally {
			lock.unlock();
		}

		endpoint.receiveNotification(notificationList);
	}

	private boolean deliveryQueuesEmpty() {
		return !(!messageQueue.isEmpty() || !notificationQueue.isEmpty() || !statusQueue.isEmpty());
	}

	/**
	 * Signals the worker to stop delivery. The worker will eventually stop some time afterwards.
	 */
	public void stopDelivery() {
		this.stopDelivery = true;
	}

	/**
	 * Signals the worker that the experiment has ended. The worker will eventually stop himself when all messages in the
	 * messageQueue have been delivered.
	 */
	public void experimentEnded() {
		this.experimentEnded = true;
	}

	public void receive(final Message... messages) {
		receive(Lists.newArrayList(messages));
	}

	public void receive(final List<Message> messages) {
		lock.lock();
		try {

			int queueSpaceAvailable =
					DeliveryManagerConstants.DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE - messageQueue.size();

			int messagesAdded = 0;
			if (queueSpaceAvailable > 0) {
				for (int i = 0; i < queueSpaceAvailable; i++) {
					messageQueue.add(messages.get(i));
					messagesAdded++;
				}
			}

			final int messagesDropped = messages.size() - messagesAdded;

			if (messagesDropped > 0) {
				enqueueDropNotificationIfNotificationRateAllows(messagesDropped);
			}

			workAvailable.signal();
		} finally {
			lock.unlock();
		}
	}

	public void receiveNotification(final String... notifications) {
		lock.lock();
		try {
			Collections.addAll(notificationQueue, notifications);
			workAvailable.signal();
		} finally {
			lock.unlock();
		}
	}

	public void receiveNotification(final List<String> notifications) {
		lock.lock();
		try {
			notificationQueue.addAll(notifications);
			workAvailable.signal();
		} finally {
			lock.unlock();
		}
	}

	public void receiveStatus(final RequestStatus... statuses) {
		lock.lock();
		try {
			Collections.addAll(statusQueue, statuses);
			workAvailable.signal();
		} finally {
			lock.unlock();
		}
	}

	public void receiveStatus(final List<RequestStatus> statuses) {
		lock.lock();
		try {
			statusQueue.addAll(statuses);
			workAvailable.signal();
		} finally {
			lock.unlock();
		}

	}

	/**
	 * Notifies the currently registered controllers that a number of messages have been dropped if the last notification
	 * lies far enough in the past.
	 *
	 * @param messagesDropped the number of messages being dropped right now
	 */

	private void enqueueDropNotificationIfNotificationRateAllows(final int messagesDropped) {

		// increase number of messages that have been dropped since the last notification
		messagesDroppedSinceLastNotification += messagesDropped;

		// enqueue notification if last time lies far enough in the past
		if (lastMessageDropNotificationTimeDiff.isTimeout()) {

			String msg = "Dropped " + messagesDroppedSinceLastNotification + " messages in the last " +
					lastMessageDropNotificationTimeDiff.ms() + " milliseconds, " +
					"because the experiment is producing more messages than the backend is able to deliver.";
			receiveNotification(msg);
			lastMessageDropNotificationTimeDiff.touch();
			messagesDroppedSinceLastNotification = 0;
		}
	}

}
