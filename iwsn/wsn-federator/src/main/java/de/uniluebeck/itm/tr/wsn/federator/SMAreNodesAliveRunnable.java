package de.uniluebeck.itm.tr.wsn.federator;

import eu.wisebed.testbed.api.wsn.v23.SessionManagement;

import java.util.List;

class SMAreNodesAliveRunnable implements Runnable {

	private final FederatorController federatorController;

	private final String federatorRequestId;

	private final SessionManagement smEndpoint;

	private final List<String> nodes;

	SMAreNodesAliveRunnable(final FederatorController federatorController,
							final SessionManagement smEndpoint,
							final String federatorRequestId,
							final List<String> nodes) {

		this.federatorController = federatorController;
		this.smEndpoint = smEndpoint;
		this.federatorRequestId = federatorRequestId;
		this.nodes = nodes;
	}

	@Override
	public void run() {
		// instance smEndpoint is potentially not thread-safe!!!
		synchronized (smEndpoint) {
			done(smEndpoint.areNodesAlive(nodes, federatorController.getControllerEndpointUrl()));
		}
	}

	private void done(String federatedRequestId) {
		federatorController.addRequestIdMapping(federatedRequestId, federatorRequestId);
	}
}