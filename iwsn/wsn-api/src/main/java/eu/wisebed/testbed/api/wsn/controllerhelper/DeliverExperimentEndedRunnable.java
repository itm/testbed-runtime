package eu.wisebed.testbed.api.wsn.controllerhelper;

import eu.wisebed.testbed.api.wsn.v23.Controller;

import java.util.concurrent.ScheduledThreadPoolExecutor;

class DeliverExperimentEndedRunnable extends DeliverRunnable {

	DeliverExperimentEndedRunnable(final ScheduledThreadPoolExecutor scheduler,
								   final DeliverFailureListener failureListener,
								   final String controllerEndpointUrl,
								   final Controller controllerEndpoint) {

		super(scheduler, failureListener, controllerEndpointUrl, controllerEndpoint);
	}

	@Override
	protected void deliver(final Controller controller) {
		controller.experimentEnded();
	}
}