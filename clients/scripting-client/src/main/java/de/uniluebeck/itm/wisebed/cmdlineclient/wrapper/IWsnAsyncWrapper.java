package de.uniluebeck.itm.wisebed.cmdlineclient.wrapper;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.JobResult;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;

public interface IWsnAsyncWrapper {

	public abstract Future<Void> addController(final String controllerEndpointUrl);

	public abstract Future<Void> removeController(final String controllerEndpointUrl);

	public abstract Future<JobResult> send(List<String> nodeIds, Message message, int timeout, TimeUnit timeUnit);

	public abstract Future<JobResult> setChannelPipeline(final List<String> nodes,
			final List<ChannelHandlerConfiguration> channelHandlerConfigurations, int timeout, TimeUnit timeUnit);

	public abstract Future<?> getVersion();

	public abstract Future<JobResult> areNodesAlive(List<String> nodes, int timeout, TimeUnit timeUnit);

	@SuppressWarnings("unused")
	public abstract Future<String> describeCapabilities(String capability);

	public abstract Future<JobResult> destroyVirtualLink(String sourceNode, String targetNode, int timeout, TimeUnit timeUnit);

	public abstract Future<JobResult> disableNode(String node, int timeout, TimeUnit timeUnit);

	public abstract Future<JobResult> disablePhysicalLink(String nodeA, String nodeB, int timeout, TimeUnit timeUnit);

	@SuppressWarnings("unused")
	public abstract Future<JobResult> enableNode(String node, int timeout, TimeUnit timeUnit);

	public abstract Future<JobResult> enablePhysicalLink(String nodeA, String nodeB, int timeout, TimeUnit timeUnit);

	public abstract Future<JobResult> flashPrograms(List<String> nodeIds, List<Integer> programIndices, List<Program> programs, int timeout,
			TimeUnit timeUnit);

	public abstract Future<List<String>> getFilters();

	public abstract Future<String> getNetwork();

	public abstract Future<List<ChannelHandlerDescription>> getSupportedChannelHandlers();

	public abstract Future<JobResult> resetNodes(List<String> nodes, int timeout, TimeUnit timeUnit);

	public abstract Future<JobResult> setVirtualLink(String sourceNode, String targetNode, String remoteServiceInstance, List<String> parameters,
			List<String> filters, int timeout, TimeUnit timeUnit);

}