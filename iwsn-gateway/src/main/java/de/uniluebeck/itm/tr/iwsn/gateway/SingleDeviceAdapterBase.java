/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.tr.util.*;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.size;

public abstract class SingleDeviceAdapterBase extends AbstractService implements DeviceAdapter {

	protected final NodeUrn nodeUrn;

	protected SingleDeviceAdapterBase(final NodeUrn nodeUrn) {
		this.nodeUrn = nodeUrn;
	}

	@Override
	public ListenableFutureMap<NodeUrn, Boolean> areNodesAlive(final Iterable<NodeUrn> nodeUrns) {
		checkArgument(size(checkNotNull(nodeUrns)) == 1 && contains(nodeUrns, nodeUrn));
		return new SettableFutureMap<NodeUrn, Boolean>(ImmutableMap.of(nodeUrn, isNodeAlive()));
	}

	@Override
	public ListenableFutureMap<NodeUrn, Boolean> areNodesConnected(final Iterable<NodeUrn> nodeUrns) {
		checkArgument(size(checkNotNull(nodeUrns)) == 1 && contains(nodeUrns, nodeUrn));
		return new SettableFutureMap<NodeUrn, Boolean>(ImmutableMap.of(nodeUrn, isNodeConnected()));
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> disableNodes(final Iterable<NodeUrn> nodeUrns) {
		checkArgument(size(checkNotNull(nodeUrns)) == 1 && contains(nodeUrns, nodeUrn));
		return new SettableFutureMap<NodeUrn, NodeApiCallResult>(ImmutableMap.of(nodeUrn, disableNode()));
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> disablePhysicalLinks(
			final Map<NodeUrn, NodeUrn> sourceTargetMap) {
		checkArgument(sourceTargetMap.size() == 1 && sourceTargetMap.containsKey(nodeUrn));
		return new SettableFutureMap<NodeUrn, NodeApiCallResult>(
				ImmutableMap.of(nodeUrn, disablePhysicalLink(new MacAddress(sourceTargetMap.get(nodeUrn).getSuffix())))
		);
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> disableVirtualLinks(
			final Map<NodeUrn, NodeUrn> sourceTargetMap) {
		checkArgument(sourceTargetMap.size() == 1 && sourceTargetMap.containsKey(nodeUrn));
		return new SettableFutureMap<NodeUrn, NodeApiCallResult>(
				ImmutableMap.of(nodeUrn, disableVirtualLink(new MacAddress(sourceTargetMap.get(nodeUrn).getSuffix())))
		);
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> enableNodes(final Iterable<NodeUrn> nodeUrns) {
		checkArgument(size(checkNotNull(nodeUrns)) == 1 && contains(nodeUrns, nodeUrn));
		return new SettableFutureMap<NodeUrn, NodeApiCallResult>(ImmutableMap.of(nodeUrn, enableNode()));
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> enablePhysicalLinks(
			final Map<NodeUrn, NodeUrn> sourceTargetMap) {
		checkArgument(sourceTargetMap.size() == 1 && sourceTargetMap.containsKey(nodeUrn));
		return new SettableFutureMap<NodeUrn, NodeApiCallResult>(
				ImmutableMap.of(nodeUrn, enablePhysicalLink(new MacAddress(sourceTargetMap.get(nodeUrn).getSuffix())))
		);
	}

	@Override
	public ListenableFutureMap<NodeUrn, NodeApiCallResult> enableVirtualLinks(
			final Map<NodeUrn, NodeUrn> sourceTargetMap) {
		checkArgument(sourceTargetMap.size() == 1 && sourceTargetMap.containsKey(nodeUrn));
		return new SettableFutureMap<NodeUrn, NodeApiCallResult>(
				ImmutableMap.of(nodeUrn, enableVirtualLink(new MacAddress(sourceTargetMap.get(nodeUrn).getSuffix())))
		);
	}

	@Override
	public ProgressListenableFutureMap<NodeUrn, Void> flashProgram(final Iterable<NodeUrn> nodeUrns,
																   byte[] binaryImage) {
		checkArgument(size(checkNotNull(nodeUrns)) == 1 && contains(nodeUrns, nodeUrn));
		return new ProgressSettableFutureMap<NodeUrn, Void>(ImmutableMap.of(nodeUrn, flashProgram(binaryImage)));
	}

	@Override
	public Set<NodeUrn> getNodeUrns() {
		return isRunning() ? ImmutableSet.of(nodeUrn) : ImmutableSet.<NodeUrn>of();
	}

	@Override
	public ListenableFutureMap<NodeUrn, Void> resetNodes(final Iterable<NodeUrn> nodeUrns) {
		checkArgument(size(checkNotNull(nodeUrns)) == 1 && contains(nodeUrns, nodeUrn));
		return new SettableFutureMap<NodeUrn, Void>(ImmutableMap.of(nodeUrn, resetNode()));
	}

	@Override
	public ListenableFutureMap<NodeUrn, Void> sendMessage(final Iterable<NodeUrn> nodeUrns, final byte[] messageBytes) {
		checkArgument(size(checkNotNull(nodeUrns)) == 1 && contains(nodeUrns, nodeUrn));
		return new SettableFutureMap<NodeUrn, Void>(ImmutableMap.of(nodeUrn, sendMessage(messageBytes)));
	}

	@Override
	public ListenableFutureMap<NodeUrn, Void> setChannelPipelines(final Iterable<NodeUrn> nodeUrns,
																  final List<Tuple<String, Multimap<String, String>>> channelHandlerConfigs) {
		checkArgument(size(checkNotNull(nodeUrns)) == 1 && contains(nodeUrns, nodeUrn));
		return new SettableFutureMap<NodeUrn, Void>(ImmutableMap.of(nodeUrn, setChannelPipeline(channelHandlerConfigs))
		);
	}

	protected abstract ListenableFuture<NodeApiCallResult> enableNode();

	protected abstract ListenableFuture<NodeApiCallResult> enablePhysicalLink(MacAddress targetMacAddress);

	protected abstract ListenableFuture<NodeApiCallResult> disableVirtualLink(MacAddress targetMacAddress);

	protected abstract ListenableFuture<NodeApiCallResult> disableNode();

	protected abstract ListenableFuture<NodeApiCallResult> disablePhysicalLink(MacAddress targetMacAddress);

	protected abstract ProgressListenableFuture<Void> flashProgram(byte[] binaryImage);

	protected abstract ListenableFuture<Boolean> isNodeAlive();

	protected abstract ListenableFuture<Boolean> isNodeConnected();

	protected abstract ListenableFuture<Void> resetNode();

	protected abstract ListenableFuture<Void> sendMessage(byte[] messageBytes);

	protected abstract ListenableFuture<NodeApiCallResult> enableVirtualLink(MacAddress targetMacAddress);

	protected abstract ListenableFuture<Void> setChannelPipeline(
			List<Tuple<String, Multimap<String, String>>> channelHandlerConfigs);
}