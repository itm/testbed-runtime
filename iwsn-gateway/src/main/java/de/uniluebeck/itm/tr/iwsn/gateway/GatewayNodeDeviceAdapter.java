package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ListenableFuture;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.tr.util.ListenableFutureMap;
import de.uniluebeck.itm.tr.util.ProgressListenableFutureMap;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.collect.Sets.newHashSet;

public class GatewayNodeDeviceAdapter implements GatewayDeviceAdapter {

	private final Device gatewayNode;

	private final Set<NodeUrn> reachableNodeUrns = newHashSet();

	public GatewayNodeDeviceAdapter(final Device gatewayNode) {
		this.gatewayNode = gatewayNode;
	}

	@Override
	public Set<NodeUrn> getNodeUrns() {
		return reachableNodeUrns;
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> enableNodes(final Set<NodeUrn> nodeUrns) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> enablePhysicalLinks(
			final Map<NodeUrn, MacAddress> sourceTargetMap) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> enableVirtualLinks(
			final Map<NodeUrn, MacAddress> sourceTargetMap) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> disableNodes(final Set<NodeUrn> nodeUrns) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> disablePhysicalLinks(
			final Map<NodeUrn, MacAddress> sourceTargetMap) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> disableVirtualLinks(
			final Map<NodeUrn, MacAddress> sourceTargetMap) {
		return null;  // TODO implement
	}

	@Override
	public ProgressListenableFutureMap<NodeUrn, Void> flashProgram(final Map<NodeUrn, byte[]> binaryImageMap) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFutureMap<NodeUrn, Boolean> areNodesAlive(final Set<NodeUrn> nodeUrns) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFutureMap<NodeUrn, Boolean> areNodesConnected(final Set<NodeUrn> nodeUrns) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFutureMap<NodeUrn, Void> resetNodes(final Set<NodeUrn> nodeUrns) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFutureMap<NodeUrn, Void> sendMessage(final Set<NodeUrn> nodeUrns, final byte[] messageBytes) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFutureMap<NodeUrn, Void> setChannelPipelines(final Set<NodeUrn> nodeUrns,
																  final List<Tuple<String, Multimap<String, String>>> channelHandlerConfigs) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFuture<State> start() {
		return null;  // TODO implement
	}

	@Override
	public State startAndWait() {
		return null;  // TODO implement
	}

	@Override
	public boolean isRunning() {
		return false;  // TODO implement
	}

	@Override
	public State state() {
		return null;  // TODO implement
	}

	@Override
	public ListenableFuture<State> stop() {
		return null;  // TODO implement
	}

	@Override
	public State stopAndWait() {
		return null;  // TODO implement
	}

	@Override
	public void addListener(final Listener listener, final Executor executor) {
		// TODO implement
	}
}
