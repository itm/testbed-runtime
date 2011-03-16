/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

package eu.wisebed.testbed.api.wsn;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.internal.Nullable;
import de.uniluebeck.itm.tr.util.TimeDiff;
import eu.wisebed.testbed.api.wsn.v22.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Helper class that manages a set of {@link eu.wisebed.testbed.api.wsn.v22.Controller} Web Service endpoints and allows
 * to asynchronously deliver messages and request status updates to them in parallel. If the delivery to a recipient is
 * repeatedly ({@link eu.wisebed.testbed.api.wsn.ControllerHelper#RETRIES} times) impossible due to whatever reason, the
 * recipient is removed from the list of recipients. Between every try there's a pause of {@link
 * eu.wisebed.testbed.api.wsn.ControllerHelper#RETRY_TIMEOUT} in time unit {@link eu.wisebed.testbed.api.wsn.ControllerHelper#RETRY_TIME_UNIT}.
 */
public class ControllerHelper {

	/**
	 * The logger instance for this class.
	 */
	private static final Logger log = LoggerFactory.getLogger(ControllerHelper.class);

	/**
	 * Number of message delivery retries. If delivering a message fails, e.g., due to temporarily unavailable network
	 * connection on either server or client side the delivery will be retried after {@link ControllerHelper#RETRY_TIMEOUT}
	 * time units (cf. {@link ControllerHelper#RETRY_TIME_UNIT}).
	 */
	protected static final int RETRIES = 3;

	/**
	 * Number of time units to wait for retry of message delivery (cf. {@link ControllerHelper#RETRY_TIME_UNIT}).
	 */
	protected static final long RETRY_TIMEOUT = 5;

	/**
	 * The time unit of the {@link ControllerHelper#RETRY_TIMEOUT}.
	 */
	protected static final TimeUnit RETRY_TIME_UNIT = TimeUnit.SECONDS;

	/**
	 * The default maximum number of messages to be held in a delivery queue before new messages are discarded.
	 */
	protected static final int DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE = 1000;

	/**
	 * Base class for message delivery. Retries message delivery {@link ControllerHelper#RETRIES} times if an error occurs
	 * before "giving up". If delivery fails repeatedly the controller for which it fails is removed.
	 */
	private abstract class AbstractDeliverRunnable implements Runnable {

		protected int retries = 0;

		protected final Map.Entry<String, Controller> controllerEntry;

		protected AbstractDeliverRunnable(Map.Entry<String, Controller> controllerEntry) {
			this.controllerEntry = controllerEntry;
		}

		@Override
		public final void run() {

			if (retries < RETRIES) {

				retries++;
				try {

					// try to deliver the message
					deliver(controllerEntry.getValue());

				} catch (Exception e) {

					// delivery failed
					log.warn("Could not deliver message / request status to Controller at endpoint URL {}. "
							+ "Trying again in {} {}"
							+ "Error = {}",
							new Object[]{
									controllerEntry.getKey(),
									RETRY_TIMEOUT,
									RETRY_TIME_UNIT.toString().toLowerCase(),
									e
							}
					);

					// reschedule delivery
					executorService.schedule(this, RETRY_TIMEOUT, RETRY_TIME_UNIT);
				}
			} else {

				// if number of delivery retries is larger than maximum do not try delivery again
				log.warn("Repeatedly (tried {} times) could not deliver message to Controller at endpoint URL {}. "
						+ "Removing controller endpoint...",
						new Object[]{
								RETRIES, controllerEntry.getKey()
						}
				);

				// delete controller from list of controllers as obviously this controller is offline
				removeController(controllerEntry.getKey());
			}
		}

		/**
		 * Does the actual delivery logic to the client <b>exactly one time</b>.
		 *
		 * @param controller the Controller proxy to the client to deliver to
		 */
		protected abstract void deliver(Controller controller);

	}

	private class DeliverRequestStatusRunnable extends AbstractDeliverRunnable {

		private Function<List<RequestStatus>, List<String>> extractRequestIdListFunction =
				new Function<List<RequestStatus>, List<String>>() {
					@Override
					public List<String> apply(final List<RequestStatus> requestStatuses) {
						if (requestStatuses == null) {
							return null;
						}
						List<String> requestIds = Lists.newArrayListWithCapacity(requestStatuses.size());
						for (RequestStatus requestStatus : requestStatuses) {
							requestIds.add(requestStatus.getRequestId());
						}
						return requestIds;
					}
				};

		private Function<List<? extends Object>, String> convertListToStringFunction =
				new Function<List<? extends Object>, String>() {
					@Override
					public String apply(final List<? extends Object> objects) {
						return objects == null ? null : Arrays.toString(objects.toArray());
					}
				};

		private Function<List<RequestStatus>, String> convertRequestStatusListToStringFunction = Functions.compose(
				convertListToStringFunction,
				extractRequestIdListFunction
		);

		private List<RequestStatus> requestStatusList;

		private DeliverRequestStatusRunnable(Map.Entry<String, Controller> controllerEntry,
											 List<RequestStatus> requestStatusList) {
			super(controllerEntry);
			this.requestStatusList = requestStatusList;
		}

		@Override
		protected void deliver(Controller controller) {

			if (log.isDebugEnabled()) {

				log.debug("StatusDelivery[requestIds={},endpointUrl={},queueSize={}]",
						new Object[]{
								convertRequestStatusListToStringFunction.apply(requestStatusList),
								controllerEntry.getKey(),
								executorService.getQueue().size()
						}
				);
			}

			controller.receiveStatus(requestStatusList);
		}
	}

	private class DeliverNotificationRunnable extends AbstractDeliverRunnable {

		private final List<String> notifications;

		public DeliverNotificationRunnable(final Map.Entry<String, Controller> controllerEntry,
										   final List<String> notifications) {
			super(controllerEntry);
			this.notifications = notifications;
		}

		@Override
		protected void deliver(final Controller controller) {
			controller.receiveNotification(notifications);
		}
	}

	private class DeliverMessageRunnable extends AbstractDeliverRunnable {

		private List<Message> msg;

		private DeliverMessageRunnable(Map.Entry<String, Controller> controllerEntry, List<Message> msg) {
			super(controllerEntry);
			this.msg = msg;
		}

		@Override
		protected void deliver(Controller controller) {
			controller.receive(msg);
		}
	}

	private class ExperimentEndedRunnable extends AbstractDeliverRunnable {

		public ExperimentEndedRunnable(final Map.Entry<String, Controller> controllerEntry) {
			super(controllerEntry);
		}

		@Override
		protected void deliver(final Controller controller) {
			controller.experimentEnded();
		}
	}

	/**
	 * The actual instance maximum number of messages to be held in a delivery queue before new messages are discarded.
	 */
	protected final int maximumDeliveryQueueSize;

	/**
	 * The set of {@link eu.wisebed.testbed.api.wsn.v22.Controller} instances currently listening to this WSN service
	 * instance. Maps from the endpoint URL to an instantiated endpoint proxy.
	 */
	private final Map<String, Controller> controllerEndpoints = new HashMap<String, Controller>();

	/**
	 * Used to deliver messages and request status messages in parallel.
	 */
	private final ScheduledThreadPoolExecutor executorService;

	/**
	 * A {@link TimeDiff} instance that is used to determine if a notification should be sent to the controller that
	 * informs him of messages being dropped due to queue congestion.
	 */
	private final TimeDiff lastMessageDropNotificationTimeDiff = new TimeDiff(1000);

	/**
	 * The number of messages that have been dropped since the controller was notified of dropped messages the last time.
	 */
	private int messagesDroppedSinceLastNotification = 0;

	/**
	 * A lock for the critical region defined by {@link ControllerHelper#lastMessageDropNotificationTimeDiff} and {@link
	 * ControllerHelper#messagesDroppedSinceLastNotification}.
	 */
	private final Lock messageDropLock = new ReentrantLock();

	public ControllerHelper() {
		this(DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE);
	}

	/**
	 * Constructs a new {@link ControllerHelper} instance.
	 *
	 * @param maximumDeliveryQueueSize the maximum size of the message delivery queue after which messages are dropped
	 */
	public ControllerHelper(@Nullable Integer maximumDeliveryQueueSize) {

		this.maximumDeliveryQueueSize =
				maximumDeliveryQueueSize == null ? DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE : maximumDeliveryQueueSize;

		executorService = new ScheduledThreadPoolExecutor(1,
				new ThreadFactoryBuilder().setNameFormat("ControllerHelper-MessageExecutor %d").build(),
				new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
						log.warn("!!!! ControllerHelper.rejectedExecution !!!! Re-scheduling in 100ms.");
						executorService.schedule(r, 100, TimeUnit.MILLISECONDS);
					}
				}
		);
		executorService.setMaximumPoolSize(Integer.MAX_VALUE);

	}

	/**
	 * Returns the number of controllers currently registered.
	 *
	 * @return the number of controllers currently registered
	 */
	public int getControllerCount() {
		return controllerEndpoints.size();
	}

	/**
	 * Adds a Controller service endpoint URL to the list of recipients.
	 *
	 * @param controllerEndpointUrl the endpoint URL of a {@link eu.wisebed.testbed.api.wsn.v22.Controller} Web Service
	 *                              instance
	 */
	public void addController(String controllerEndpointUrl) {
		Controller controller = WSNServiceHelper.getControllerService(controllerEndpointUrl, executorService);
		synchronized (controllerEndpoints) {
			controllerEndpoints.put(controllerEndpointUrl, controller);
		}
	}

	/**
	 * Removes a Controller service endpoint URL from the list of recipients.
	 *
	 * @param controllerEndpointUrl the endpoint URL of a {@link eu.wisebed.testbed.api.wsn.v22.Controller} Web Service
	 *                              instance
	 */
	public void removeController(String controllerEndpointUrl) {
		synchronized (controllerEndpoints) {
			controllerEndpoints.remove(controllerEndpointUrl);
		}
	}

	/**
	 * Forwards the notification asynchronously to all currently registered controllers.
	 * <p/>
	 * <b>Please note:</b> individual notification messages may be dropped if the delivery queue is too long.
	 *
	 * @param notifications a list of notifications to be forwarded all currently registered controllers
	 */
	public void receiveNotification(final List<String> notifications) {
		synchronized (controllerEndpoints) {
			for (Map.Entry<String, Controller> controllerEntry : controllerEndpoints.entrySet()) {
				log.debug("Delivering notification to endpoint URL {}", controllerEntry.getKey());
				DeliverNotificationRunnable runnable = new DeliverNotificationRunnable(controllerEntry, notifications);
				executorService.submit(runnable);
			}
		}
	}

	/**
	 * Asynchronously notifies all currently registered controllers that the experiment has ended.
	 */
	public void experimentEnded() {
		synchronized (controllerEndpoints) {
			for (Map.Entry<String, Controller> controllerEntry : controllerEndpoints.entrySet()) {
				log.debug("Calling experimentEnded() on endpoint URL {}", controllerEntry.getKey());
				ExperimentEndedRunnable runnable = new ExperimentEndedRunnable(controllerEntry);
				executorService.submit(runnable);
			}
		}
	}

	/**
	 * Delivers {@code messages} to all currently registered controllers.
	 *
	 * @param messages the list of messages to be delivered
	 */
	public void receive(List<Message> messages) {

		if (executorService.getQueue().size() > maximumDeliveryQueueSize) {
			log.error("More than {} messages in the delivery queue. Dropping message!", maximumDeliveryQueueSize);
			sendDropNotificationIfNotificationRateAllows(messages.size());
			return;
		}

		// TODO use more intelligent bundling for messages to de delivered

		synchronized (controllerEndpoints) {
			for (Map.Entry<String, Controller> controllerEntry : controllerEndpoints.entrySet()) {
				log.debug("Delivering message to endpoint URL {}", controllerEntry.getKey());
				DeliverMessageRunnable runnable = new DeliverMessageRunnable(controllerEntry, messages);
				executorService.submit(runnable);
			}
		}
	}

	/**
	 * Notifies the currently registered controllers that a number of messages have been dropped if the last notification
	 * lies far enough in the past.
	 *
	 * @param messagesDropped the number of messages being dropped right now
	 */
	private void sendDropNotificationIfNotificationRateAllows(final int messagesDropped) {
		messageDropLock.lock();
		try {

			// increase number of messages that have been dropped since the last notification
			messagesDroppedSinceLastNotification += messagesDropped;

			// send notification if last time lies far enough in the past
			if (lastMessageDropNotificationTimeDiff.isTimeout()) {

				String msg = "Dropped " + messagesDroppedSinceLastNotification + " messages in the last " +
						lastMessageDropNotificationTimeDiff.ms() + " milliseconds, " +
						"because the experiment is producing more messages than the backend is able to deliver.";
				receiveNotification(Lists.newArrayList(msg));
				lastMessageDropNotificationTimeDiff.touch();
				messagesDroppedSinceLastNotification = 0;
			}

		} finally {
			messageDropLock.unlock();
		}
	}

	/**
	 * Delivers {@code message} to all currently registered controllers.
	 *
	 * @param message the {@link eu.wisebed.testbed.api.wsn.v22.Message} instance to deliver
	 */
	public void receive(Message message) {
		receive(Lists.newArrayList(message));
	}

	public void receiveStatus(List<RequestStatus> requestStatusList) {

		// TODO use more intelligent bundling for messages to de delivered

		synchronized (controllerEndpoints) {

			for (Map.Entry<String, Controller> controllerEntry : controllerEndpoints.entrySet()) {
				executorService.submit(new DeliverRequestStatusRunnable(controllerEntry, requestStatusList));
			}
		}
	}

	/**
	 * Delivers {@code requestStatus} to all recipients.
	 *
	 * @param requestStatus the {@link eu.wisebed.testbed.api.wsn.v22.RequestStatus} instance to deliver
	 */
	public void receiveStatus(RequestStatus requestStatus) {
		receiveStatus(Lists.newArrayList(requestStatus));
	}

	/**
	 * Sends a request status to all currently connected controllers indicating that the request included an unknown node
	 * URN by setting it's return value field to -1 and passing the exception message.
	 *
	 * @param e		 the exception
	 * @param requestId the requestId
	 */
	public void receiveUnknownNodeUrnRequestStatus(final UnknownNodeUrnException_Exception e, final String requestId) {

		for (String nodeUrn : e.getFaultInfo().getUrn()) {

			Status status = new Status();
			status.setMsg(e.getFaultInfo().getMessage());
			status.setNodeId(nodeUrn);
			status.setValue(-1);

			RequestStatus requestStatus = new RequestStatus();
			requestStatus.setRequestId(requestId);
			requestStatus.getStatus().add(status);

			this.receiveStatus(requestStatus);

		}

	}

	/**
	 * Tests the connectivity of the given endpoint URL ({@code controllerEndpointURL}}) by trying to establish a TCP
	 * connection to this port.
	 *
	 * @param controllerEndpointURL the endpoint URL of the Web service for which to test connectivity
	 *
	 * @return {@code true} if a connection can be established (i.e. connectivity is given), {@code false} otherwise
	 */
	public static boolean testConnectivity(String controllerEndpointURL) {

		URI uri;

		try {
			uri = URI.create(controllerEndpointURL);
		} catch (Exception e) {
			log.error("Invalid controllerEndpointURL given in testConnectivity(): {}", controllerEndpointURL);
			return false;
		}

		try {

			Socket socket = new Socket(uri.getHost(), uri.getPort());
			boolean connected = socket.isConnected();
			socket.close();
			return connected;

		} catch (IOException e) {
			log.warn("Could not connect to controller endpoint host/port. Reason: {}", e.getMessage());
		}

		return false;
	}

}
