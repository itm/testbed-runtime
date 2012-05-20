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
import static de.uniluebeck.itm.tr.util.StringUtils.toPrintableString;


public class NodeApi {

	private static final Logger log = LoggerFactory.getLogger(NodeApi.class);

	private TimeUnit defaultTimeUnit;

	private int defaultTimeout;

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

	private final Interaction interaction;

	private final LinkControl linkControl;

	private final NetworkDescription networkDescription;

	private final NodeControl nodeControl;

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

					currentJobLock.lock();
					currentJob = jobQueue.take();

					// execute job
					if (log.isDebugEnabled()) {
						log.debug("{} => Sending to node with request ID {}: {}",
								new Object[]{
										nodeUrn,
										currentJob.requestId,
										toPrintableString(currentJob.buffer.array(), 200)
								}
						);
					}

					deviceAdapter.sendToNode(currentJob.buffer);

					log.debug("{} => Waiting for node to answer to job with request ID {}", nodeUrn,
							currentJob.requestId
					);
					boolean timeout = !currentJobDone.await(defaultTimeout, defaultTimeUnit);
					log.debug("{} => Job with request ID {} done (timeout={}, success={}).",
							new Object[]{
									nodeUrn,
									currentJob.requestId,
									!timeout,
									currentJobResult != null && currentJobResult.isSuccessful()
							}
					);

					if (timeout) {
						currentJob.future.setException(new TimeoutException());
					} else {
						currentJob.future.set(currentJobResult);
					}

				} catch (InterruptedException e) {
					log.trace("{} => Interrupted while waiting for job to be done."
							+ " This is propably OK as it should only happen during shutdown.",
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

	public NodeApi(final String nodeUrn, final NodeApiDeviceAdapter deviceAdapter, final int defaultTimeout,
				   final TimeUnit defaultTimeUnit) {

		checkNotNull(nodeUrn);
		checkNotNull(deviceAdapter);
		checkNotNull(defaultTimeout);
		checkNotNull(defaultTimeUnit);

		this.nodeUrn = nodeUrn;
		this.defaultTimeout = defaultTimeout;
		this.defaultTimeUnit = defaultTimeUnit;

		this.deviceAdapter = deviceAdapter;
		this.deviceAdapter.setNodeApi(this);

		this.interaction = new InteractionImpl(nodeUrn, this);
		this.linkControl = new LinkControlImpl(nodeUrn, this);
		this.networkDescription = new NetworkDescriptionImpl(nodeUrn, this);
		this.nodeControl = new NodeControlImpl(nodeUrn, this);
	}

	public Interaction getInteraction() {
		return interaction;
	}

	public LinkControl getLinkControl() {
		return linkControl;
	}

	public NetworkDescription getNetworkDescription() {
		return networkDescription;
	}

	public NodeControl getNodeControl() {
		return nodeControl;
	}

	public NodeApiDeviceAdapter getDeviceAdapter() {
		return deviceAdapter;
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
					new Object[]{
							nodeUrn,
							requestId,
							toPrintableString(buffer.array(), 200)
					}
			);
		}

		jobQueue.add(new Job(requestId, future, buffer));

	}

	boolean receiveFromNode(ByteBuffer packet) {

		checkNotNull(packet);

		byte[] packetBytes = packet.array();

		boolean isNodeAPIPacket =
				Packets.Interaction.isInteractionPacket(packet) ||
						Packets.LinkControl.isLinkControlPacket(packet) ||
						Packets.NetworkDescription.isNetworkDescriptionPacket(packet) ||
						Packets.NodeControl.isNodeControlPacket(packet);

		if (!isNodeAPIPacket) {
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
					new Object[]{nodeUrn, requestId, responseCode, responsePayload}
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

	public void start() {
		log.debug("{} => Starting Node API JobExecutorThread", nodeUrn);
		jobExecutorThread.start();
	}

	public void stop() {
		log.debug("{} => Stopping Node API JobExecutorThread", nodeUrn);
		jobExecutorThread.interrupt();
	}

}