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

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.tr.util.ListenableFutureMap;
import de.uniluebeck.itm.tr.util.ProgressListenableFutureMap;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DeviceAdapter extends Service {

	Set<NodeUrn> getNodeUrns();

	ListenableFutureMap<NodeUrn, NodeApiCallResult> enableNodes(Iterable<NodeUrn> nodeUrns);

	ListenableFutureMap<NodeUrn, NodeApiCallResult> enablePhysicalLinks(Map<NodeUrn, NodeUrn> sourceTargetMap);

	ListenableFutureMap<NodeUrn, NodeApiCallResult> enableVirtualLinks(Map<NodeUrn, NodeUrn> sourceTargetMap);

	ListenableFutureMap<NodeUrn, NodeApiCallResult> disableNodes(Iterable<NodeUrn> nodeUrns);

	ListenableFutureMap<NodeUrn, NodeApiCallResult> disablePhysicalLinks(Map<NodeUrn, NodeUrn> sourceTargetMap);

	ListenableFutureMap<NodeUrn, NodeApiCallResult> disableVirtualLinks(Map<NodeUrn, NodeUrn> sourceTargetMap);

	ProgressListenableFutureMap<NodeUrn, Void> flashProgram(Iterable<NodeUrn> nodeUrns, byte[] binaryImage);

	ListenableFutureMap<NodeUrn, Boolean> areNodesAlive(Iterable<NodeUrn> nodeUrns);

	ListenableFutureMap<NodeUrn, Boolean> areNodesConnected(Iterable<NodeUrn> nodeUrns);

	ListenableFutureMap<NodeUrn, Void> resetNodes(Iterable<NodeUrn> nodeUrns);

	ListenableFutureMap<NodeUrn, Void> sendMessage(Iterable<NodeUrn> nodeUrns, byte[] messageBytes);

	ListenableFutureMap<NodeUrn, Void> setChannelPipelines(Iterable<NodeUrn> nodeUrns,
														   List<Tuple<String, Multimap<String, String>>> channelHandlerConfigs);
}
