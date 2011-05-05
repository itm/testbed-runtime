package de.uniluebeck.itm.tr.wsn.federator;

import eu.wisebed.testbed.api.wsn.v23.WSN;

class DestroyVirtualLinkRunnable extends AbstractRequestRunnable {

	private String sourceNode;

	private String targetNode;

	DestroyVirtualLinkRunnable(FederatorController federatorController, WSN wsnEndpoint,
									   String federatorRequestId,
									   String sourceNode, String targetNode) {

		super(federatorController, wsnEndpoint, federatorRequestId);

		this.sourceNode = sourceNode;
		this.targetNode = targetNode;
	}

	@Override
	public void run() {
		// instance wsnEndpoint is potentially not thread-safe!!!
		synchronized (wsnEndpoint) {
			done(wsnEndpoint.destroyVirtualLink(sourceNode, targetNode));
		}
	}
}