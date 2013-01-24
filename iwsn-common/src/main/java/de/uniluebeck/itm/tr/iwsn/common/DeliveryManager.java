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

package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Controller;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import eu.wisebed.api.v3.controller.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Helper class that manages a set of {@link Controller} Web Service endpoints and allows to asynchronously deliver
 * messages, notifications and request status updates to them in parallel. If the delivery to a recipient is repeatedly
 * ({@link DeliveryManagerConstants#RETRIES} times) impossible due to whatever reason, the recipient is removed from
 * the
 * list of recipients. Between every try there's a pause of {@link DeliveryManagerConstants#RETRY_TIMEOUT} in time unit
 * {@link DeliveryManagerConstants#RETRY_TIME_UNIT}.
 */
public class DeliveryManager extends AbstractService implements Service {

	/**
	 * The logger instance for this class.
	 */
	private static final Logger log = LoggerFactory.getLogger(DeliveryManager.class);

	/**
	 * The actual instance maximum number of messages to be held in a delivery queue before new messages are discarded.
	 */
	protected final int maximumDeliveryQueueSize;

	/**
	 * The set of {@link Controller} instances currently listening to this WSN service instance. Maps from the endpoint
	 * URL
	 * to an instantiated endpoint proxy.
	 */
	private ImmutableMap<String, DeliveryWorker> controllers = ImmutableMap.of();

	/**
	 * Used to deliver messages and request status messages in parallel.
	 */
	private ExecutorService executorService;

	/**
	 * Constructs a new {@link DeliveryManager} instance.
	 */
	public DeliveryManager() {
		this(null);
	}

	/**
	 * Constructs a new {@link DeliveryManager} instance.
	 *
	 * @param maximumDeliveryQueueSize
	 * 		the maximum size of the message delivery queue after which messages are dropped
	 * 		or {@code null} to use the default value of {@link DeliveryManagerConstants#DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE}.
	 */
	public DeliveryManager(final @Nullable Integer maximumDeliveryQueueSize) {
		this.maximumDeliveryQueueSize = maximumDeliveryQueueSize == null ?
				DeliveryManagerConstants.DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE :
				maximumDeliveryQueueSize;
	}

	/**
	 * Adds a Controller service endpoint URL to the list of recipients.
	 *
	 * @param endpointUrl
	 * 		the endpoint URL of a {@link Controller} Web Service instance
	 */
	public void addController(String endpointUrl) {

		if (controllers.containsKey(endpointUrl)) {
			log.debug("Not adding controller endpoint {} as it is already in the set of controllers.", endpointUrl);
			return;
		}

		log.debug("Adding controller endpoint {} to the set of controllers.", endpointUrl);
		final Controller endpoint = WisebedServiceHelper.getControllerService(endpointUrl, executorService);

		final Deque<Message> messageQueue = new LinkedList<Message>();
		final Deque<Notification> notificationQueue = new LinkedList<Notification>();
		final Deque<RequestStatus> statusQueue = new LinkedList<RequestStatus>();
		final Deque<List<NodeUrn>> nodesAttachedQueue = new LinkedList<List<NodeUrn>>();
		final Deque<List<NodeUrn>> nodesDetachedQueue = new LinkedList<List<NodeUrn>>();

		final DeliveryWorker deliveryWorker = new DeliveryWorker(
				this,
				endpointUrl,
				endpoint,
				messageQueue,
				statusQueue,
				notificationQueue,
				nodesAttachedQueue,
				nodesDetachedQueue,
				maximumDeliveryQueueSize
		);

		executorService.submit(deliveryWorker);

		controllers = ImmutableMap.<String, DeliveryWorker>builder()
				.putAll(controllers)
				.put(endpointUrl, deliveryWorker)
				.build();

	}

	/**
	 * Removes a Controller service endpoint URL from the list of recipients.
	 *
	 * @param endpointUrl
	 * 		the endpoint URL of a {@link Controller} Web Service instance
	 */
	public void removeController(String endpointUrl) {

		final DeliveryWorker deliveryWorker = controllers.get(endpointUrl);

		if (deliveryWorker != null) {

			log.debug("{} => Removing controller endpoint from the set of controllers.", endpointUrl);
			deliveryWorker.stopDelivery();

			ImmutableMap.Builder<String, DeliveryWorker> controllerEndpointsBuilder = ImmutableMap.builder();

			for (Map.Entry<String, DeliveryWorker> entry : controllers.entrySet()) {
				if (!entry.getKey().equals(endpointUrl)) {
					controllerEndpointsBuilder.put(entry.getKey(), entry.getValue());
				}
			}

			controllers = controllerEndpointsBuilder.build();

		} else {
			log.debug("{} => Not removing controller endpoint as it was not in the set of controllers.", endpointUrl);
		}

	}

	/**
	 * Asynchronously notifies all currently registered controllers that the experiment has ended.
	 */
	public void reservationEnded() {

		if (isRunning()) {

			for (DeliveryWorker deliveryWorker : controllers.values()) {
				deliveryWorker.experimentEnded();
			}
		}
	}

	public void nodesAttached(final List<NodeUrn> nodeUrns) {
		if (isRunning()) {
			for (DeliveryWorker deliveryWorker : controllers.values()) {
				deliveryWorker.nodesAttached(nodeUrns);
			}
		}
	}


	public void nodesDetached(final List<NodeUrn> nodeUrns) {
		if (isRunning()) {
			for (DeliveryWorker deliveryWorker : controllers.values()) {
				deliveryWorker.nodesDetached(nodeUrns);
			}
		}
	}

	/**
	 * Delivers {@code messages} to all currently registered controllers.
	 *
	 * @param messages
	 * 		the list of messages to be delivered
	 */
	public void receive(final Message... messages) {

		if (isRunning()) {

			for (DeliveryWorker deliveryWorker : controllers.values()) {
				deliveryWorker.receive(messages);
			}
		}
	}

	/**
	 * Delivers {@code messages} to all currently registered controllers.
	 *
	 * @param messages
	 * 		the list of messages to be delivered
	 */
	public void receive(List<Message> messages) {

		if (isRunning()) {

			for (DeliveryWorker deliveryWorker : controllers.values()) {
				deliveryWorker.receive(messages);
			}
		}
	}

	/**
	 * Forwards the list of notifications to the {@link DeliveryWorker} instances that every connected controller has.
	 *
	 * @param notifications
	 * 		a list of notifications to be forwarded to all currently registered controllers
	 */
	public void receiveNotification(final Notification... notifications) {

		if (isRunning()) {

			for (DeliveryWorker deliveryWorker : controllers.values()) {
				deliveryWorker.receiveNotification(notifications);
			}
		}
	}

	/**
	 * Forwards the list of notifications to the {@link DeliveryWorker} instances that every connected controller has.
	 *
	 * @param notifications
	 * 		a list of notifications to be forwarded to all currently registered controllers
	 */
	public void receiveNotification(final List<Notification> notifications) {

		if (isRunning()) {

			for (DeliveryWorker deliveryWorker : controllers.values()) {
				deliveryWorker.receiveNotification(notifications);
			}
		}
	}

	/**
	 * Forwards the list of statuses to the {@link DeliveryWorker} instances that every connected controller has.
	 *
	 * @param statuses
	 * 		a list of statuses to be forwarded to all currently registered controllers
	 */
	public void receiveStatus(final RequestStatus... statuses) {

		if (isRunning()) {

			for (DeliveryWorker deliveryWorker : controllers.values()) {
				deliveryWorker.receiveStatus(statuses);
			}
		}
	}

	/**
	 * Forwards the list of statuses to the {@link DeliveryWorker} instances that every connected controller has.
	 *
	 * @param statuses
	 * 		a list of statuses to be forwarded to all currently registered controllers
	 */
	public void receiveStatus(List<RequestStatus> statuses) {

		if (isRunning()) {

			for (DeliveryWorker deliveryWorker : controllers.values()) {
				deliveryWorker.receiveStatus(statuses);
			}
		}
	}

	/**
	 * Sends status messages to client for all node URNs containing the given {@code statusValue} and the Exceptions error
	 * message.
	 *
	 * @param nodeUrns
	 * 		the node URNs for which the error occurred
	 * @param requestId
	 * 		the request ID to which this status messages belong
	 * @param e
	 * 		the Exception that occurred
	 * @param statusValue
	 * 		the Integer value that should be sent to the controllers
	 */
	public void receiveFailureStatusMessages(List<NodeUrn> nodeUrns, long requestId, Exception e, int statusValue) {

		if (isRunning()) {

			RequestStatus requestStatus = new RequestStatus();
			requestStatus.setRequestId(requestId);

			for (NodeUrn nodeUrn : nodeUrns) {
				Status status = new Status();
				status.setNodeUrn(nodeUrn);
				status.setValue(statusValue);
				status.setMsg(e.getMessage());
				requestStatus.getStatus().add(status);
			}

			receiveStatus(Lists.<RequestStatus>newArrayList(requestStatus));
		}
	}

	/**
	 * Sends a request status to all currently connected controllers indicating that the request included an unknown node
	 * URN by setting it's return value field to -1 and passing the exception message.
	 *
	 * @param nodeUrns
	 * 		the nodeUrns that failed
	 * @param msg
	 * 		an error message that should be sent with the status update
	 * @param requestId
	 * 		the requestId
	 */
	public void receiveUnknownNodeUrnRequestStatus(final Set<NodeUrn> nodeUrns, final String msg,
												   final long requestId) {

		if (isRunning()) {

			RequestStatus requestStatus = new RequestStatus();
			requestStatus.setRequestId(requestId);

			for (NodeUrn nodeUrn : nodeUrns) {

				Status status = new Status();
				status.setMsg(msg);
				status.setNodeUrn(nodeUrn);
				status.setValue(-1);

				requestStatus.getStatus().add(status);

			}

			receiveStatus(Lists.<RequestStatus>newArrayList(requestStatus));
		}
	}

	@Override
	protected void doStart() {

		try {

			if (!isRunning()) {

				log.debug("Starting DeliveryManager...");
				executorService = Executors.newCachedThreadPool(
						new ThreadFactoryBuilder().setNameFormat("DeliveryWorker %d").build()
				);

			}

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		try {

			log.debug("Stopping delivery manager (asynchronously)...");

			reservationEnded();

			final Thread shutdownThread = new Thread("DeliveryManager-ShutdownThread") {
				@Override
				public void run() {

					for (DeliveryWorker deliveryWorker : controllers.values()) {
						deliveryWorker.experimentEnded();
					}

					// try gently to shut down executor which will succeed if no messages are to be delivered anymore
					executorService.shutdown();

					// wait until all messages have been delivered or could not be delivered
					while (!executorService.isTerminated()) {

						try {

							executorService.awaitTermination(1, TimeUnit.SECONDS);
							log.debug("Still waiting for termination of message delivery jobs...");

						} catch (InterruptedException e) {
							log.error("InterruptedException while shutting down ExecutorService: " + e, e);
						}
					}

					log.debug("Stopped delivery manager!");
				}
			};

			shutdownThread.start();

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}