package eu.wisebed.testbed.api.wsn.controllerhelper;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import eu.wisebed.api.controller.Controller;

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