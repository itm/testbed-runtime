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

package de.uniluebeck.itm.tr.iwsn.nodeapi;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiPackets.Interaction.*;
import static de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiPackets.LinkControl.*;
import static de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiPackets.NetworkDescription.newGetNeighborhoodPacket;
import static de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiPackets.NetworkDescription.newGetPropertyValuePacket;
import static de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiPackets.NodeControl.*;
import static de.uniluebeck.itm.tr.util.StringUtils.toPrintableString;


class NodeApiImpl extends AbstractService implements NodeApi {

	private static final Logger log = LoggerFactory.getLogger(NodeApiImpl.class);

	private TimeUnit defaultTimeUnit;

	private long defaultTimeout;

	private final String nodeUrn;

	private static class Job {

		public final int requestId;

		public final SettableFuture<NodeApiCallResult> future;

		public final ByteBuffer buffer;

		public Job(final int requestId, final SettableFuture<NodeApiCallResult> future, final ByteBuffer buffer) {
			this.requestId = requestId;
			this.future = future;
			this.buffer = buffer;
		}
	}

	private int lastRequestID = 0;

	private final NodeApiDeviceAdapter deviceAdapter;

	private final BlockingDeque<Job> jobQueue = new LinkedBlockingDeque<Job>();

	private final ReentrantLock currentJobLock = new ReentrantLock(true);

	private Job currentJob;

	private NodeApiCallResult currentJobResult;

	private final Thread jobExecutorThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (!Thread.interrupted()) {

				// waiting blocking for next job to execute
				try {
					currentJob = jobQueue.take();
				} catch (InterruptedException e) {
					log.trace("{} => Interrupted while waiting for job to be done."
							+ " This is probably OK as it should only happen during shutdown.",
							nodeUrn
					);
					continue;
				}

				currentJobLock.lock();

				try {

					// execute job
					if (log.isDebugEnabled()) {
						log.debug("{} => Sending to node with request ID {}: {}",
								nodeUrn,
								currentJob.requestId,
								toPrintableString(currentJob.buffer.array(), 200)
						);
					}

					deviceAdapter.sendToNode(currentJob.buffer);

					log.debug("{} => Waiting for node to answer to job with request ID {}", nodeUrn,
							currentJob.requestId
					);
					boolean timeout = !currentJobDone.await(defaultTimeout, defaultTimeUnit);
					log.debug("{} => Job with request ID {} done (timeout={}, success={}).",
							nodeUrn,
							currentJob.requestId,
							!timeout,
							currentJobResult != null && currentJobResult.isSuccessful()
					);

					if (timeout) {
						currentJob.future.setException(new TimeoutException());
					} else {
						currentJob.future.set(currentJobResult);
					}

				} catch (InterruptedException e) {

					log.trace("{} => Interrupted while waiting for job to be done."
							+ " This is probably OK as it should only happen during shutdown.",
							nodeUrn
					);

				} finally {

					currentJobLock.unlock();
				}
			}
		}
	}, "Node API JobExecutorThread"
	);

	private final Condition currentJobDone = currentJobLock.newCondition();

	@Inject
	public NodeApiImpl(@Assisted final String nodeUrn,
					   @Assisted final NodeApiDeviceAdapter deviceAdapter,
					   @Assisted final long defaultTimeout,
					   @Assisted final TimeUnit defaultTimeUnit) {

		this.nodeUrn = checkNotNull(nodeUrn);
		this.defaultTimeout = checkNotNull(defaultTimeout);
		this.defaultTimeUnit = checkNotNull(defaultTimeUnit);
		this.deviceAdapter = checkNotNull(deviceAdapter);
		this.deviceAdapter.setNodeApi(this);
	}

	/**
	 * Creates a requestId in a thread-safe manner.
	 *
	 * @return a newly created request ID between 0 and 255
	 */
	synchronized int nextRequestId() {
		return lastRequestID >= 255 ? (lastRequestID = 0) : ++lastRequestID;
	}

	void sendToNode(final int requestId, final SettableFuture<NodeApiCallResult> future, final ByteBuffer buffer) {

		if (log.isDebugEnabled()) {
			log.debug("{} => Enqueueing job to node with request ID {}: {}",
					nodeUrn, requestId, toPrintableString(buffer.array(), 200)
			);
		}

		jobQueue.add(new Job(requestId, future, buffer));

	}

	boolean receiveFromNode(ByteBuffer packet) {

		checkNotNull(packet);

		byte[] packetBytes = packet.array();

		boolean isNodeAPIPacket =
				NodeApiPackets.Interaction.isInteractionPacket(packet) ||
						NodeApiPackets.LinkControl.isLinkControlPacket(packet) ||
						NodeApiPackets.NetworkDescription.isNetworkDescriptionPacket(packet) ||
						NodeApiPackets.NodeControl.isNodeControlPacket(packet);

		if (!isNodeAPIPacket) {
			return false;
		}

		if (packetBytes.length < 3) {
			return false;
		}

		int requestId = (packetBytes[1] & 0xFF);
		byte responseCode = packetBytes[2];
		byte[] responsePayload = null;

		if (packetBytes.length > 3) {
			responsePayload = new byte[packetBytes.length - 3];
			System.arraycopy(packetBytes, 3, responsePayload, 0, packetBytes.length - 3);
		}

		if (log.isDebugEnabled()) {
			log.debug("{} => Received from node with request ID {} and response code {}: {}",
					nodeUrn, requestId, responseCode, responsePayload
			);
		}

		try {

			currentJobLock.lock();

			if (currentJob != null && currentJob.requestId == requestId) {

				currentJobResult = new NodeApiCallResultImpl(requestId, responseCode, responsePayload);

				log.debug("{} => Signalling that current job is done (response received).", nodeUrn);
				currentJobDone.signal();

			} else if (log.isDebugEnabled()) {
				log.debug("{} => Received message for unknown requestId: {}", nodeUrn, requestId);
			}

		} finally {
			currentJobLock.unlock();
		}

		return true;

	}

	@Override
	protected void doStart() {
		try {
			log.debug("{} => Starting Node API JobExecutorThread", nodeUrn);
			jobExecutorThread.start();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			log.debug("{} => Stopping Node API JobExecutorThread", nodeUrn);
			jobExecutorThread.interrupt();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public ListenableFuture<NodeApiCallResult> sendVirtualLinkMessage(byte RSSI, byte LQI, long destination,
																	  long source, byte[] payload) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newVirtualLinkMessagePacket(requestId, RSSI, LQI, destination, source, payload));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> sendVirtualLinkMessage(long destination, long source, byte[] payload) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newVirtualLinkMessagePacket(requestId, destination, source, payload));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> sendByteMessage(byte binaryType, byte[] payload) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newByteMessagePacket(requestId, binaryType, payload));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> flashProgram(byte[] payload) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newFlashProgramPacket(requestId, payload));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> setVirtualLink(long destinationNode) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newSetVirtualLinkPacket(requestId, destinationNode));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> destroyVirtualLink(long destinationNode) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newDestroyVirtualLinkPacket(requestId, destinationNode));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> enablePhysicalLink(long nodeB) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newEnablePhysicalLinkPacket(requestId, nodeB));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> disablePhysicalLink(long nodeB) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newDisablePhysicalLinkPacket(requestId, nodeB));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> getPropertyValue(byte property) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newGetPropertyValuePacket(requestId, property));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> getNeighborhood() {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newGetNeighborhoodPacket(requestId));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> enableNode() {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newEnableNodePacket(requestId));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> disableNode() {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newDisableNodePacket(requestId));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> resetNode(int time) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newResetNodePacket(requestId, time));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> setStartTime(int time) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newSetStartTimePacket(requestId, time));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> setVirtualID(long virtualNodeID) {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newSetVirtualIDPacket(requestId, virtualNodeID));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> getVirtualID() {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newGetIDPacket(requestId));
		return future;
	}

	@Override
	public ListenableFuture<NodeApiCallResult> areNodesAlive() {
		int requestId = nextRequestId();
		SettableFuture<NodeApiCallResult> future = SettableFuture.create();
		sendToNode(requestId, future, newAreNodesAlivePacket(requestId));
		return future;
	}

}