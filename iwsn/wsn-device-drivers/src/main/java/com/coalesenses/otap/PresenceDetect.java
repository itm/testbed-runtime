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

package com.coalesenses.otap;

import com.coalesenses.otap.macromsg.MacroFabricSerializer;
import com.coalesenses.otap.macromsg.PresenceDetectReply;
import com.coalesenses.otap.macromsg.PresenceDetectRequest;
import com.coalesenses.seraerial.SerAerialPacket;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.generic.ChipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Dennis Pfisterer
 */
public class PresenceDetect {

	/**
	 *
	 */
	private static final Logger log = LoggerFactory.getLogger(PresenceDetect.class);

	/**
	 *
	 */
	private boolean detectionEnabled = false;

	/**
	 *
	 */
	private OtapPlugin plugin = null;

	/**
	 *
	 */
	private ScheduledExecutorService resendTimer = Executors.newSingleThreadScheduledExecutor();

	/**
	 *
	 */
	private TimeDiff detectStart = new TimeDiff();

	/**
	 *
	 */
	private int detectMsgIntervallMillis = 150; // for one hop

	/**
	 *
	 */
	private long detectInvalidationTimeoutMillis = 160 * detectMsgIntervallMillis; // 40 seconds

	/**
	 *
	 */
	private Map<Integer, OtapDevice> devices = new HashMap<Integer, OtapDevice>();

	/**
	 *
	 */
	private Set<PresenceDetectListener> listeners = new HashSet<PresenceDetectListener>();

	/**
	 *
	 */
	private ScheduledFuture<?> resendTimerScheduledFuture;

	/**
	 * @param pluginOtap
	 */
	public PresenceDetect(OtapPlugin pluginOtap) {
		super();
		plugin = pluginOtap;
	}

	/**
	 *
	 */
	public void timerServiceTimeout() {
		//On enabled detection, transmit a detect request packet
		if (isDetectionEnabled()) {
			PresenceDetectRequest pd = new PresenceDetectRequest();
			((OtapPlugin) plugin).otapBroadcast(MacroFabricSerializer.serialize(pd));
			log.debug("Sent presence detect request");
		}

		//Cleanup, remove devices where the last reply is too old
		Map<Integer, OtapDevice> remaining = new HashMap<Integer, OtapDevice>();
		Map<Integer, OtapDevice> deleted = new HashMap<Integer, OtapDevice>();

		for (Integer i : devices.keySet()) {
			OtapDevice d = devices.get(i);
			if (d.getLastReception().ms() <= this.detectInvalidationTimeoutMillis) {
				remaining.put(i, d);
			} else {
				deleted.put(i, d);
			}
		}

		this.devices = remaining;

		for (OtapDevice d : deleted.values()) {
			for (PresenceDetectListener l : listeners) {
				l.presenceDetectChange(d, PresenceDetectListener.Change.Removed);
			}
		}


	}

	/**
	 * @param p
	 */
	public void handle(SerAerialPacket p) {
		if (!detectionEnabled) {
			return;
		}

		byte[] b = p.getContent();


		MacroFabricSerializer.DataTypeCode type = MacroFabricSerializer.getDataTypeCode(b);

		if (type == MacroFabricSerializer.DataTypeCode.PRESENCEDETECTREPLY) {
			log.debug("rx presence detect reply");
			PresenceDetectReply reply = MacroFabricSerializer.deserialize_PresenceDetectReply(b, null);
			if (reply != null) {
				log.debug("Presence detect reply received from " + StringUtils
						.toHexString(reply.device_id) + ", software revision " + reply.revision_no
				);

				boolean newDevice = !devices.containsKey(reply.device_id);

				OtapDevice d = getDevice(reply.device_id);
				boolean informationChanged = (d.getApplicationID() != reply.application_id)
						|| (d.getSoftwareRevision() != reply.revision_no)
						|| (d.getChipType() != ChipType.getChipType(reply.chip_type))
						|| (d.getProtocolVersion() != reply.protocol_version);
				d.setApplicationID(reply.application_id);
				d.setSoftwareRevision(reply.revision_no);
				d.setChipType(ChipType.getChipType(reply.chip_type));
				d.setProtocolVersion(reply.protocol_version);
				d.setProgrammable(plugin.getProgramChipType(), plugin);
				d.getLastReception().touch();

				String protocolVersionOk =
						(d.getProtocolVersionOk(plugin) ? ", version ok" : ", False program version");
				protocolVersionOk += " ( " + d.getProtocolVersionAsString(plugin) + ")";
				if (newDevice || informationChanged) {
					d.setStatusMessage("App " + reply.application_id + " rev. " + reply.revision_no + protocolVersionOk
					);
				}

				for (PresenceDetectListener l : listeners) {
					if (newDevice) {
						l.presenceDetectChange(d, PresenceDetectListener.Change.Added);
					} else if (informationChanged) {
						l.presenceDetectChange(d, PresenceDetectListener.Change.Updated);
					}
				}

				if (newDevice) {
					((OtapPlugin) plugin).setOtapStatusText("Detected " + this.devices.size() + " devices");
				}

			} else {
				log.debug("Incorrect Presence Detect Reply " + p + " len " + b.length);
			}
		}


	}

	/**
	 * @param id
	 *
	 * @return
	 */
	private OtapDevice getDevice(int id) {

		if (!devices.containsKey(id)) {
			OtapDevice d = new OtapDevice();
			d.setId(id);
			devices.put(id, d);
		}
		return devices.get(id);
	}

	/**
	 * @return
	 */
	public Collection<OtapDevice> getDetectedDevices() {
		return this.devices.values();
	}

	/**
	 * @param detectionEnabled
	 */
	public void setDetectionEnabled(boolean detectionEnabled) {
		this.detectionEnabled = detectionEnabled;

		if (detectionEnabled) {
			plugin.warnOtapFunctionality();
			plugin.sendSetChannel();
			if (plugin.getMultihopSupportState()) {
				detectMsgIntervallMillis = 250;
			}
			//else
			//	detectMsgIntervallMillis = 50;
			log.debug("Presence detection enabled. Starting timer (interval " + detectMsgIntervallMillis + " ms)");
			resendTimerScheduledFuture = resendTimer.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					timerServiceTimeout();
				}
			}, 0, detectMsgIntervallMillis, TimeUnit.MILLISECONDS
			);
			detectStart.touch();
		} else {
			log.debug("Presence detection cancelled. Stopping timer.");
			if (resendTimerScheduledFuture != null) {
				resendTimerScheduledFuture.cancel(true);
				resendTimerScheduledFuture = null;
			}
		}
	}

	/**
	 * @return
	 */
	public TimeDiff getDetectionStart() {
		return this.detectStart;
	}

	/**
	 * @param plugin
	 */
	public void setParentPlugin(OtapPlugin plugin) {
		if (!(plugin instanceof OtapPlugin)) {
			throw new Error("Parent plugin not of expetect type");
		}
		this.plugin = (OtapPlugin) plugin;
	}

	/**
	 * @return
	 */
	public boolean isDetectionEnabled() {
		return detectionEnabled;
	}

	/**
	 * @param l
	 */
	public void addListener(PresenceDetectListener l) {
		this.listeners.add(l);
	}

	/**
	 * @param l
	 */
	public void removeListener(PresenceDetectListener l) {
		this.listeners.remove(l);
	}

	/**
	 *
	 */
	public void removeAllListeners() {
		this.listeners.clear();
	}

	/**
	 *
	 */
	public void clearDeviceList() {
		this.devices.clear();
	}

}
