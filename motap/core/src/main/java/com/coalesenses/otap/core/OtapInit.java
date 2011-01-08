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
import com.coalesenses.otap.core.macromsg.OtapInitReply;
import com.coalesenses.otap.core.macromsg.OtapInitRequest;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class OtapInit {

	/**
	 *
	 */
	private static final Logger log = LoggerFactory.getLogger(OtapInit.class);

	/**
	 *
	 */
	private int txIntervalMs = 2000;

	/**
	 *
	 */
	private TimeDiff participatingDevicesStart = new TimeDiff();

	/**
	 *
	 */
	private TimeDiff lastMessageTransmission = new TimeDiff();

	/**
	 *
	 */
	private HashSet<com.coalesenses.otap.core.OtapFlasherListener> listeners = new HashSet<com.coalesenses.otap.core.OtapFlasherListener>();

	/**
	 *
	 */
	private ScheduledExecutorService workTimer = Executors.newSingleThreadScheduledExecutor();

	/**
	 *
	 */
	private OtapPlugin plugin = null;

	/**
	 *
	 */
	private boolean running = false;

	/**
	 *
	 */
	private BinProgram program = null;

	/**
	 *
	 */
	private Collection<com.coalesenses.otap.core.OtapDevice> selectedDevices = null;

	/**
	 *
	 */
	private TreeSet<com.coalesenses.otap.core.OtapDevice> participatingDevices = new TreeSet<com.coalesenses.otap.core.OtapDevice>();

	/**
	 *
	 */
	private int deviceSettingMaxRerequests = 50;

	/**
	 *
	 */
	private int deviceSettingTimeoutMultiplierMs = 15;

	/**
	 *
	 */
	private TimeDiff lastGuiUpdate = new TimeDiff(500);

	/**
	 *
	 */
	private ScheduledFuture<?> scheduledFuture;

	/**
	 * @param pluginOtap
	 */
	public OtapInit(OtapPlugin pluginOtap) {
		super();
		plugin = pluginOtap;
	}

	/**
	 *
	 */
	public void startOtapInit(BinProgram program) {
		stopOtapInit();
		log.info("Starting participating devices run, " + selectedDevices.size() + " should participate");

		this.participatingDevicesStart.touch();
		this.participatingDevices.clear();
		this.program = program;
		this.running = true;

		for (com.coalesenses.otap.core.OtapDevice d : selectedDevices) {
			d.setProgress(0.0);
			d.setStatusMessage("Waiting for init reply");
			for (com.coalesenses.otap.core.OtapFlasherListener l : listeners) {
				l.otapFlashUpdate(d);
			}
		}
		scheduledFuture = workTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				timerServiceTimeout();
			}
		}, 10, 500, TimeUnit.MILLISECONDS
		);
	}

	/**
	 * @param devices
	 *
	 * @return
	 */
	public boolean setParticipatingDevices(Collection<com.coalesenses.otap.core.OtapDevice> devices) {
		if (running) {
			log.warn("Ignoring participating devices set while running.");
			return false;
		}

		this.selectedDevices = new LinkedList<com.coalesenses.otap.core.OtapDevice>();
		this.selectedDevices.addAll(devices);

		log.debug("OK, got " + devices.size() + " detected devices that shall participate");

		return true;
	}

	/**
	 *
	 */
	public void stopOtapInit() {
		if (scheduledFuture != null) {
			scheduledFuture.cancel(true);
			scheduledFuture = null;
		}
		running = false;
		log.debug("Stopped current participating devices run, participating: " + participatingDevices.size());
	}

	/**
	 * @param p
	 */
	public void handle(com.coalesenses.otap.core.seraerial.SerAerialPacket p) {
		if (!running) {
			return;
		}

		byte[] b = p.getContent();
		MacroFabricSerializer.DataTypeCode t = MacroFabricSerializer.getDataTypeCode(b);

		if (t == MacroFabricSerializer.DataTypeCode.OTAPINITREPLY) {
			OtapInitReply rep = MacroFabricSerializer
                    .deserialize_OtapInitReply(b);
			if (rep != null) {
				boolean alreadyParticipating = false;
				for (com.coalesenses.otap.core.OtapDevice d : this.participatingDevices) {
					if (d.getId() == rep.device_id) {
						alreadyParticipating = true;
						break;
					}
				}
				if (!alreadyParticipating) {
					com.coalesenses.otap.core.OtapDevice d = this.getParticipatingDevice(rep.device_id);
					participatingDevices.add(d);

					d.setStatusMessage("Init reply received");
					for (com.coalesenses.otap.core.OtapFlasherListener l : listeners) {
						l.otapFlashUpdate(d);
					}
				}
				log.debug("Init Reply Received  from " + Integer.toHexString(rep.device_id) + ": " + rep +
						", " + participatingDevices.size() + " participating devices: " + StringUtils
						.toString(participatingDevices)
				);
			} else {
				log.debug("Incorrect Otap Init Reply " + b);
			}
		}

	}

	/**
	 *
	 */
	public void timerServiceTimeout() {
		if (!running || lastMessageTransmission.ms() < txIntervalMs) {
			return;
		}


		//Prepare a re-usable Otap init request
		OtapInitRequest
                req = new OtapInitRequest();
		req.chunk_count = program.getChunkCount();
		req.max_re_requests = (short) this.deviceSettingMaxRerequests;
		req.timeout_multiplier_ms = (short) this.deviceSettingTimeoutMultiplierMs;

		//Check how many packets must be sent to notify all devices
		Iterator<com.coalesenses.otap.core.OtapDevice> it = this.selectedDevices.iterator();

		int maxDevicesPerPacket = req.participating_devices.value.length;
		int packetCount = (int) Math.ceil(((double) this.selectedDevices.size()) / ((double) maxDevicesPerPacket));

		log.debug("Got " + selectedDevices
				.size() + " present devices, resulting in " + packetCount + " packets with max. " + maxDevicesPerPacket + " devices each"
		);

		//Complete the request and broadcast it
		for (int i = 0; i < packetCount; ++i) {
			int deviceCount = 0;

			//Fill the participating device array
			while ((it.hasNext()) && (deviceCount < req.participating_devices.value.length - 1)) {
				com.coalesenses.otap.core.OtapDevice d = it.next();
				req.participating_devices.value[deviceCount++] = d.getId();
			}


			//Send the message
			if (deviceCount > 0) {
				req.participating_devices.count = deviceCount;
				log.debug("Sending init req: " + StringUtils.toHexString(
                        MacroFabricSerializer.serialize(req)));
				plugin.otapBroadcast(MacroFabricSerializer.serialize(req));
				lastMessageTransmission.touch();
			}
		}

		//Update the GUI
		if (lastGuiUpdate.isTimeout()) {
			plugin.setOtapStatusText(
					"Waiting for " + selectedDevices.size() + " init replies, got " + participatingDevices.size()
			);
			lastGuiUpdate.touch();
		}

		//log.debug("Transmitted " + packetCount + " packets");
	}

	/**
	 * @return
	 */
	public Collection<com.coalesenses.otap.core.OtapDevice> getParticipatingDevices() {
		return this.participatingDevices;
	}

	/**
	 * @param l
	 *
	 * @return
	 */
	private com.coalesenses.otap.core.OtapDevice getParticipatingDevice(int l) {
		for (com.coalesenses.otap.core.OtapDevice d : this.participatingDevices) {
			if (d.getId() == l) {
				return d;
			}
		}

		for (com.coalesenses.otap.core.OtapDevice d : this.selectedDevices) {
			if (d.getId() == l) {
				return d;
			}
		}

		log.debug("OtapInit: Got init reply from unknown device");
		com.coalesenses.otap.core.OtapDevice d = new com.coalesenses.otap.core.OtapDevice();
		d.setId(l);
		participatingDevices.add(d);
		return d;
	}

	/**
	 * @return
	 */
	public TimeDiff getStartingTime() {
		return this.participatingDevicesStart;
	}

	/**
	 * @param plugin
	 */
	public void setParentPlugin(OtapPlugin plugin) {
		if (!(plugin instanceof OtapPlugin)) {
			throw new Error("Parent plugin not of expected type");
		}
		this.plugin = (OtapPlugin) plugin;
	}

	/**
	 * @param l
	 */
	public void addListener(com.coalesenses.otap.core.OtapFlasherListener l) {
		listeners.add(l);
	}

	/**
	 * @param l
	 */
	public void removeListener(com.coalesenses.otap.core.OtapFlasherListener l) {
		listeners.remove(l);
	}

	/**
	 * @return
	 */
	public int getDeviceSettingMaxRerequests() {
		return deviceSettingMaxRerequests;
	}

	/**
	 * @param deviceSettingMaxRerequests
	 */
	public void setDeviceSettingMaxRerequests(int deviceSettingMaxRerequests) {
		this.deviceSettingMaxRerequests = deviceSettingMaxRerequests;
	}

	/**
	 * @return
	 */
	public int getDeviceSettingTimeoutMultiplierMs() {
		return deviceSettingTimeoutMultiplierMs;
	}

	/**
	 * @param deviceSettingTimeoutMultiplierMs
	 *
	 */
	public void setDeviceSettingTimeoutMultiplierMs(int deviceSettingTimeoutMultiplierMs) {
		this.deviceSettingTimeoutMultiplierMs = deviceSettingTimeoutMultiplierMs;
	}

	/**
	 * @return
	 */
	public Collection<com.coalesenses.otap.core.OtapDevice> getSelectedDevices() {
		return selectedDevices;
	}

}
