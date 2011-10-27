package de.uniluebeck.itm.wisebed.cmdlineclient.wrapper;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.AsyncJobObserver;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.Job;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.JobResult;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.JobResultListener;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.WSN;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;


public class WSNAsyncWrapper {

	private static class FutureJobResultListener implements JobResultListener {

		private SettableFuture<JobResult> future;

		private FutureJobResultListener(final SettableFuture<JobResult> future) {
			this.future = future;
		}

		@Override
		public void receiveJobResult(final JobResult result) {
			future.set(result);
		}

		@Override
		public void receiveMessage(final Message msg) throws IOException {
			// nothing to do
		}

		@Override
		public void timeout() {
			future.setException(new TimeoutException());
		}
	}

	private ExecutorService executor = Executors.newCachedThreadPool();

	private AsyncJobObserver jobs = new AsyncJobObserver(10, TimeUnit.SECONDS);

	private WSN wsn;

	private WSNAsyncWrapper(final WSN wsn) {
		this.wsn = wsn;
	}

	public static WSNAsyncWrapper of(final WSN wsn) {
		return new WSNAsyncWrapper(wsn);
	}

	public void receive(Collection<RequestStatus> requestStatuses) {
		for (RequestStatus requestStatus : requestStatuses) {
			jobs.receive(requestStatus);
		}
	}

	public void receive(RequestStatus requestStatus) {
		jobs.receive(requestStatus);
	}

	public Future<Void> addController(final String controllerEndpointUrl) {
		return executor.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				wsn.addController(controllerEndpointUrl);
				return null;
			}
		}
		);
	}

	public Future<Void> removeController(final String controllerEndpointUrl) {
		return executor.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				wsn.removeController(controllerEndpointUrl);
				return null;
			}
		}
		);
	}

	public Future<JobResult> send(List<String> nodeIds, Message message, int timeout, TimeUnit timeUnit) {
		SettableFuture<JobResult> future = SettableFuture.create();
		Job job = new Job("send", wsn.send(nodeIds, message), nodeIds, Job.JobType.send);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, timeout, timeUnit);
		return future;
	}

	public Future<JobResult> setChannelPipeline(final List<String> nodes,
												final List<ChannelHandlerConfiguration> channelHandlerConfigurations,
												int timeout, TimeUnit timeUnit) {

		final SettableFuture<JobResult> future = SettableFuture.create();
		final Job job = new Job(
				"setChannelPipeline",
				wsn.setChannelPipeline(nodes, channelHandlerConfigurations),
				nodes,
				Job.JobType.setChannelPipeline
		);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, timeout, timeUnit);
		return future;
	}

	public Future<?> getVersion() {
		return executor.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return wsn.getVersion();
			}
		}
		);
	}

	public Future<JobResult> areNodesAlive(List<String> nodes, int timeout, TimeUnit timeUnit) {
		SettableFuture<JobResult> future = SettableFuture.create();
		Job job = new Job("areNodesAlive", wsn.areNodesAlive(nodes), nodes, Job.JobType.areNodesAlive);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, timeout, timeUnit);
		return future;
	}

	@SuppressWarnings("unused")
	public Future<String> describeCapabilities(String capability) {
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	public Future<JobResult> destroyVirtualLink(String sourceNode, String targetNode, int timeout, TimeUnit timeUnit) {
		SettableFuture<JobResult> future = SettableFuture.create();
		Job job = new Job("destroyVirtualLink", wsn.destroyVirtualLink(sourceNode, targetNode), sourceNode,
				Job.JobType.destroyVirtualLink
		);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, timeout, timeUnit);
		return future;
	}

	public Future<JobResult> disableNode(String node, int timeout, TimeUnit timeUnit) {
		SettableFuture<JobResult> future = SettableFuture.create();
		Job job = new Job("disableNode", wsn.disableNode(node), node, Job.JobType.disableNode);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, timeout, timeUnit);
		return future;
	}

	public Future<JobResult> disablePhysicalLink(String nodeA, String nodeB, int timeout, TimeUnit timeUnit) {
		SettableFuture<JobResult> future = SettableFuture.create();
		Job job = new Job("disablePhysicalLink", wsn.disablePhysicalLink(nodeA, nodeB), nodeA,
				Job.JobType.disablePhysicalLink
		);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, timeout, timeUnit);
		return future;
	}

	@SuppressWarnings("unused")
	public Future<JobResult> enableNode(String node, int timeout, TimeUnit timeUnit) {
		SettableFuture<JobResult> future = SettableFuture.create();
		Job job = new Job("enableNode", wsn.enableNode(node), node, Job.JobType.enableNode);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, timeout, timeUnit);
		return future;
	}

	public Future<JobResult> enablePhysicalLink(String nodeA, String nodeB, int timeout, TimeUnit timeUnit) {
		SettableFuture<JobResult> future = SettableFuture.create();
		Job job = new Job("enablePhysicalLink", wsn.enablePhysicalLink(nodeA, nodeB), nodeA,
				Job.JobType.enablePhysicalLink
		);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, timeout, timeUnit);
		return future;
	}

	public Future<JobResult> flashPrograms(List<String> nodeIds, List<Integer> programIndices, List<Program> programs,
										   int timeout, TimeUnit timeUnit) {

		SettableFuture<JobResult> future = SettableFuture.create();
		Job job = new Job("flashPrograms", wsn.flashPrograms(nodeIds, programIndices, programs), nodeIds,
				Job.JobType.flashPrograms
		);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, timeout, timeUnit);
		return future;
	}

	public Future<List<String>> getFilters() {
		return executor.submit(new Callable<List<String>>() {
			@Override
			public List<String> call() throws Exception {
				return wsn.getFilters();
			}
		}
		);
	}

	public Future<String> getNetwork() {
		return executor.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return wsn.getNetwork();
			}
		}
		);
	}

	public Future<List<ChannelHandlerDescription>> getSupportedChannelHandlers() {
		return executor.submit(new Callable<List<ChannelHandlerDescription>>() {
			@Override
			public List<ChannelHandlerDescription> call() throws Exception {
				return wsn.getSupportedChannelHandlers();
			}
		});
	}

	public Future<JobResult> resetNodes(List<String> nodes, int timeout, TimeUnit timeUnit) {
		SettableFuture<JobResult> future = SettableFuture.create();
		Job job = new Job("resetNodes", wsn.resetNodes(nodes), nodes, Job.JobType.resetNodes);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, timeout, timeUnit);
		return future;
	}

	public Future<JobResult> setVirtualLink(String sourceNode, String targetNode, String remoteServiceInstance,
											List<String> parameters, List<String> filters, int timeout,
											TimeUnit timeUnit) {

		SettableFuture<JobResult> future = SettableFuture.create();
		Job job = new Job(
				"setVirtualLink",
				wsn.setVirtualLink(
						sourceNode,
						targetNode,
						remoteServiceInstance,
						parameters,
						filters
				),
				sourceNode,
				Job.JobType.setVirtualLink
		);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, timeout, timeUnit);
		return future;
	}

}
