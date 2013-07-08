package de.uniluebeck.itm.tr.common;

import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.v3.wsn.FlashProgramsConfiguration;
import eu.wisebed.api.v3.wsn.Link;
import eu.wisebed.api.v3.wsn.VirtualLink;

import java.util.Collection;
import java.util.List;

public interface WSNPreconditions {

	void checkAreNodesAliveArguments(Collection<NodeUrn> nodes);

	void checkFlashProgramsArguments(List<FlashProgramsConfiguration> flashProgramsConfigurations);

	void checkSendArguments(List<NodeUrn> nodeIds, byte[] message);

	void checkResetNodesArguments(List<NodeUrn> nodes);

	void checkSetVirtualLinkArguments(List<VirtualLink> links);

	void checkDestroyVirtualLinkArguments(List<Link> links);

	void checkDisableNodeArguments(List<NodeUrn> nodeUrns);

	void checkDisablePhysicalLinkArguments(List<Link> links);

	void checkEnableNodeArguments(List<NodeUrn> nodeUrns);

	void checkEnablePhysicalLinkArguments(List<Link> links);

	void checkSetChannelPipelineArguments(List<NodeUrn> nodes,
										  List<ChannelHandlerConfiguration> channelHandlerConfigurations);
}
