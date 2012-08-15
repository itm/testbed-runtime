package de.uniluebeck.itm.tr.wsn.federator;

import eu.wisebed.api.v3.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.v3.wsn.WSN;

import java.util.List;

public class SetChannelPipelineRunnable extends AbstractRequestRunnable {

	private final List<String> nodeUrns;

	private final List<ChannelHandlerConfiguration> channelHandlerConfigurations;

	public SetChannelPipelineRunnable(final FederatorController federatorController,
									  final WSN wsnEndpoint,
									  final long federatedRequestId,
									  final long federatorRequestId,
									  final List<String> nodeUrns,
									  final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		super(federatorController, wsnEndpoint, federatedRequestId, federatorRequestId);

		this.nodeUrns = nodeUrns;
		this.channelHandlerConfigurations = channelHandlerConfigurations;
	}

	@Override
	protected void executeRequestOnFederatedTestbed(final long federatedRequestId) {
		wsnEndpoint.setChannelPipeline(federatedRequestId, nodeUrns, channelHandlerConfigurations);
	}
}
