package de.uniluebeck.itm.tr.wsn.federator;

import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.v3.wsn.WSN;

import java.util.List;

public class SetChannelPipelineRunnable extends AbstractRequestRunnable {

	private final List<NodeUrn> nodeUrns;

	private final List<ChannelHandlerConfiguration> channelHandlerConfigurations;

	public SetChannelPipelineRunnable(final FederatorController federatorController,
									  final WSN wsnEndpoint,
									  final long federatedRequestId,
									  final long federatorRequestId,
									  final List<NodeUrn> nodeUrns,
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
