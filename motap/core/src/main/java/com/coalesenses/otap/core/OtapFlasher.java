/**********************************************************************************************************************
 * Copyright (c) 2010, coalesenses GmbH                                                                               *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the coalesenses GmbH nor the names of its contributors may be used to endorse or promote     *
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

package com.coalesenses.otap.core;

import com.coalesenses.otap.core.macromsg.MacroFabricSerializer;
import com.coalesenses.otap.core.macromsg.OtapProgramRequest;
import com.coalesenses.otap.core.macromsg.OtapProgramReply;
import com.coalesenses.otap.core.seraerial.SerAerialPacket;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Dennis Pfisterer
 */
public class OtapFlasher {

	/**
	 *
	 */
	private static final Logger log = LoggerFactory.getLogger(OtapFlasher.class);

	/**
	 *
	 */
	private OtapPlugin plugin = null;

	/**
	 *
	 */
	private ScheduledExecutorService workTimer = Executors.newSingleThreadScheduledExecutor();

	/**
	 *
	 */
	private com.coalesenses.otap.core.BinProgram program = null;

	/**
	 *
	 */
	private Collection<com.coalesenses.otap.core.OtapDevice> devices = null;

	/**
	 *
	 */
	private OtapChunk chunk = null;

	/**
	 *
	 */
	private TimeDiff chunkStart = new TimeDiff();

	/**
	 *
	 */
	private TimeDiff lastPacketSent = new TimeDiff();

	/**
	 *
	 */
	private TimeDiff lastGuiUpdate = new TimeDiff(500);

	/**
	 *
	 */
	private TimeDiff lastTreeCreation = new TimeDiff();

	/**
	 *
	 */
	private TreeSet<OtapPacket> remainingPacketsInChunk = new TreeSet<OtapPacket>();

	/**
	 *
	 */
	private HashSet<OtapFlasherListener> listeners = new HashSet<OtapFlasherListener>();

	/**
	 *
	 */
	private boolean running = false;

	/**
	 *
	 */
	private int msPerPacket = 100; // timeout multiplier

	/**
	 *
	 */
	private int maxRetries = 30;

	/**
	 *
	 */
	private int maxTimePerChunkMs = -1;


	/**
	 *
	 */
	private TimeDiff waitForRerequestsStart = new TimeDiff();

	/**
	 *
	 */
	private boolean waitForRerequests = false;

	/**
	 *
	 */
	private ScheduledFuture<?> timerSchedule;

	/**
	 * @param pluginOtap
	 */
	public OtapFlasher(OtapPlugin pluginOtap) {
		super();
		plugin = pluginOtap;
	}

	/**
	 *
	 */
	public synchronized void timerServiceTimeout() {
		if (!isRunning()) {
			return;
		}

		//Check if skip to next chunk or done
		checkLeapToNextChunk();

		if (this.lastPacketSent.ms() < this.msPerPacket) {
			return;
		}

		if (waitForRerequests) {
			if (waitForRerequestsStart.ms() > 1000) {
				waitForRerequests = false;
			} else {
				if (plugin.getMultihopSupportState()) {
					if (lastTreeCreation.ms() > 10000) {
						plugin.createTree();
						lastTreeCreation.touch();
					}
				}
				return;
			}
		}


		//Send the remaining packets
		if (remainingPacketsInChunk.size() > 0) {
			//Get the next remaining packet
			OtapPacket p = remainingPacketsInChunk.first();
			remainingPacketsInChunk.remove(p);

			//Prepare the request using Fabric
			OtapProgramRequest req = new OtapProgramRequest();
			req.chunk_no = chunk.getChunkNumber();
			req.index = (short) p.getIndex();
			req.packets_in_chunk = (byte) chunk.getPacketCount();
			req.overall_packet_no = p.getOverallPacketNumber();
			req.remaining = (short) remainingPacketsInChunk.size();
			byte[] code = p.getContent();
			req.code.count = (short) code.length;
			for (int i = 0; i < req.code.value.length; i++) {
				req.code.value[i] = (byte) 0xFF;
			}
			System.arraycopy(code, 0, req.code.value, 0, req.code.count);

			log.debug(
					"Sending packet with " + req.code.count + " bytes. Chunk[" + req.chunk_no + "], Index[" + req.index + "], PacketsInChunk["
							+ req.packets_in_chunk + "], OverallPacketNo[" + req.overall_packet_no + "], Remaining[" + req.remaining + "], RevisionNo["
							+ "]"
			);

			//Send the packet
			byte[] packet = MacroFabricSerializer.serialize(req);
			plugin.otapBroadcast(packet);
			lastPacketSent.touch();

		}
		// Wait for requests
		else {
			if (!waitForRerequests) {
				waitForRerequests = true;
				waitForRerequestsStart.touch();
			}
		}

		//Update the GUI
		if (lastGuiUpdate != null) {
			if (lastGuiUpdate.isTimeout() || program.getChunkCount() <= 5) {
				if (plugin != null && chunk != null) {
					plugin.setOtapStatusText("Chunk " + (chunk.getChunkNumber() + 1) + "/" + program
							.getChunkCount() + ", Remaining packets in chunk "
							+ remainingPacketsInChunk.size() + "/" + chunk
							.getPacketCount() + ", Remaining time for chunk: "
							+ ((maxTimePerChunkMs - chunkStart.ms()) / 1000) + "s"
					);
					lastGuiUpdate.touch();
				}
			}
		}

	}

	/**
	 *
	 */
	private void handleProgramReply(com.coalesenses.otap.core.OtapDevice d, OtapProgramReply reply) {
		if (d.getChunkNo() == reply.chunk_no && d.isChunkComplete()) {
			return;
		}

		d.setChunkNo(reply.chunk_no);

		//Only trigger the request for packets if missing packets are requested, otherwise, the device is done for this chunk
		if (reply.missing_indices.count > 0) {
			log.debug("Missing indices @ " + Integer.toHexString(reply.device_id) + " [" + StringUtils
					.toString(reply.missing_indices.value, 0, reply.missing_indices.count)
					+ "], chunk " + reply.chunk_no
			);

			//Add the missing packets to the set of remaining packets in this chunk
			for (int i = 0; i < reply.missing_indices.count; ++i) {
				int packetIndex = reply.missing_indices.value[i];
				OtapPacket packet = chunk.getPacketByIndex(packetIndex);
				if (packet != null)
				//requestedPacketsInChunk.add(packet);
				{
					remainingPacketsInChunk.add(packet);
				} else {
					log.warn("Packet index " + packetIndex + " not found in chunk");
				}
			}

			d.setStatusMessage(
					"Chunk " + (reply.chunk_no + 1) + ": Requested " + reply.missing_indices.count + " packet(s)"
			);
		} else {
			log.debug("No missing indices at " + Integer.toHexString(reply.device_id) + ", chunk " + reply.chunk_no);
			d.setChunkComplete(true);

			if (reply.chunk_no == program.getChunkCount() - 1) {
				d.setStatusMessage("Done");
			} else {
				d.setStatusMessage("Chunk " + (reply.chunk_no + 1) + "/" + program.getChunkCount() + " complete");
			}
		}

		double progress = getProgress(reply.chunk_no, reply.missing_indices.count);
		log.debug("Device " + Integer.toHexString(d.getId()) + " progress " + progress);
		d.setProgress(progress);

		for (OtapFlasherListener l : listeners) {
			l.otapFlashUpdate(d);
		}
	}

	/**
	 *
	 */
	private double getProgress(int chunk, int missingPackets) {
		double overallPackets = program.getPacketCount();
		double packetsUpToThisChunk = program.getPacketCount(0, chunk) - missingPackets;
		double progress = 0.0;

		if (overallPackets > 0.0) {
			progress = packetsUpToThisChunk / overallPackets;
		}

		log.debug(
				"Progress: " + progress + ", overall packets: " + overallPackets + ", packets up to this chunk " + program
						.getPacketCount(0, chunk)
						+ ", missing: " + missingPackets
		);

		return progress;
	}

	/**
	 *
	 */
	public synchronized void handle(SerAerialPacket p) {
		//Check for the correct current chunk
		checkLeapToNextChunk();
		if (!isRunning()) {
			return;
		}

		//Determine the message type
		byte[] b = p.getContent();
		MacroFabricSerializer.DataTypeCode type = MacroFabricSerializer.getDataTypeCode(b);
		log.debug("Otap rx packet: " + type.toString());

		//React to program replies
		if (type == MacroFabricSerializer.DataTypeCode.OTAPPROGRAMREPLY) {
			OtapProgramReply reply = MacroFabricSerializer.deserialize_OtapProgramReply(b, null);

			if (reply != null) {
				//Check if the device participate in the OTAP cycle
				if (!getDeviceParticipates(reply.device_id)) {
					log.debug("Device " + reply.device_id + " does not participate. Ignoring packet.");
					return;
				}

				//Only react to matching requests, discard old ones
				if (chunk.getChunkNumber() == reply.chunk_no) {
					handleProgramReply(getDevice(reply.device_id), reply);
				} else {
					log.debug(
							"Received invalid reply from " + reply.device_id + ": chunk[is:" + reply.chunk_no + ",expected:" + chunk
									.getChunkNumber() + "]"
					);
				}
			} else {
				log.debug("Incomplete program reply");
			}
		}

	}

	/**
	 *
	 */
	private synchronized void checkLeapToNextChunk() {
		if (!running) {
			return;
		}

		LinkedList<com.coalesenses.otap.core.OtapDevice> del = new LinkedList<com.coalesenses.otap.core.OtapDevice>();
		for (com.coalesenses.otap.core.OtapDevice d : devices) {
			if (d.getChunkNo() + 1 < chunk.getChunkNumber()) {
				log.warn("From now on we do not wait for device " + Integer.toHexString(d.getId()) + " anymore.");
				del.add(d);
			}
		}
		for (com.coalesenses.otap.core.OtapDevice d : del) {
			devices.remove(d);
		}

		if (chunkStart.ms() >= maxTimePerChunkMs || isAllDevicesReceivedAllPacketsInChunk()) {
			log.info("OTAP::Leaping to next chunk. Still remaining packets [" + remainingPacketsInChunk.size() + "]");
			prepareChunk(chunk.getChunkNumber() + 1);
		}
	}

	/**
	 *
	 */
	private synchronized void prepareChunk(int number) {
		log.info("Preparing chunk " + number);

		chunk = program.getChunk(number);
		remainingPacketsInChunk.clear();
		//requestedPacketsInChunk.clear();
		chunkStart.touch();
		waitForRerequests = false;

		for (com.coalesenses.otap.core.OtapDevice d : this.devices) {
			d.setChunkComplete(false);
		}

		if (chunk != null) {
			remainingPacketsInChunk.addAll(chunk.getPackets());
			log.debug("OK, now got " + remainingPacketsInChunk.size() + " packets to transmit");
		} else {
			log.info("OTAP::No more chunks available. Stopping OTAP. Done.");
			cancelOtap();
			for (OtapFlasherListener l : listeners) {
				l.otapDone();
			}
		}
	}

	/**
	 * @throws IOException
	 */
	public synchronized void startOtap(
            com.coalesenses.otap.core.BinProgram program, Collection<com.coalesenses.otap.core.OtapDevice> devices) {
		cancelOtap();
		log.info("OTAP::Starting programming");

		int maxPacketsPerChunk = 0;
		for (int i = 0; i < program.getChunkCount(); ++i) {
			if (program.getPacketCount(i, i) > maxPacketsPerChunk) {
				maxPacketsPerChunk = program.getPacketCount(i, i);
			}
		}

		maxTimePerChunkMs = maxRetries * maxPacketsPerChunk * msPerPacket + 10000;

		log.debug(
				"Using " + msPerPacket + " ms per packet, " + maxTimePerChunkMs + " ms per chunk, " + maxPacketsPerChunk
						+ " packets per chunk (max)" + maxRetries + " max retries"
		);

		this.program = program;
		this.devices = devices;
		this.running = true;

		prepareChunk(0);

		timerSchedule = workTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				timerServiceTimeout();
			}
		}, 0, this.msPerPacket / 2, TimeUnit.MILLISECONDS
		);
	}

	/**
	 *
	 */
	public void cancelOtap() {
//		public synchronized void cancelOtap() {
        if(timerSchedule!=null)
            timerSchedule.cancel(true);

		if (isRunning()) {
			log.info("OTAP::Stopped programming");
		}
		// SERIAL BEGIN DEBUG
		/*
		if (logFilePrintWriter != null)
		{
			logFilePrintWriter.flush();
			logFilePrintWriter.close();
			log_file = null;
		}*/
		// SERIAL END DEBUG

		running = false;
	}

	/**
	 *
	 */
	private boolean isAllDevicesReceivedAllPacketsInChunk() {
		if (devices.size() <= 0) {
			return false;
		}

		boolean allPacketsAtAllDevicesRX = true;
		for (com.coalesenses.otap.core.OtapDevice d : devices) {
			if (!d.isChunkComplete()) {
				allPacketsAtAllDevicesRX = false;
			}
		}

		if (allPacketsAtAllDevicesRX) {
			log.debug("All devices rxed all packets in chunk");
		}

		return allPacketsAtAllDevicesRX;
	}

	/**
	 *
	 */
	private boolean getDeviceParticipates(long deviceId) {
		com.coalesenses.otap.core.OtapDevice d = getDevice(deviceId);
		return d != null;
	}

	/**
	 *
	 */
	private com.coalesenses.otap.core.OtapDevice getDevice(long deviceId) {
		for (com.coalesenses.otap.core.OtapDevice d : devices) {
			if (d.getId() == deviceId) {
				return d;
			}
		}
		return null;
	}

	/**
	 *
	 */
	public void addListener(OtapFlasherListener l) {
		listeners.add(l);
	}

	/**
	 *
	 */
	public void removeListener(OtapFlasherListener l) {
		listeners.remove(l);
	}

	/**
	 *
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 *
	 */
	public void setParentPlugin(OtapPlugin plugin) {
		if (!(plugin instanceof OtapPlugin)) {
			throw new Error("Parent plugin not of expected type");
		}

		this.plugin = (OtapPlugin) plugin;
	}

	/**
	 *
	 */
	public int getMsPerPacket() {
		return msPerPacket;
	}

	/**
	 *
	 */
	public void setMsPerPacket(int msPerPacket) {
		this.msPerPacket = msPerPacket;
	}

	/**
	 *
	 */
	public int getMaxRetries() {
		return maxRetries;
	}

	/**
	 *
	 */
	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

}
