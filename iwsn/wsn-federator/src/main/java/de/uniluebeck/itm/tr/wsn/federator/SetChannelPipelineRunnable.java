package de.uniluebeck.itm.tr.wsn.federator;

import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.WSN;

import java.util.List;
import java.util.concurrent.Callable;

public class SetChannelPipelineRunnable extends AbstractRequestRunnable {

	private final List<String> nodeUrns;

	private List<ChannelHandlerConfiguration> channelHandlerConfigurations;

	public SetChannelPipelineRunnable(final FederatorController federatorController, final WSN wsnEndpoint,
									  final String federatorRequestId, final List<String> nodeUrns,
									  final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		super(federatorController, wsnEndpoint, federatorRequestId);

		this.nodeUrns = nodeUrns;
		this.channelHandlerConfigurations = channelHandlerConfigurations;
	}

	@Override
	public void run() {
		synchronized (wsnEndpoint) {
			done(wsnEndpoint.setChannelPipeline(nodeUrns, channelHandlerConfigurations));
		}
	}
}
