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

package de.uniluebeck.itm.wsn.devicedrivers.telosb;

import gnu.io.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for communicating with telosb motes via I2C
 *
 * @author Friedemann Wesner
 */
public class TelosI2CCom {
	private static final Logger log = LoggerFactory.getLogger(TelosI2CCom.class);

	private SerialPort serialPort = null;

	/**
	 * Constructor
	 *
	 * @param serialPort
	 */
	public TelosI2CCom(SerialPort serialPort) {
		//log.debug("TelosI2CCom initialized with port: "+serialPort.getName());
		this.serialPort = serialPort;
	}

	private void setSDA(boolean value) {
		if (serialPort != null) {
			serialPort.setDTR(!value);
		} else {
			log.error("can not set SDA, serialPort is null");
		}
	}

	private void setSCL(boolean value) {
		if (serialPort != null) {
			serialPort.setRTS(!value);
		} else {
			log.error("can not set SCL, serialPort is null");
		}
	}

	private void I2CStart() {
		//log.debug("I2C start");
		setSDA(true);
		setSCL(true);
		setSDA(false);
	}

	private void I2CStop() {
		//log.debug("I2C stop");
		setSDA(false);
		setSCL(true);
		setSDA(true);
	}

	private void writeBit(int bitValue) {
		if (bitValue < 0 || bitValue > 1) {
			log.error(" * error: " + bitValue + " is no valid bit value.");
			return;
		}
		//log.debug("write bit "+bitValue);

		setSCL(false);
		setSDA(bitValue == 1);
		sleepMicro(2);
		setSCL(true);
		sleepMicro(1);
		setSCL(false);
	}

	private void writeByte(int data) {
		//log.debug("write byte: "+data);

		// write 8 bits, starting with msb 
		for (int i = 7; i >= 0; i--) {
			writeBit(getBitValue(data, i));
		}
		// acknowledge
		writeBit(0);
	}

	/**
	 * Write command byte to telos mote via I2C
	 *
	 * @param address
	 * @param cmdByte
	 */
	public void writeCommand(int address, int cmdByte) {
		//log.debug(" * writing I2C command "+cmdByte+" at address "+address);

		I2CStart();
		writeByte(0x90 | (address << 1));
		writeByte(cmdByte);
		I2CStop();
	}

	private synchronized void sleepMicro(int microSec) {
		try {
			this.wait(0, microSec * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private int getBitValue(int data, int bitNo) {
		if (bitNo < 0 || bitNo > 7) {
			return -1;
		}

		return (data >> bitNo) & 0x01;
	}
}
