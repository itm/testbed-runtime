package eu.wisebed.testbed.api.wsn.controllerhelper;

import eu.wisebed.testbed.api.wsn.v23.Controller;
import eu.wisebed.testbed.api.wsn.v23.Message;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

class DeliverMessageRunnable extends DeliverRunnable {

	private List<Message> msg;

	DeliverMessageRunnable(final ScheduledThreadPoolExecutor scheduler,
						   final DeliverFailureListener failureListener,
						   final String controllerEndpointUrl,
						   final Controller controllerEndpoint,
						   final List<Message> msg) {

		super(scheduler, failureListener, controllerEndpointUrl, controllerEndpoint);
		this.msg = msg;
	}

	@Override
	protected void deliver(Controller controller) {
		controller.receive(msg);
	}
}