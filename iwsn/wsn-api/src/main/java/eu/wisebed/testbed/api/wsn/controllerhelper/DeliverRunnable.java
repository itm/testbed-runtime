package eu.wisebed.testbed.api.wsn.controllerhelper;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wisebed.api.controller.Controller;

/**
 * Base class for message delivery. Retries message delivery {@link ControllerHelperConstants#RETRIES} times if an error
 * occurs before "giving up". If delivery fails repeatedly the controller for which it fails is removed.
 */
abstract class DeliverRunnable implements Runnable {

	protected final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	protected final ScheduledThreadPoolExecutor scheduler;

	private final DeliverFailureListener failureListener;

	protected final String controllerEndpointUrl;

	protected final Controller controllerEndpoint;

	protected int retries = 0;

	protected DeliverRunnable(final ScheduledThreadPoolExecutor scheduler,
							  final DeliverFailureListener failureListener, final String controllerEndpointUrl,
							  final Controller controllerEndpoint) {

		this.scheduler = scheduler;
		this.failureListener = failureListener;
		this.controllerEndpointUrl = controllerEndpointUrl;
		this.controllerEndpoint = controllerEndpoint;
	}

	@Override
	public final void run() {

		if (retries < ControllerHelperConstants.RETRIES) {

			retries++;
			try {

				// try to deliver the message
				deliver(controllerEndpoint);

			} catch (Exception e) {

				// delivery failed
				log.warn("Could not deliver message / request status to Controller at endpoint URL {}. "
						+ "Trying again in {} {}"
						+ "Error = {}",
						new Object[]{
								controllerEndpointUrl,
								ControllerHelperConstants.RETRY_TIMEOUT,
								ControllerHelperConstants.RETRY_TIME_UNIT.toString().toLowerCase(),
								e
						}
				);

				// reschedule delivery
				scheduler.schedule(
						this,
						ControllerHelperConstants.RETRY_TIMEOUT,
						ControllerHelperConstants.RETRY_TIME_UNIT
				);
			}
		} else {

			// if number of delivery retries is larger than maximum do not try delivery again
			log.warn("Repeatedly (tried {} times) could not deliver message to Controller at endpoint URL {}. "
					+ "Removing controller endpoint...",
					new Object[]{ControllerHelperConstants.RETRIES, controllerEndpointUrl}
			);

			// delete controller from list of controllers as obviously this controller is offline
			failureListener.deliveryFailed(controllerEndpointUrl);
		}
	}

	/**
	 * Does the actual delivery logic to the client <b>exactly one time</b>.
	 *
	 * @param controller the Controller proxy to the client to deliver to
	 */
	protected abstract void deliver(Controller controller);

}