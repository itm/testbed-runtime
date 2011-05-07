package de.uniluebeck.itm.tr.wsn.federator;

import eu.wisebed.api.wsn.WSN;

abstract class AbstractRequestRunnable implements Runnable {

	private FederatorController federatorController;

	protected final WSN wsnEndpoint;

	protected final String federatorRequestId;

	protected AbstractRequestRunnable(FederatorController federatorController, WSN wsnEndpoint,
									  String federatorRequestId) {

		this.federatorController = federatorController;
		this.wsnEndpoint = wsnEndpoint;
		this.federatorRequestId = federatorRequestId;
	}

	protected void done(String federatedRequestId) {
		federatorController.addRequestIdMapping(federatedRequestId, federatorRequestId);
	}
}