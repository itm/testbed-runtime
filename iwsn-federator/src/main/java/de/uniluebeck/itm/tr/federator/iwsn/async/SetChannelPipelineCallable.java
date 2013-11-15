package de.uniluebeck.itm.tr.federator.iwsn.async;

import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.v3.wsn.WSN;

import java.util.List;
import java.util.concurrent.Callable;

public class SetChannelPipelineCallable implements Callable<Void> {

	private final WSN wsnEndpoint;

	private final long requestId;

	private final List<NodeUrn> nodeUrns;

	private final List<ChannelHandlerConfiguration> channelHandlerConfigurations;

	public SetChannelPipelineCallable(final WSN wsnEndpoint,
									  final long requestId,
									  final List<NodeUrn> nodeUrns,
									  final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {
		this.wsnEndpoint = wsnEndpoint;
		this.requestId = requestId;
		this.nodeUrns = nodeUrns;
		this.channelHandlerConfigurations = channelHandlerConfigurations;
	}

	@Override
	public Void call() throws Exception {
		wsnEndpoint.setChannelPipeline(requestId, nodeUrns, channelHandlerConfigurations);
		return null;
	}
}
