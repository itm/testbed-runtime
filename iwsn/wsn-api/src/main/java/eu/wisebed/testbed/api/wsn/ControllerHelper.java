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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.internal.Nullable;
import eu.wisebed.testbed.api.wsn.v211.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Helper class that manages a set of {@link eu.wisebed.testbed.api.wsn.v211.Controller} Web Service endpoints and
 * allows to asynchronously deliver messages and request status updates to them in parallel. If the delivery to a
 * recipient is repeatedly ({@link eu.wisebed.testbed.api.wsn.ControllerHelper#RETRIES} times) impossible due to
 * whatever reason, the recipient is removed from the list of recipients. Between every try there's a pause of {@link
 * eu.wisebed.testbed.api.wsn.ControllerHelper#RETRY_TIMEOUT} in time unit {@link eu.wisebed.testbed.api.wsn.ControllerHelper#RETRY_TIMEUNIT}.
 */
public class ControllerHelper {

	private static final Logger log = LoggerFactory.getLogger(ControllerHelper.class);

	public static final int RETRIES = 3;

	public static final long RETRY_TIMEOUT = 5;

	public static final TimeUnit RETRY_TIMEUNIT = TimeUnit.SECONDS;

	private static final int DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE = 1000;

	private final int maximumDeliveryQueueSize;

	public int getControllerCount() {
		return controllerEndpoints.size();
	}

	private abstract class AbstractDeliverRunnable implements Runnable {

		private int retries = 0;

		private final Map.Entry<String, Controller> controllerEntry;

		protected AbstractDeliverRunnable(Map.Entry<String, Controller> controllerEntry) {
			this.controllerEntry = controllerEntry;
		}

		@Override
		public final void run() {

			if (retries < RETRIES) {

				retries++;
				try {

					deliver(controllerEntry.getValue());

				} catch (Exception e) {
					log.warn("Could not deliver message / request status to Controller at endpoint URL {}. "
							+ "Trying again in {} {}",
							new Object[]{
									controllerEntry.getKey(),
									RETRY_TIMEOUT,
									RETRY_TIMEUNIT.toString().toLowerCase()
							}
					);
					executorService.schedule(this, RETRY_TIMEOUT, RETRY_TIMEUNIT);
				}
			} else {

				log.warn("Repeatedly (tried {} times) could not deliver message to Controller at endpoint URL {}. "
						+ "Removing controller endpoint...",
						new Object[]{
								RETRIES, controllerEntry.getKey()
						}
				);

				removeController(controllerEntry.getKey());

			}
		}

		protected abstract void deliver(Controller controller);

	}

	private class DeliverRequestStatusRunnable extends AbstractDeliverRunnable {

		private RequestStatus requestStatus;

		private DeliverRequestStatusRunnable(Map.Entry<String, Controller> controllerEntry,
											 RequestStatus requestStatus) {
			super(controllerEntry);
			this.requestStatus = requestStatus;
		}

		@Override
		protected void deliver(Controller controller) {
			controller.receiveStatus(requestStatus);
		}

	}

	private class DeliverMessageRunnable extends AbstractDeliverRunnable {

		private Message msg;

		private DeliverMessageRunnable(Map.Entry<String, Controller> controllerEntry, Message msg) {
			super(controllerEntry);
			this.msg = msg;
		}

		@Override
		protected void deliver(Controller controller) {
			controller.receive(msg);
		}

	}

	/**
	 * The set of {@link eu.wisebed.testbed.api.wsn.v211.Controller} instances currently listening to this WSN service
	 * instance. Maps from the endpoint URL to an instantiated endpoint proxy.
	 */
	private final Map<String, Controller> controllerEndpoints = new HashMap<String, Controller>();

	/**
	 * Used to deliver messages and request status updates in parallel.
	 */
	private final ScheduledThreadPoolExecutor executorService;

	public ControllerHelper() {
		this(DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE);
	}

	/**
	 * Constructs a new {@link ControllerHelper} instance.
	 */
	public ControllerHelper(@Nullable Integer maximumDeliveryQueueSize) {
		this.maximumDeliveryQueueSize =
				maximumDeliveryQueueSize == null ? DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE : maximumDeliveryQueueSize;
		executorService = new ScheduledThreadPoolExecutor(1,
				new ThreadFactoryBuilder().setNameFormat("ControllerHelper-Thread %d").build()
		);
		executorService.setMaximumPoolSize(1);
	}

	/**
	 * Adds a Controller service endpoint URL to the list of recipients.
	 *
	 * @param controllerEndpointUrl the endpoint URL of a {@link eu.wisebed.testbed.api.wsn.v211.Controller} Web Service
	 *                              instance
	 */
	public void addController(String controllerEndpointUrl) {
		Controller controller = WSNServiceHelper.getControllerService(controllerEndpointUrl);
		synchronized (controllerEndpoints) {
			controllerEndpoints.put(controllerEndpointUrl, controller);
		}
		executorService.setMaximumPoolSize(getControllerCount());
	}

	/**
	 * Removes a Controller service endpoint URL from the list of recipients.
	 *
	 * @param controllerEndpointUrl the endpoint URL of a {@link eu.wisebed.testbed.api.wsn.v211.Controller} Web Service
	 *                              instance
	 */
	public void removeController(String controllerEndpointUrl) {
		synchronized (controllerEndpoints) {
			controllerEndpoints.remove(controllerEndpointUrl);
		}
		executorService.setMaximumPoolSize(getControllerCount());
	}

	/**
	 * Delivers {@code message} to all recipients.
	 *
	 * @param message the {@link eu.wisebed.testbed.api.wsn.v211.Message} instance to deliver
	 */
	public void receive(Message message) {

		if (executorService.getQueue().size() > maximumDeliveryQueueSize) {
			log.error("More than {} messages in the delivery queue. Dropping message!", maximumDeliveryQueueSize
			);
			// TODO find more elegant solution on how to cope with too much load
			return;
		}

		synchronized (controllerEndpoints) {
			for (Map.Entry<String, Controller> controllerEntry : controllerEndpoints.entrySet()) {
				log.debug("Delivering message to endpoint URL {}", controllerEntry.getKey());
				executorService.submit(new DeliverMessageRunnable(controllerEntry, message));
			}
		}
	}

	/**
	 * Delivers {@code requestStatus} to all recipients.
	 *
	 * @param requestStatus the {@link eu.wisebed.testbed.api.wsn.v211.RequestStatus} instance to deliver
	 */
	public void receiveStatus(RequestStatus requestStatus) {

		if (executorService.getQueue().size() > maximumDeliveryQueueSize) {
			log.error("More than {} messages in the delivery queue. Dropping requestStatus!", maximumDeliveryQueueSize);
			// TODO find more elegant solution on how to cope with too much load
			return;
		}

		synchronized (controllerEndpoints) {
			for (Map.Entry<String, Controller> controllerEntry : controllerEndpoints.entrySet()) {
				log.debug("Delivering request status with requestID {} to endpoint URL {}",
						requestStatus.getRequestId(), controllerEntry.getKey()
				);
				executorService.submit(new DeliverRequestStatusRunnable(controllerEntry, requestStatus));
			}
		}
	}

	public void receiveUnkownNodeUrnRequestStatus(final UnknownNodeUrnException_Exception e, final String requestId) {

		for (String nodeUrn : e.getFaultInfo().getUrn()) {

			Status status = new Status();
			status.setMsg(e.getFaultInfo().getMessage());
			status.setNodeId(nodeUrn);
			status.setValue(-1);

			RequestStatus requestStatus = new RequestStatus();
			requestStatus.setRequestId(requestId);
			requestStatus.getStatus().add(status);

			receiveStatus(requestStatus);

		}

	}

	public int getMaximumDeliveryQueueSize() {
		return maximumDeliveryQueueSize;
	}
}
