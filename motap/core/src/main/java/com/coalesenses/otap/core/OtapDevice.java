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

import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.generic.ChipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Pfisterer
 */
public class OtapDevice implements Comparable<OtapDevice> {

	/**
	 *
	 */
	private static final Logger log = LoggerFactory.getLogger(OtapDevice.class);

	/**
	 *
	 */
	private int id;

	/**
	 *
	 */
	private ChipType chipType = ChipType.Unknown;

	/**
	 *
	 */
	private boolean fitsToBinFileAndProtocolVersion = false;

	/**
	 *
	 */
	private int lqi;

	/**
	 *
	 */
	private TimeDiff lastReception = new TimeDiff();

	/**
	 *
	 */
	private TimeDiff lastMessageTransmitted = new TimeDiff();

	/**
	 *
	 */
	private boolean chunkComplete = false;

	/**
	 *
	 */
	private int chunkNo = -1;

	/**
	 *
	 */
	private String statusMessage = null;

	/**
	 *
	 */
	private double progress = 0.0;

	/**
	 * Checks whether the received OTAP protocol version matches the protocol version of iShell.
	 */
	private int protocolVersion = 0;

	/**
	 *
	 */
	private long applicationID = 0;

	/**
	 *
	 */
	private int softwareRevision = 0;

	@Override
	public String toString() {
		return "id: " + id + ", lqi: " + lqi + ", last heartbeat " + lastReception.ms() + "ms ago";
	}

	/**
	 *
	 */
	public int compareTo(OtapDevice d) {
		if (d == null) {
			return 1;
		}

		if (d == this) {
			return 0;
		}

		if (getId() == d.getId()) {
			return 0;
		}

		if (getId() - d.getId() > 0) {
			return 1;
		}

		return -1;
	}

	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof OtapDevice)) {
			return false;
		}
		return compareTo((OtapDevice) arg0) == 0;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public TimeDiff getLastReception() {
		return lastReception;
	}

	public TimeDiff getLastMessageTransmitted() {
		return lastMessageTransmitted;
	}

	public void setLastReception() {
		this.lastReception.touch();
	}

	public void setLastMessageTransmitted() {
		this.lastMessageTransmitted.touch();
	}

	public int getLqi() {
		return lqi;
	}

	public void setLqi(int lqi) {
		this.lqi = lqi;
	}

	public boolean isChunkComplete() {
		return chunkComplete;
	}

	public void setChunkComplete(boolean chunkComplete) {
		this.chunkComplete = chunkComplete;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String message) {
		statusMessage = message;
	}

	public double getProgress() {
		return progress;
	}

	public void setProgress(double progress) {
		this.progress = progress;
	}

	public ChipType getChipType() {
		return chipType;
	}

	public void setChipType(ChipType chipType) {
		this.chipType = chipType;
	}

	public boolean programmable() {
		return fitsToBinFileAndProtocolVersion;
	}

	public void setProgrammable(ChipType chipType, OtapPlugin p) {
		// A device must response with the type of the bin program or type Shawn to be compatible
		this.fitsToBinFileAndProtocolVersion =
				((this.chipType.equals(chipType) || this.chipType.equals(ChipType.Shawn))
						//&& (this.protocolVersion == protocol));
						&& (getProtocolVersionOk(p)));
	}


	public void setProtocolVersion(short protocol_version) {
		this.protocolVersion = protocol_version;
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}


	public int getChunkNo() {
		return chunkNo;
	}


	public void setChunkNo(int chunkNo) {
		this.chunkNo = chunkNo;
	}


	public long getApplicationID() {
		return applicationID;
	}


	public void setApplicationID(long applicationID) {
		this.applicationID = applicationID;
	}


	public int getSoftwareRevision() {
		return softwareRevision;
	}


	public void setSoftwareRevision(int softwareRevision) {
		this.softwareRevision = softwareRevision;
	}

	/**
	 *
	 */
	public boolean getProtocolVersionOk(OtapPlugin p) {
		if (!p.getMultihopSupportState()) {
			return ((protocolVersion == p.getMotapProtocolVersion()) ||
					(protocolVersion == p.getOtapProtocolVersion()));
		} else {
			return (protocolVersion == p.getMotapProtocolVersion());
		}
	}

	/**
	 *
	 */
	public String getProtocolVersionAsString(OtapPlugin p) {
		if (protocolVersion == p.getMotapProtocolVersion()) {
			return "MOTAP";
		} else if (protocolVersion == p.getOtapProtocolVersion()) {
			return "OTAP";
		} else {
			return "UNKNOWN " + protocolVersion;
		}
	}

}
