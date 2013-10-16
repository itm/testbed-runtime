package de.uniluebeck.itm.tr.iwsn.common;


import com.google.common.collect.Lists;
import de.uniluebeck.itm.util.TimeDiff;
import de.uniluebeck.itm.util.Tuple;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Controller;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import org.apache.cxf.interceptor.Fault;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.Arrays;
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
	private final Deque<Notification> notificationQueue;

	/**
	 * The queue that contains all nodesAttached events to be delivered to {@link DeliveryWorker#endpoint}.
	 */
	private final Deque<Tuple<DateTime, List<NodeUrn>>> nodesAttachedQueue;

	/**
	 * The queue that contains all nodesDetached events to be delivered to {@link DeliveryWorker#endpoint}.
	 */
	private final Deque<Tuple<DateTime, List<NodeUrn>>> nodesDetachedQueue;

	/**
	 * A {@link de.uniluebeck.itm.util.TimeDiff} instance that is used to determine if a notification should be sent to
	 * the controller that informs him of messages being dropped due to messageQueue congestion.
	 */
	private final TimeDiff lastMessageDropNotificationTimeDiff = new TimeDiff(1000);

	private final String endpointUri;

	/**
	 * The number of messages that have been dropped since the controller was notified of dropped messages the last time.
	 */
	private int messagesDroppedSinceLastNotification = 0;

	private volatile boolean stopDelivery = false;

	private volatile boolean experimentEnded = false;

	private volatile DateTime experimentEndedAt = null;

	private final Lock lock = new ReentrantLock();

	private final Condition workAvailable = lock.newCondition();

	private final int maximumDeliveryQueueSize;

	public DeliveryWorker(final DeliveryManager deliveryManager,
						  final String endpointUri,
						  final Controller endpoint,
						  final Deque<Message> messageQueue,
						  final Deque<RequestStatus> statusQueue,
						  final Deque<Notification> notificationQueue,
						  final Deque<Tuple<DateTime, List<NodeUrn>>> nodesAttachedQueue,
						  final Deque<Tuple<DateTime, List<NodeUrn>>> nodesDetachedQueue,
						  final int maximumDeliveryQueueSize) {
		this.deliveryManager = checkNotNull(deliveryManager);
		this.endpointUri = checkNotNull(endpointUri);
		this.endpoint = checkNotNull(endpoint);
		this.messageQueue = checkNotNull(messageQueue);
		this.statusQueue = checkNotNull(statusQueue);
		this.notificationQueue = checkNotNull(notificationQueue);
		this.nodesAttachedQueue = checkNotNull(nodesAttachedQueue);
		this.nodesDetachedQueue = checkNotNull(nodesDetachedQueue);
		this.maximumDeliveryQueueSize = maximumDeliveryQueueSize;
	}

	@Override
	public void run() {

		int subsequentDeliveryErrors = 0;

		while (!stopDelivery) {

			// check if signal 'reservationEnded' has been given
			lock.lock();
			try {

				if (experimentEnded && deliveryQueuesEmpty()) {
					log.debug(
							"{} => Calling reservationEnded() as experiment ended and all messages have been delivered.",
							endpoint
					);
					try {
						endpoint.reservationEnded(experimentEndedAt);
					} catch (Exception e) {
						log.warn(
								"{} => Exception while calling reservationEnded() after delivering all queued messages.",
								endpoint
						);
					}
					deliveryManager.removeController(endpointUri);
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
			} else if (!nodesAttachedQueue.isEmpty()) {
				deliverySuccessful = deliverNodesAttached();
			} else if (!nodesDetachedQueue.isEmpty()) {
				deliverySuccessful = deliverNodesDetached();
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
						log.error("{} => Interrupted while waiting for retry: " + e1, endpoint);
						stopDelivery();
						continue;
					}

				} else {

					log.warn("{} => Repeatedly could not deliver messages. Removing endpoint.", endpoint);
					deliveryManager.removeController(endpointUri); // will also call stopDelivery()
				}
			}

			// wait for new work to arrive if queues are empty
			lock.lock();
			try {
				if (!experimentEnded && !stopDelivery && deliveryQueuesEmpty()) {
					workAvailable.await();
				}
			} catch (InterruptedException e) {
				log.error("{} => Interrupted while waiting for work to arrive: " + e, endpoint);
				stopDelivery();
			} finally {
				lock.unlock();
			}

		}

		log.debug("{} => Stopping message delivery.", endpoint);

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

		log.trace("{} => Delivering {} messages.", endpoint, messageList.size());
		try {

			// try to send messages to endpoint
			endpoint.receive(messageList);
			return true;

		} catch (Exception e) {

			boolean clientDied = e instanceof Fault && e.getCause() instanceof ConnectException;

			// if delivery failed
			if (!clientDied) {
				log.warn("{} => Exception while delivering messages. Reason: {}", endpoint, e);
			}

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

		log.trace("{} => Delivering {} status messages", endpoint, statusList.size());
		try {

			endpoint.receiveStatus(statusList);
			return true;

		} catch (Exception e) {

			// if delivery failed
			log.warn("{} => Exception while delivering status messages. Reason: {}", endpoint, e);

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

		final List<Notification> notificationList = Lists.newArrayList();

		lock.lock();
		try {
			while (!notificationQueue.isEmpty() && notificationList.size() < MAXIMUM_NOTIFICATION_LIST_SIZE) {
				notificationList.add(notificationQueue.poll());
			}
		} finally {
			lock.unlock();
		}

		log.trace("{} => Delivering {} notifications", endpoint, notificationList.size());
		try {

			endpoint.receiveNotification(notificationList);
			return true;

		} catch (Exception e) {

			// if delivery failed
			log.warn("{} => Exception while delivering notifications. Reason: {}", endpoint, e);

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

	private boolean deliverNodesAttached() {

		final Tuple<DateTime, List<NodeUrn>> nodesAttachedEvent;

		lock.lock();
		try {
			nodesAttachedEvent = nodesAttachedQueue.poll();
		} finally {
			lock.unlock();
		}

		if (log.isTraceEnabled()) {
			log.trace(
					"{} => Delivering nodes attached events for node URNs {}",
					endpoint,
					Arrays.toString(nodesAttachedEvent.getSecond().toArray())
			);
		}

		try {

			endpoint.nodesAttached(nodesAttachedEvent.getFirst(), nodesAttachedEvent.getSecond());
			return true;

		} catch (Exception e) {
			lock.lock();
			try {
				nodesAttachedQueue.addFirst(nodesAttachedEvent);
			} finally {
				lock.unlock();
			}
		}

		return false;
	}

	private boolean deliverNodesDetached() {

		final Tuple<DateTime, List<NodeUrn>> nodesDetachedEvent;

		lock.lock();
		try {
			nodesDetachedEvent = nodesDetachedQueue.poll();
		} finally {
			lock.unlock();
		}

		if (log.isTraceEnabled()) {
			log.trace(
					"{} => Delivering nodes detached events for node URNs {}",
					endpoint,
					Arrays.toString(nodesDetachedEvent.getSecond().toArray())
			);
		}

		try {

			endpoint.nodesDetached(nodesDetachedEvent.getFirst(), nodesDetachedEvent.getSecond());
			return true;

		} catch (Exception e) {
			lock.lock();
			try {
				nodesDetachedQueue.addFirst(nodesDetachedEvent);
			} finally {
				lock.unlock();
			}
		}

		return false;
	}

	private boolean deliveryQueuesEmpty() {
		return !(!messageQueue.isEmpty() ||
				!notificationQueue.isEmpty() ||
				!statusQueue.isEmpty() ||
				!nodesAttachedQueue.isEmpty() ||
				!nodesDetachedQueue.isEmpty());
	}

	/**
	 * Signals the worker to stop delivery. The worker will eventually stop some time afterwards.
	 */
	public void stopDelivery() {
		this.stopDelivery = true;
	}

	/**
	 * Signals the worker that the experiment has started.
	 *
	 * @param timestamp
	 * 		the time at which the reservation started
	 */
	public void reservationStarted(final DateTime timestamp) {
		endpoint.reservationStarted(timestamp);
	}

	/**
	 * Signals the worker that the experiment has ended. The worker will eventually stop himself when all messages in the
	 * messageQueue have been delivered.
	 */
	public void reservationEnded(final DateTime timestamp) {
		this.experimentEndedAt = timestamp;
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

	public void receiveNotification(final Notification... notifications) {
		lock.lock();
		try {
			Collections.addAll(notificationQueue, notifications);
			workAvailable.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void nodesAttached(final DateTime timestamp, final List<NodeUrn> nodeUrns) {
		lock.lock();
		try {
			nodesAttachedQueue.add(new Tuple<DateTime, List<NodeUrn>>(timestamp, nodeUrns));
			workAvailable.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void nodesDetached(final DateTime timestamp, final List<NodeUrn> nodeUrns) {
		lock.lock();
		try {
			nodesDetachedQueue.add(new Tuple<DateTime, List<NodeUrn>>(timestamp, nodeUrns));
			workAvailable.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void receiveNotification(final List<Notification> notifications) {
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
	 * @param messagesDropped
	 * 		the number of messages being dropped right now
	 */

	private void enqueueDropNotificationIfNotificationRateAllows(final int messagesDropped) {

		// increase number of messages that have been dropped since the last notification
		messagesDroppedSinceLastNotification += messagesDropped;

		// enqueue notification if last time lies far enough in the past
		if (lastMessageDropNotificationTimeDiff.isTimeout()) {

			String msg = "Dropped " + messagesDroppedSinceLastNotification + " messages in the last " +
					lastMessageDropNotificationTimeDiff.ms() + " milliseconds, " +
					"because the experiment is producing more messages than the backend is able to deliver.";
			Notification notification = new Notification();
			notification.setMsg(msg);
			receiveNotification(notification);
			lastMessageDropNotificationTimeDiff.touch();
			messagesDroppedSinceLastNotification = 0;
		}
	}
}