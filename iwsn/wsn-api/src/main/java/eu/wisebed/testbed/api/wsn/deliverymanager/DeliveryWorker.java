package eu.wisebed.testbed.api.wsn.deliverymanager;


import com.google.common.collect.Lists;
import de.uniluebeck.itm.tr.util.TimeDiff;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
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
	private final Deque<Message> messageQueue;

	/**
	 * The queue that contains all {@link RequestStatus} to be delivered to {@link DeliveryWorker#endpoint}.
	 */
	private final Deque<RequestStatus> statusQueue;

	/**
	 * The queue that contains all notifications to be delivered to {@link DeliveryWorker#endpoint}.
	 */
	private final Deque<String> notificationQueue;

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

	private final int maximumDeliveryQueueSize;

	public DeliveryWorker(final DeliveryManager deliveryManager,
						  final String endpointUrl,
						  final Controller endpoint,
						  final Deque<Message> messageQueue,
						  final Deque<RequestStatus> statusQueue,
						  final Deque<String> notificationQueue,
						  final int maximumDeliveryQueueSize) {

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
		this.maximumDeliveryQueueSize = maximumDeliveryQueueSize;
	}

	@Override
	public void run() {

		int subsequentDeliveryErrors = 0;

		while (!stopDelivery) {

			// check if signal 'experimentEnded' has been given
			lock.lock();
			try {

				if (experimentEnded && deliveryQueuesEmpty()) {
					log.debug(
							"{} => Calling experimentEnded() as experiment ended and all messages have been delivered.",
							endpointUrl
					);
					try {
						endpoint.experimentEnded();
					} catch (Exception e) {
						log.warn(
								"{} => Exception while calling experimentEnded() after delivering all queued messages.",
								endpointUrl
						);
					}
					deliveryManager.removeController(endpointUrl);
					return;
				}

			} finally {
				lock.unlock();
			}

			boolean deliverySuccessful = true;

			// deliver notifications, statuses and messages according to their priority one after another
			if (!notificationQueue.isEmpty()) {
				deliverySuccessful = deliverNotifications();
			} else if (!statusQueue.isEmpty()) {
				deliverySuccessful = deliverStatuses();
			} else if (!messageQueue.isEmpty()) {
				deliverySuccessful = deliverMessages();
			}

			if (deliverySuccessful) {

				subsequentDeliveryErrors = 0;

			} else {

				subsequentDeliveryErrors++;

				if (subsequentDeliveryErrors < DeliveryManagerConstants.RETRIES) {

					// wait a little before retrying, could be a temporary network outage
					try {

						Thread.sleep(
								DeliveryManagerConstants.RETRY_TIME_UNIT.toMillis(
										DeliveryManagerConstants.RETRY_TIMEOUT
								)
						);

					} catch (InterruptedException e1) {
						log.error("{} => Interrupted while waiting for retry: " + e1, endpointUrl);
						stopDelivery();
						continue;
					}

				} else {

					log.warn("{} => Repeatedly could not deliver messages. Removing endpoint.", endpointUrl);
					deliveryManager.removeController(endpointUrl); // will also call stopDelivery()
				}
			}

			// wait for new work to arrive if queues are empty
			lock.lock();
			try {
				if (!experimentEnded && !stopDelivery && deliveryQueuesEmpty()) {
					workAvailable.await();
				}
			} catch (InterruptedException e) {
				log.error("{} => Interrupted while waiting for work to arrive: " + e, endpointUrl);
				stopDelivery();
			} finally {
				lock.unlock();
			}

		}

		log.debug("{} => Stopping message delivery.", endpointUrl);

	}

	private boolean deliverMessages() {

		final List<Message> messageList = Lists.newArrayListWithExpectedSize(MAXIMUM_MESSAGE_LIST_SIZE);

		lock.lock();
		try {

			while (!messageQueue.isEmpty() && messageList.size() < MAXIMUM_MESSAGE_LIST_SIZE) {
				messageList.add(messageQueue.poll());
			}

		} finally {
			lock.unlock();
		}

		log.trace("{} => Delivering {} messages.", endpointUrl, messageList.size());
		try {

			// try to send messages to endpoint
			endpoint.receive(messageList);
			return true;

		} catch (Exception e) {

			// if delivery failed
			log.warn("{} => Exception while delivering messages. Reason: {}", endpointUrl, e);

			// put messages back in that have been taken out before (in reverse order)
			lock.lock();
			try {
				for (int i = messageList.size() - 1; i >= 0; i--) {
					messageQueue.addFirst(messageList.get(i));
				}
			} finally {
				lock.unlock();
			}

			return false;
		}
	}

	private boolean deliverStatuses() {

		final List<RequestStatus> statusList = Lists.newArrayListWithExpectedSize(MAXIMUM_REQUEST_STATUS_LIST_SIZE);

		lock.lock();
		try {
			while (!statusQueue.isEmpty() && statusList.size() < MAXIMUM_REQUEST_STATUS_LIST_SIZE) {
				statusList.add(statusQueue.poll());
			}
		} finally {
			lock.unlock();
		}

		log.trace("{} => Delivering {} status messages", endpointUrl, statusList.size());
		try {

			endpoint.receiveStatus(statusList);
			return true;

		} catch (Exception e) {

			// if delivery failed
			log.warn("{} => Exception while delivering status messages. Reason: {}", endpointUrl, e);

			// put statuses back in that have been taken out before (in reverse order)
			lock.lock();
			try {
				for (int i = statusList.size() - 1; i >= 0; i--) {
					statusQueue.addFirst(statusList.get(i));
				}
			} finally {
				lock.unlock();
			}

			return false;

		}
	}

	private boolean deliverNotifications() {

		final List<String> notificationList = Lists.newArrayList();

		lock.lock();
		try {
			while (!notificationQueue.isEmpty() && notificationList.size() < MAXIMUM_NOTIFICATION_LIST_SIZE) {
				notificationList.add(notificationQueue.poll());
			}
		} finally {
			lock.unlock();
		}

		log.trace("{} => Delivering {} notifications", endpointUrl, notificationList.size());
		try {

			endpoint.receiveNotification(notificationList);
			return true;

		} catch (Exception e) {

			// if delivery failed
			log.warn("{} => Exception while delivering notifications. Reason: {}", endpointUrl, e);

			// put statuses back in that have been taken out before (in reverse order)
			lock.lock();
			try {
				for (int i = notificationList.size() - 1; i >= 0; i--) {
					notificationQueue.addFirst(notificationList.get(i));
				}
			} finally {
				lock.unlock();
			}

			return false;

		}
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
		lock.lock();
		try {
			workAvailable.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void receive(final Message... messages) {
		receive(Lists.newArrayList(messages));
	}

	public void receive(final List<Message> messages) {
		lock.lock();
		try {

			int queueSpaceAvailable = maximumDeliveryQueueSize - messageQueue.size();

			int messagesAdded = 0;
			if (queueSpaceAvailable > 0) {
				for (int i = 0; i < messages.size() && i < queueSpaceAvailable; i++) {
					messageQueue.add(messages.get(i));
					messagesAdded++;
				}
			}

			final int messagesDropped = messages.size() - messagesAdded;

			if (messagesDropped > 0) {
				enqueueDropNotificationIfNotificationRateAllows(messagesDropped);
			}

			workAvailable.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void receiveNotification(final String... notifications) {
		lock.lock();
		try {
			Collections.addAll(notificationQueue, notifications);
			workAvailable.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void receiveNotification(final List<String> notifications) {
		lock.lock();
		try {
			notificationQueue.addAll(notifications);
			workAvailable.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void receiveStatus(final RequestStatus... statuses) {
		lock.lock();
		try {
			Collections.addAll(statusQueue, statuses);
			workAvailable.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void receiveStatus(final List<RequestStatus> statuses) {
		lock.lock();
		try {
			statusQueue.addAll(statuses);
			workAvailable.signalAll();
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
