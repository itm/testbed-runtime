package de.uniluebeck.itm.tr.wsn.federator;

import java.util.List;

import eu.wisebed.api.wsn.WSN;

class ResetNodesRunnable extends AbstractRequestRunnable {

	private List<String> nodes;

	ResetNodesRunnable(FederatorController federatorController, WSN wsnEndpoint, String federatorRequestId,
							   List<String> nodes) {

		super(federatorController, wsnEndpoint, federatorRequestId);

		this.nodes = nodes;
	}

	@Override
	public void run() {
		// instance wsnEndpoint is potentially not thread-safe!!!
		synchronized (wsnEndpoint) {
			done(wsnEndpoint.resetNodes(nodes));
		}
	}
}