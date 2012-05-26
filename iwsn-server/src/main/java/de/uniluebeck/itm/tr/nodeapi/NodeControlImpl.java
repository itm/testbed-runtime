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

package de.uniluebeck.itm.tr.nodeapi;

import com.google.common.util.concurrent.SettableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;


public class NodeControlImpl implements NodeControl {

	private final String nodeUrn;

	private final NodeApi nodeApi;

	public NodeControlImpl(final String nodeUrn, NodeApi nodeApi) {
		this.nodeUrn = nodeUrn;
		this.nodeApi = nodeApi;
	}

	@Override
	public Future<NodeApiCallResult> enableNode() {

		int requestId = nodeApi.nextRequestId();
		ByteBuffer buffer = Packets.NodeControl.newEnableNodePacket(
				requestId
		);
		SettableFuture<NodeApiCallResult> future = SettableFuture.<NodeApiCallResult>create();
		nodeApi.sendToNode(requestId, future, buffer);
		return future;
	}

	@Override
	public Future<NodeApiCallResult> disableNode() {

		int requestId = nodeApi.nextRequestId();
		ByteBuffer buffer = Packets.NodeControl.newDisableNodePacket(
				requestId
		);
		SettableFuture<NodeApiCallResult> future = SettableFuture.<NodeApiCallResult>create();
		nodeApi.sendToNode(requestId, future, buffer);
		return future;
	}

	@Override
	public Future<NodeApiCallResult> resetNode(int time) {

		int requestId = nodeApi.nextRequestId();
		ByteBuffer buffer = Packets.NodeControl.newResetNodePacket(
				requestId, time
		);
		SettableFuture<NodeApiCallResult> future = SettableFuture.<NodeApiCallResult>create();
		nodeApi.sendToNode(requestId, future, buffer);
		return future;
	}

	@Override
	public Future<NodeApiCallResult> setStartTime(int time) {

		int requestId = nodeApi.nextRequestId();
		ByteBuffer buffer = Packets.NodeControl.newSetStartTimePacket(
				requestId, time
		);
		SettableFuture<NodeApiCallResult> future = SettableFuture.<NodeApiCallResult>create();
		nodeApi.sendToNode(requestId, future, buffer);
		return future;
	}

	@Override
	public Future<NodeApiCallResult> setVirtualID(long virtualNodeID) {

		int requestId = nodeApi.nextRequestId();
		ByteBuffer buffer = Packets.NodeControl.newSetVirtualIDPacket(
				requestId, virtualNodeID
		);
		SettableFuture<NodeApiCallResult> future = SettableFuture.<NodeApiCallResult>create();
		nodeApi.sendToNode(requestId, future, buffer);
		return future;
	}

	@Override
	public Future<NodeApiCallResult> getVirtualID() {

		int requestId = nodeApi.nextRequestId();
		ByteBuffer buffer = Packets.NodeControl.newGetIDPacket(
				requestId
		);
		SettableFuture<NodeApiCallResult> future = SettableFuture.<NodeApiCallResult>create();
		nodeApi.sendToNode(requestId, future, buffer);
		return future;
	}

	@Override
	public Future<NodeApiCallResult> areNodesAlive() {

		int requestId = nodeApi.nextRequestId();
		ByteBuffer buffer = Packets.NodeControl.newAreNodesAlivePacket(
				requestId
		);
		SettableFuture<NodeApiCallResult> future = SettableFuture.<NodeApiCallResult>create();
		nodeApi.sendToNode(requestId, future, buffer);
		return future;
	}

}
