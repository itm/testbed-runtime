package de.uniluebeck.itm.tr.wsn.federator;

import eu.wisebed.testbed.api.wsn.v23.WSN;

import java.util.List;

class SetVirtualLinkRunnable extends AbstractRequestRunnable {

	private String sourceNode;

	private String targetNode;

	private String remoteServiceInstance;

	private List<String> parameters;

	private List<String> filters;

	SetVirtualLinkRunnable(FederatorController federatorController, WSN wsnEndpoint,
						   String federatorRequestId,
						   String sourceNode, String targetNode, String remoteServiceInstance,
						   List<String> parameters,
						   List<String> filters) {

		super(federatorController, wsnEndpoint, federatorRequestId);

		this.sourceNode = sourceNode;
		this.targetNode = targetNode;
		this.remoteServiceInstance = remoteServiceInstance;
		this.parameters = parameters;
		this.filters = filters;
	}

	@Override
	public void run() {
		// instance wsnEndpoint is potentially not thread-safe!!!
		synchronized (wsnEndpoint) {
			done(wsnEndpoint.setVirtualLink(sourceNode, targetNode, remoteServiceInstance, parameters, filters));
		}
	}
}