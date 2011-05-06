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

package eu.wisebed.testbed.api.wsn.controllerhelper;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.internal.Nullable;

import de.uniluebeck.itm.tr.util.TimeDiff;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;


/**
 * Helper class that manages a set of {@link eu.wisebed.testbed.api.wsn.v23.Controller} Web Service endpoints and allows
 * to asynchronously deliver messages and request status updates to them in parallel. If the delivery to a recipient is
 * repeatedly ({@link ControllerHelperConstants#RETRIES} times) impossible due to whatever reason, the recipient is
 * removed from the list of recipients. Between every try there's a pause of {@link
 * ControllerHelperConstants#RETRY_TIMEOUT} in time unit {@link ControllerHelperConstants#RETRY_TIME_UNIT}.
 */
public class ControllerHelper implements DeliverFailureListener {

	/**
	 * The logger instance for this class.
	 */
	private static final Logger log = LoggerFactory.getLogger(ControllerHelper.class);

	/**
	 * The actual instance maximum number of messages to be held in a delivery queue before new messages are discarded.
	 */
	protected final int maximumDeliveryQueueSize;

	/**
	 * The set of {@link eu.wisebed.testbed.api.wsn.v23.Controller} instances currently listening to this WSN service
	 * instance. Maps from the endpoint URL to an instantiated endpoint proxy.
	 */
	private final Map<String, Controller> controllerEndpoints = new HashMap<String, Controller>();

	/**
	 * Used to deliver messages and request status messages in parallel.
	 */
	private final ScheduledThreadPoolExecutor scheduler;

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
		this(ControllerHelperConstants.DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE);
	}

	/**
	 * Constructs a new {@link ControllerHelper} instance.
	 *
	 * @param maximumDeliveryQueueSize the maximum size of the message delivery queue after which messages are dropped
	 */
	public ControllerHelper(final @Nullable Integer maximumDeliveryQueueSize) {

		this.maximumDeliveryQueueSize =
				maximumDeliveryQueueSize == null ?
						ControllerHelperConstants.DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE :
						maximumDeliveryQueueSize;

		scheduler = new ScheduledThreadPoolExecutor(1,
				new ThreadFactoryBuilder().setNameFormat("ControllerHelper-MessageExecutor %d").build(),
				new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
						log.warn("!!!! ControllerHelper.rejectedExecution !!!! Re-scheduling in 100ms.");
						scheduler.schedule(r, 100, TimeUnit.MILLISECONDS);
					}
				}
		);
		scheduler.setMaximumPoolSize(Integer.MAX_VALUE);

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
	 * @param controllerEndpointUrl the endpoint URL of a {@link eu.wisebed.testbed.api.wsn.v23.Controller} Web Service
	 *                              instance
	 */
	public void addController(String controllerEndpointUrl) {
		Controller controller = WSNServiceHelper.getControllerService(controllerEndpointUrl, scheduler);
		synchronized (controllerEndpoints) {
			controllerEndpoints.put(controllerEndpointUrl, controller);
		}
	}

	/**
	 * Removes a Controller service endpoint URL from the list of recipients.
	 *
	 * @param controllerEndpointUrl the endpoint URL of a {@link eu.wisebed.testbed.api.wsn.v23.Controller} Web Service
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
				DeliverNotificationRunnable runnable = new DeliverNotificationRunnable(
						scheduler,
						this,
						controllerEntry.getKey(),
						controllerEntry.getValue(),
						notifications
				);
				scheduler.submit(runnable);
			}
		}
	}

	/**
	 * Sends status messages to client for all node URNs containing the given {@code statusValue} and the Exceptions error
	 * message.
	 *
	 * @param nodeUrns	the node URNs for which the error occurred
	 * @param requestId   the request ID to which this status messages belong
	 * @param e		   the Exception that occurred
	 * @param statusValue the Integer value that should be sent to the controllers
	 */
	public void receiveFailureStatusMessages(List<String> nodeUrns, String requestId, Exception e, int statusValue) {

		RequestStatus requestStatus = new RequestStatus();
		requestStatus.setRequestId(requestId);

		for (String nodeId : nodeUrns) {
			Status status = new Status();
			status.setNodeId(nodeId);
			status.setValue(statusValue);
			status.setMsg(e.getMessage());
			requestStatus.getStatus().add(status);
		}

		receiveStatus(requestStatus);
	}

	/**
	 * Asynchronously notifies all currently registered controllers that the experiment has ended.
	 */
	public void experimentEnded() {
		synchronized (controllerEndpoints) {
			for (Map.Entry<String, Controller> controllerEntry : controllerEndpoints.entrySet()) {
				log.debug("Calling experimentEnded() on endpoint URL {}", controllerEntry.getKey());
				DeliverExperimentEndedRunnable runnable = new DeliverExperimentEndedRunnable(
						scheduler,
						this,
						controllerEntry.getKey(),
						controllerEntry.getValue()
				);
				scheduler.submit(runnable);
			}
		}
	}

	/**
	 * Delivers {@code messages} to all currently registered controllers.
	 *
	 * @param messages the list of messages to be delivered
	 */
	public void receive(List<Message> messages) {

		if (scheduler.getQueue().size() > maximumDeliveryQueueSize) {
			log.error("More than {} messages in the delivery queue. Dropping message!", maximumDeliveryQueueSize);
			sendDropNotificationIfNotificationRateAllows(messages.size());
			return;
		}

		// TODO use more intelligent bundling for messages to de delivered

		synchronized (controllerEndpoints) {
			for (Map.Entry<String, Controller> controllerEntry : controllerEndpoints.entrySet()) {
				log.debug("Delivering message to endpoint URL {}", controllerEntry.getKey());
				DeliverMessageRunnable runnable = new DeliverMessageRunnable(
						scheduler,
						this,
						controllerEntry.getKey(),
						controllerEntry.getValue(),
						messages
				);
				scheduler.submit(runnable);
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
	 * @param message the {@link eu.wisebed.testbed.api.wsn.v23.Message} instance to deliver
	 */
	public void receive(Message message) {
		receive(Lists.newArrayList(message));
	}

	public void receiveStatus(List<RequestStatus> requestStatusList) {

		// TODO use more intelligent bundling for messages to de delivered

		synchronized (controllerEndpoints) {

			for (Map.Entry<String, Controller> controllerEntry : controllerEndpoints.entrySet()) {
				scheduler.submit(new DeliverRequestStatusRunnable(
						scheduler,
						this,
						controllerEntry.getKey(),
						controllerEntry.getValue(),
						requestStatusList
				)
				);
			}
		}
	}

	/**
	 * Delivers {@code requestStatus} to all recipients.
	 *
	 * @param requestStatus the {@link eu.wisebed.testbed.api.wsn.v23.RequestStatus} instance to deliver
	 */
	public void receiveStatus(RequestStatus requestStatus) {
		receiveStatus(Lists.newArrayList(requestStatus));
	}

	/**
	 * Sends a request status to all currently connected controllers indicating that the request included an unknown node
	 * URN by setting it's return value field to -1 and passing the exception message.
	 *
	 * @param nodeUrns  the nodeUrns that failed
	 * @param msg	   an error message that should be sent with the status update
	 * @param requestId the requestId
	 */
	public void receiveUnknownNodeUrnRequestStatus(final Set<String> nodeUrns, final String msg,
												   final String requestId) {

		RequestStatus requestStatus = new RequestStatus();
		requestStatus.setRequestId(requestId);

		for (String nodeUrn : nodeUrns) {

			Status status = new Status();
			status.setMsg(msg);
			status.setNodeId(nodeUrn);
			status.setValue(-1);

			requestStatus.getStatus().add(status);

		}

		this.receiveStatus(requestStatus);

	}

	/**
	 * Tests the connectivity of the given endpoint URL ({@code endpointURL}}) by trying to establish a TCP connection to
	 * this port.
	 *
	 * @param endpointURL the endpoint URL of the Web service for which to test connectivity
	 *
	 * @return {@code true} if a connection can be established (i.e. connectivity is given), {@code false} otherwise
	 */
	public static boolean testConnectivity(String endpointURL) {

		URI uri;

		try {
			uri = URI.create(endpointURL);
		} catch (Exception e) {
			log.error("Invalid endpoint URL given in testConnectivity(): {}", endpointURL);
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

	/**
	 * Calls {@link ControllerHelper#testConnectivity(String)} and throws an {@link IllegalArgumentException} if
	 * connectivity is not given.
	 *
	 * @param endpointUrl the endpoint URL to check
	 */
	public static void checkConnectivity(final String endpointUrl) {
		if (!testConnectivity(endpointUrl)) {
			throw new RuntimeException(
					"Could not connect to host/port of the given endpoint URL: \"" + endpointUrl + "\". "
							+ "Make sure you're not behind a firewall/NAT and the endpoint is already started."
			);
		}
	}

	@Override
	public void deliveryFailed(final String controllerEndpointUrl) {
		removeController(controllerEndpointUrl);
	}

}
