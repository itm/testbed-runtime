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

import com.coalesenses.otap.core.connector.DeviceConnector;
import com.coalesenses.otap.core.connector.DeviceConnectorListener;
import com.coalesenses.otap.core.macromsg.MacroFabricSerializer;
import com.coalesenses.otap.core.seraerial.SerAerialPacket;
import com.coalesenses.otap.core.seraerial.SerialRoutingPacket;
import com.coalesenses.otap.core.util.iSenseAes;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.generic.ChipType;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.PacketTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Dennis Pfisterer
 */
public class OtapPlugin /*extends SerAerialPlugin*/ implements PresenceDetectListener, OtapFlasherListener, DeviceConnectorListener {

	/**
	 *
	 */
	private static final Logger log = LoggerFactory.getLogger(OtapPlugin.class);

	/**
	 *
	 */
	private ScheduledFuture<?> scheduledFuture;

    /**
	 *
	 */
	public enum State {

		None, PresenceDetect, OtapInit, Otap
	}

	/**
	 *
	 */
	private String statusText = "";

	/**
	 *
	 */
	private State state = State.None;

	/**
	 *
	 */
	private PresenceDetect presenceDetect = null;

	/**
	 *
	 */
	private com.coalesenses.otap.core.OtapInit otapInit = null;

	/**
	 *
	 */
	private com.coalesenses.otap.core.OtapFlasher otapFlash = null;

	/**
	 *
	 */
	//private OtapPluginGui gui = null;

	/**
	 *
	 */
	private String programFilename = null;


	/**
	 *
	 */
	private com.coalesenses.otap.core.BinProgram program = null;

	/**
	 *
	 */
	private ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

	/**
	 *
	 */
	private int otapProtocolVersion = 2;

	/**
	 *
	 */
	private int motapProtocolVersion = 10;

	/**
	 *
	 */
	private int channel = 18;

	/**
	 *
	 */
	private boolean motapSupportEnabled = false;

	/**
	 *
	 */
	private byte tree_routing_round = 0;

	/**
	 *
	 */
	private int id = 0;

	/**
	 *
	 */
	private boolean useOtapKey = false;

	/**
	 *
	 */
	private iSenseAes inaes = new iSenseAes();

	/**
	 *
	 */
	private iSenseAes outaes = new iSenseAes();

    private DeviceConnector connector = null;

    public OtapPlugin(DeviceConnector connector) {
        this.connector = connector;
        presenceDetect = new PresenceDetect(this);
		otapInit = new com.coalesenses.otap.core.OtapInit(this);
		otapFlash = new com.coalesenses.otap.core.OtapFlasher(this);

		presenceDetect.addListener(this);
		otapFlash.addListener(this);
		otapInit.addListener(this);
    }

	/**
	 * @param packet
	 *
	 * @return
	 */
	boolean otapBroadcast(byte[] packet) {
		//log.debug("Otap::broadcast: packet len " + packet.length + ": " + packet[0] + " " + packet[1]);
		// Compute CRC over packet 
		byte crc[] = {(byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA};
		for (int i = 0; i < packet.length; i++) {
			crc[i % 4] = (byte) (0xFF & (crc[i % 4] ^ packet[i]));
		}

		log.debug("crc " + StringUtils.toHexString(crc));
		byte p[] = new byte[packet.length + 4];
		for (int i = 0; i < packet.length; i++) {
			p[i] = packet[i];
		}
		for (int i = 0; i < 4; i++) {
			p[i + packet.length] = crc[i];
		}
		// End: Compute CRC over packet

		if (useOtapKey) {
			//log.debug("p: " + Tools.toHexString(p));
			byte[] p2 = new byte[p.length - 1];
			System.arraycopy(p, 1, p2, 0, p2.length);
			//log.debug("p2: " + Tools.toHexString(p2));
			byte[] b = outaes.encode(p2);
			if (b == null) {
				return false;
			}
			p2 = new byte[b.length + 1];
			//log.debug("b: " + Tools.toHexString(b));
			System.arraycopy(b, 0, p2, 1, p2.length - 1);
			p2[0] = p[0];
			p = p2;
			//log.debug("p: " + Tools.toHexString(p));
		}


		if (motapSupportEnabled) {
            com.coalesenses.otap.core.seraerial.SerialRoutingPacket serialRoutingPacket =
                    new SerialRoutingPacket(p, PacketTypes.ISenseRoutings.ISENSE_ISI_ROUTING_TREE_ROUTING, 0);
            serialRoutingPacket.setDest(0xFFFF);
            return connector.transmit(serialRoutingPacket);
		} else {
            com.coalesenses.otap.core.seraerial.SerAerialPacket
                    serAerialPacket = new com.coalesenses.otap.core.seraerial.SerAerialPacket(p);
            serAerialPacket.setDest(0xFFFF);
			return connector.transmit(serAerialPacket);
		}

	}

	/**
	 * @param enable
	 *
	 * @return
	 */
	public synchronized boolean setPresenceDetectState(boolean enable) {

		if (enable && state == State.None) {
			otapStop();
			setState(State.PresenceDetect);
			presenceDetect.setDetectionEnabled(true);
			scheduledFuture = timer.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					timerServiceTimeout();
				}
			}, 1000, 1000, TimeUnit.MILLISECONDS
			);
			log.info("OTAP::Started PresenceDetect.");
			return true;
		}

		if (!enable && state == State.PresenceDetect) {
			setState(State.None);
			presenceDetect.setDetectionEnabled(false);
			log.info("OTAP::Stopped PresenceDetect.");
			return true;
		}

		return false;
	}

	/**
	 * @param filename
	 */
	public void loadBinProgram(String filename) {
		log.info("Loading OTAP file: " + filename);
		ChipType oldChipType = null;
		if (program != null) {
			oldChipType = program.getChipType();
		}

		this.programFilename = filename;
		if (filename != null && !"".equals(filename)) {
			try {
				//program = new BinProgram(new File(filename), 235);
				program = new com.coalesenses.otap.core.BinProgram(new File(filename));
				if (program != null) {
					if (oldChipType == null || oldChipType != program.getChipType()) {
						for (com.coalesenses.otap.core.OtapDevice d : presenceDetect.getDetectedDevices()) {
							d.setProgrammable(program.getChipType(), this);
						}
					}
				}
			} catch (FileNotFoundException e) {
				log.warn("File not found: " + e, e);
			} catch (IOException e) {
				log.warn("I/O Error: " + e, e);
			}
		}
	}

	/**
	 * @return
	 */
	public String getProgramFilename() {
		return programFilename;
	}

	/**
	 *
	 */
	public void otapStart() {
		otapStop();
		loadBinProgram(programFilename);

		if (program == null) {
			log.error("No file to flash. Select a bin-file first.");
		} else {
			log.info("Started OTAP Init phase."); // TODO: timings
			if (motapSupportEnabled)
			//otapInit.setDeviceSettingTimeoutMultiplierMs(60);//Settings.instance().getInt(Settings.SettingsKey.timeout_multiplier));
			{
				otapInit.setDeviceSettingTimeoutMultiplierMs(100);				
			} else {
				otapInit.setDeviceSettingTimeoutMultiplierMs(10
				);
			}//Settings.instance().getInt(Settings.SettingsKey.timeout_multiplier));
			otapInit.setDeviceSettingMaxRerequests(30);
			//otapInit.setDeviceSettingMaxRerequests((Settings.instance().getInt(Settings.SettingsKey.max_retries)));
			setState(State.OtapInit);

			otapInit.startOtapInit(program);
			scheduledFuture = timer.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					timerServiceTimeout();
				}
			}, 1000, 1000, TimeUnit.MILLISECONDS
			);
		}

	}

	/**
	 *
	 */
	public void otapStop() {
		if (getState() != State.None) {
			log.info("Stopped OTAP programming.");
		}
		if (scheduledFuture != null) {
			scheduledFuture.cancel(true);
			scheduledFuture = null;
		}
		log.debug("1");
		presenceDetect.setDetectionEnabled(false);
		log.debug("2");
		otapInit.stopOtapInit();
		log.debug("3");
		otapFlash.cancelOtap();

		log.debug("4");
		setState(State.None);
		log.debug("5");
		setOtapStatusText("Stopped");
	}

	/**
	 * @return
	 */
	public State getState() {
		return state;
	}

	/**
	 * @param state
	 */
	private void setState(State state) {
		log.debug("State transition: " + this.state + " -> " + state);
		this.state = state;		
	}

	/**
	 * Invoked by the TimerService on timeout
	 */
	public void timerServiceTimeout() {

		if ((getState() == State.OtapInit) &&
				((this.otapInit.getStartingTime().s() > (24 + this.otapInit.getSelectedDevices().size())) // TODO
						|| (this.otapInit.getSelectedDevices().size() == this.otapInit.getParticipatingDevices()
						.size()))) {
			otapInit.stopOtapInit();
			if (otapInit.getParticipatingDevices().size() > 0) {
				log.info("Switching to Otap mode"); // TODO: timings
				if (motapSupportEnabled)
				//otapFlash.setMsPerPacket(60);//Settings.instance().getInt(Settings.SettingsKey.timeout_multiplier));
				{
					otapFlash.setMsPerPacket(100);
				} else {
					otapFlash.setMsPerPacket(10);
				}//Settings.instance().getInt(Settings.SettingsKey.timeout_multiplier));
				otapFlash.setMaxRetries(30);
				otapFlash.startOtap(program, otapInit.getParticipatingDevices());
				setState(State.Otap);
				if (scheduledFuture != null) {
					scheduledFuture.cancel(true);
					scheduledFuture = null;
				}
			} else {
				log.info("No init reply received");
				otapStop();
			}
		}
	}

	/**
	 * Invoked by the PresenceDetect class on state changes of a device
	 *
	 * @param d
	 * @param cause
	 */
	public void presenceDetectChange(com.coalesenses.otap.core.OtapDevice d, Change cause) {

		if (cause == Change.Added) {
			log.debug("found device {}",d.getId());
		} else if (cause == Change.Removed) {
			log.debug("lost device {}",d.getId());
		} else if (cause == Change.Updated) {
			log.debug("updated device {}",d.getId());
		}
	}

	/**
	 * After devices have been detected, this method should be invoked to actually select the participating devices. This
	 * is usually called by the gui prior to the start of the otap init phase to set the user-selected devices.
	 *
	 * @param devices
	 *
	 * @return
	 */
	public boolean setParticipatingDeviceList(Collection<com.coalesenses.otap.core.OtapDevice> devices) {
		if (getState() == State.None || getState() == State.PresenceDetect) {
			otapInit.setParticipatingDevices(devices);
			return true;
		} else {
			log.warn("Ignoring to set participating devices in state " + getState().name());
			return false;
		}

	}

	/**
	 * Invoked be the OtapFlasher on state changes of a certain device
	 *
	 * @param device
	 */
	public void otapFlashUpdate(com.coalesenses.otap.core.OtapDevice device) {
		log.info("flsah progress for device {}: {}",device.getId(),((int)(device.getProgress()*100)));
	}

	/**
	 * Invoked by the OtapFlasher when done
	 */
	public void otapDone() {
		log.debug("Otap done signalled. Stopping.");
		otapStop();
		setOtapStatusText("Done");
        seraerialShutdown();
	}

	/**
	 * @param statusString
	 */
	void setOtapStatusText(String statusString) {
		this.statusText = statusString;
	}

	/**
	 * @return
	 */
	String getOtapStatusText() {
		return statusText;
	}

	/**
	 * @return
	 */
	ChipType getProgramChipType() {
		if (program != null) {
			return program.getChipType();
		}
		return ChipType.Unknown;
	}

	/**
	 * @return
	 */
	com.coalesenses.otap.core.BinProgram.OtapFunctionality getProgramOtapFunctionality() {
		if (program != null) {
			return program.getOtapFunctionality();
		}
		return com.coalesenses.otap.core.BinProgram.OtapFunctionality.Nothing;
	}

	/**
	 * Invoked by the SerAerialPlugin on the reception of a packet
	 *
	 * @param p
	 */	
	public void handleDevicePacket(SerAerialPacket p) {
		byte[] buffer = p.getContent();
		if (buffer.length > 0) {
			if (buffer[0] == PacketTypes.OTAP) {
				//log.debug("otap rx");
				if (useOtapKey) {

					//log.debug("rx from " + Tools.toHexString(p.getSrc()) + ": " + Tools.toHexString(buffer));
					byte[] p2 = new byte[buffer.length - 1];
					System.arraycopy(buffer, 1, p2, 0, p2.length);
					byte[] b = inaes.decode(p2);
					if (b == null) {
						log.warn("Decode failed");
						return;
					}
					p2 = new byte[b.length + 1];
					System.arraycopy(b, 0, p2, 1, p2.length - 1);
					p2[0] = buffer[0];
					buffer = p2;
					//log.debug("decoded: " + Tools.toHexString(buffer));

					p.setContent(buffer);
				}

				//	handlers.dispatch(p);
				MacroFabricSerializer.DataTypeCode type = MacroFabricSerializer.getDataTypeCode(buffer);
				if (type == MacroFabricSerializer.DataTypeCode.PRESENCEDETECTREPLY) {
					presenceDetect.handle(p);
				} else if (type == MacroFabricSerializer.DataTypeCode.OTAPINITREPLY) {
					//log.debug("rx init reply from " + Tools.toHexString(p.getSrc()));
					otapInit.handle(p);
				} else if (type == MacroFabricSerializer.DataTypeCode.OTAPPROGRAMREPLY) {
					//log.debug("rx program reply from " + Tools.toHexString(p.getSrc()));
					TimeDiff diff = new TimeDiff();
					otapFlash.handle(p);
					System.out.println("Message dispatching took " + diff.ms() + "ms ");
				}

			} else if (buffer[0] == (byte) 8) // TREE_DATA
			{
				int sink = (int) ((((0xFF & buffer[2]) << 8) + (0xFF & buffer[1])));
				//if (sink == os_.id()) // this packet is for me, because I am a sink
				{
					int src = (int) ((((0xFF & buffer[4]) << 8) + (0xFF & buffer[3])));
					log.debug("TREE: sink received packet from " + StringUtils.toHexString(src));
					com.coalesenses.otap.core.seraerial.SerAerialPacket payload = p;
					payload.setSrc(src);
					byte content[] = new byte[buffer.length - 6];
					for (int i = 0; i < buffer.length - 6; i++) {
						content[i] = buffer[i + 6];
					}
					payload.setContent(content);
					handleDevicePacket(payload);
				}
			}

		}
	}

	public void seraerialShutdown() {
		log.debug("Plug-in disabled. Stopping otap.");
		otapStop();
        connector.shutdown();
	}

	public String getDescription() {
		return "Provides a mechanism to program a lot of devices at once by broadcasting the program over a proxy device.";
	}

	public String getName() {
		return "Over the Air Programming";
	}

	/**
	 * @return
	 */
	public int getOtapProtocolVersion() {
		return otapProtocolVersion;
	}

	/**
	 * @return
	 */
	public int getMotapProtocolVersion() {
		return motapProtocolVersion;
	}

	/**
	 * @param integer
	 */
	public void setChannel(Integer integer) {
		channel = integer;
	}

	/**
	 * Invoked by the PresenceDetect instance when presence detection is started.
	 */
	public void sendSetChannel() {
		byte[] tmp = new byte[2];
		tmp[0] = 2;
		tmp[1] = (byte) (channel & 0xFF);
        MessagePacket p = new MessagePacket(PacketTypes.ISENSE_ISHELL_INTERPRETER & 0xFF, tmp);

		connector.send(PacketTypes.ISENSE_ISHELL_INTERPRETER, tmp);

	}

	/**
	 * @return
	 */
	public PresenceDetect getPresenceDetect() {
		return presenceDetect;
	}

	/**
	 * @param b
	 */
	public void setMultihopSupportState(boolean b) {
		motapSupportEnabled = b;
	}

	/**
	 * @return
	 */
	public boolean getMultihopSupportState() {
		return motapSupportEnabled;
	}

	/**
	 * @param id
	 */
	public void handleId(int id) {
		log.debug("Setting id to " + id);
		this.id = id;
	}

	/**
	 *
	 */
	public void warnOtapFunctionality() {
		if (getProgramOtapFunctionality() == com.coalesenses.otap.core.BinProgram.OtapFunctionality.Otap &&
				getMultihopSupportState()) {
			log.warn("The new Program does not support Multihop OTAP functionality!");
			return;
		}

		if (getProgramOtapFunctionality() == com.coalesenses.otap.core.BinProgram.OtapFunctionality.Nothing) {
			log.warn("The new Program does not support OTAP functionality!");
			return;
		}

	}

	/**
	 *
	 */
	public void createTree() {
		byte[] emptyPacket = new byte[4];
		emptyPacket[0] = PacketTypes.OTAP;
		emptyPacket[1] = (byte) 0xFF;
		emptyPacket[2] = (byte) 0xFF;
		emptyPacket[3] = (byte) 0xFF;
		otapBroadcast(emptyPacket);

	}

	/**
	 * @param key
	 */
	public void setRadioKey(String key) {
		if (key.length() == 0 || key.length() == 32) {
			byte[] tmp = new byte[18];
			tmp[0] = 5;
			if (key.length() == 0) {
				for (int i = 0; i < 16; i++) {
					tmp[i + 1] = 0;
				}
				tmp[17] = 0;
				log.debug("Removing radio key");
			} else {
				for (int i = 0; i < 16; i++) {
					tmp[i + 1] = (byte) Integer.parseInt(key.substring(i * 2, i * 2 + 2), 16);

				}
				tmp[17] = 1;
				log.debug("Setting radio key: " + StringUtils.toHexString(tmp, 1, 16));
			}
			connector.send(PacketTypes.ISENSE_ISHELL_INTERPRETER, tmp);
		} else {
			log.warn("wrong key length: must be 0 or 32, but is " + key.length());
		}

	}

	/**
	 * @param key
	 * @param use
	 */
	public void setOtapKey(String key, boolean use) {
		useOtapKey = use;
		if (use) {
			if (key != null) {
				if (key.length() == 32) {
					byte[] otapKey = new byte[16];
					for (int i = 0; i < 16; i++) {
						otapKey[i] = (byte) Integer.parseInt(key.substring(i * 2, i * 2 + 2), 16);

					}
					log.debug("Setting otap key: " + StringUtils.toHexString(otapKey));
					inaes.setKey(otapKey);
					outaes.setKey(otapKey);
				} else {
					log.warn("Failed to set otap key, key too short: Should be 32, but is " + key.length());
				}

			} else {
				log.warn("Failed to set otap key, key is null");
			}
		} else {
			log.debug("Unsetting otap key");
		}

	}


    public void setProgramFilename(String programFilename) {
        this.programFilename = programFilename;
    }
}
