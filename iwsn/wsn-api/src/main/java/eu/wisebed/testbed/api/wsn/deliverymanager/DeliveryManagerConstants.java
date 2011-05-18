package eu.wisebed.testbed.api.wsn.deliverymanager;

import java.util.concurrent.TimeUnit;

class DeliveryManagerConstants {

	/**
	 * Number of message delivery retries. If delivering a message fails, e.g., due to temporarily unavailable network
	 * connection on either server or client side the delivery will be retried after {@link
	 * DeliveryManagerConstants#RETRY_TIMEOUT} time units (cf. {@link DeliveryManagerConstants#RETRY_TIME_UNIT}).
	 */
	public static final int RETRIES = 3;

	/**
	 * Number of time units to wait for retry of message delivery (cf. {@link DeliveryManagerConstants#RETRY_TIME_UNIT}).
	 */
	public static final long RETRY_TIMEOUT = 5;

	/**
	 * The time unit of the {@link DeliveryManagerConstants#RETRY_TIMEOUT}.
	 */
	public static final TimeUnit RETRY_TIME_UNIT = TimeUnit.SECONDS;

	/**
	 * The default maximum number of messages to be held in a delivery queue before new messages are discarded.
	 */
	public static final int DEFAULT_MAXIMUM_DELIVERY_QUEUE_SIZE = 1000;


}
