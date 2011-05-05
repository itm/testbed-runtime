package de.uniluebeck.itm.tr.wsn.federator;

import eu.wisebed.testbed.api.wsn.v23.Message;
import eu.wisebed.testbed.api.wsn.v23.WSN;

import java.util.List;

class SendRunnable extends AbstractRequestRunnable {

	private List<String> nodeIds;

	private Message message;

	SendRunnable(FederatorController federatorController, WSN wsnEndpoint, String federatorRequestId,
						 List<String> nodeIds, Message message) {

		super(federatorController, wsnEndpoint, federatorRequestId);

		this.nodeIds = nodeIds;
		this.message = message;
	}

	@Override
	public void run() {
		// instance wsnEndpoint is potentially not thread-safe!!!
		synchronized (wsnEndpoint) {
			done(wsnEndpoint.send(nodeIds, message));
		}
	}

}