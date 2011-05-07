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

package de.uniluebeck.itm.wsn.devicedrivers.jennic;

import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.exceptions.*;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import gnu.io.*;

import java.io.*;

/**
 * @author Markus
 */
public class JennicDevice extends iSenseDeviceImpl implements SerialPortEventListener {

	private static final int MAX_RETRIES = 5;

	private InputStream inStream = null;

	/** */
	private enum ComPortMode {

		Normal, Program
	}

	private OutputStream outStream = null;

	private SerialPort serialPort = null;

	private Object dataAvailableMonitor = new Object();

	private String port;

	private boolean old_gateway = false;

	private boolean rebootAfterFlashing = true;

	private int readBaudrate = 115200;

	private int flashBaudrate = 38400;

	private int stopbits = SerialPort.STOPBITS_1;

	private int databits = SerialPort.DATABITS_8;

	private int parityBit = SerialPort.PARITY_NONE;

	private int receiveTimeout = 2000;

	private boolean connected;

	public JennicDevice(String port) {
		this.port = port;
		connect();
	}

	public boolean connect() {
		if (port == null) {
			return false;
		}


		if (port != null && serialPort == null) {
			try {
				setSerialPort(port);

				if (serialPort == null) {
					logDebug("connect(): serialPort==null");
				}

			} catch (PortInUseException piue) {
				logWarn("Port {} already in use. Connection will be removed. ", port);

				if (serialPort != null) {
					serialPort.close();
				}

				return false;

			} catch (Exception e) {

				if (serialPort != null) {
					serialPort.close();
				}

				logWarn("Port {} does not exist. Connection will be removed. ", port);
				return false;

			}
			return true;
		}
		return true;
	}

	// -------------------------------------------------------------------------

	/**
	 * @param port
	 * @throws Exception
	 */
	public void setSerialPort(String port) throws Exception {
		logDebug("JennicDevice.setSerialPort({})", port);
		CommPortIdentifier cpi = CommPortIdentifier.getPortIdentifier(port);
		SerialPort sp = null;
		CommPort commPort = null;
		for (int i = 0; i < MAX_RETRIES; i++) {
			try {
				commPort = cpi.open(this.getClass().getName(), 1000);
				break;
			} catch (PortInUseException piue) {
				logDebug("Port in Use Retrying to connect");
				if (i >= MAX_RETRIES - 1) {
					throw (piue);
				}
				Thread.sleep(200);
			}
		}
		if (commPort instanceof SerialPort) {
			sp = (SerialPort) commPort;// cpi.open("iShell", 1000);
		} else {
			logDebug("Port is no SerialPort");
		}
		serialPort = sp;
		serialPort.addEventListener(this);
		serialPort.notifyOnDataAvailable(true);

		setComPort(ComPortMode.Normal);

		outStream = new BufferedOutputStream(serialPort.getOutputStream());
		inStream = new BufferedInputStream(serialPort.getInputStream());
		connected = true;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public boolean reset() {

		if (old_gateway) {
			logDebug("Resetting device old style");
			serialPort.setDTR(false);
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
			serialPort.setDTR(true);
		} else {
			logDebug("Resetting device new style");
			serialPort.setDTR(true);
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
			serialPort.setDTR(false);
		}

		return true;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */

	public boolean enterProgrammingMode() throws Exception {

		setComPort(ComPortMode.Program);
		// Go into programming mode (only for iSense devices)
		if (rebootAfterFlashing) {

			// Old gateway style or new?
			if (old_gateway) {
				serialPort.setDTR(false);
				Thread.sleep(200);
				serialPort.setRTS(true);
				Thread.sleep(200);
				serialPort.setDTR(true);
				Thread.sleep(200);
				serialPort.setRTS(false);
				// log.error("Entered programming mode the old iSense style");
			} else {
				serialPort.setDTR(true);
				Thread.sleep(200);
				serialPort.setRTS(true);
				Thread.sleep(200);
				serialPort.setDTR(false);
				Thread.sleep(200);
				serialPort.setRTS(false);
				// log.debug("Entered programming mode the new iSense style");
			}

		}

		// log.debug("Com port in programming mode");
		return true;
	}

	// -------------------------------------------------------------------------

	/**
	 * @throws UnsupportedCommOperationException
	 *
	 */
	private void setComPort(ComPortMode mode) throws UnsupportedCommOperationException {
		int baudrate = (mode == ComPortMode.Program) ? flashBaudrate : readBaudrate;
		serialPort.setSerialPortParams(baudrate, databits, stopbits, parityBit);

		// Is this an iSense device or a Jennic eval kit
		if (rebootAfterFlashing) {

			// Old gateway or new gateway?
			if (old_gateway) {
				serialPort.setDTR(true);
				serialPort.setRTS(false);
				logDebug("Setting COM-Port parameters (old style): baudrate: " + baudrate);
			} else {
				serialPort.setDTR(false);
				serialPort.setRTS(false);
				logDebug("Setting COM-Port parameters (new style): baudrate: " + baudrate);
			}

		} else {

			// Go into programming mode (jennic eval kit style)
			if (mode == ComPortMode.Program) {
				serialPort.setRTS(true);
			} else {
				serialPort.setRTS(false);
			}

		}

	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */

	public void leaveProgrammingMode() throws Exception {
		// log.debug("Leaving programming mode.");

		// Restore normal settings
		flushReceiveBuffer();
		setComPort(ComPortMode.Normal);

		// Reboot (if requested by the user)
		if (rebootAfterFlashing) {
			logDebug("Rebooting device");
			reset();
		}

		// log.debug("Done. Left programming mode.");
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */

	public ChipType getChipType() throws Exception {
		
		ChipType chipType = ChipType.Unknown;
		
		sendBootLoaderMessage(Messages.chipIdMessage());
		
		try {
			// Read chip type read response
				byte[] res = receiveBootLoaderReply(Messages.CHIP_ID_RESPONSE);
				String S = "received: (len="+res.length+") ";
				for (int i = 0; i < res.length; i++)
					S = S + "res["+i+"]="+StringUtils.toHexString(res[i]) + " ";
				logDebug(S);
				if (res.length == 6)
				{
					if ( ((res[1]==0) && (res[2]==0x10) && (res[3]==0x40) && (res[4]==0x46) && (res[5]==(byte)0x86)) ||
							((res[1]==0) && (res[2]==0x10) && (res[3]==(byte)0x80) && (res[4]==0x46) && (res[5]==(byte)0x86)) )
					{
						chipType = ChipType.JN5148;
						logDebug("Chip identified as " + chipType + ".");
						/*sendBootLoaderMessage(Messages.changeBaudRateMessage());
						try {
							res = receiveBootLoaderReply(Messages.CHANGE_BAUD_RATE_RESPONSE);
							if (res[1]==0)
							{
								log.debug("receive baud rate response --> setting baud rate to 115200.");
								int baudrate = 115200;
								int databits = Settings.instance().getInt(SettingsKey.databits);
								int stopbits = Settings.instance().getInt(SettingsKey.stopbits);
								int parity = Settings.instance().getInt(SettingsKey.parity);
								serialPort.setSerialPortParams(baudrate, databits, stopbits, parity);
								
							}
						}
						catch (UnexpectedResponseException e ) {}*/
					}
					else logError("BYTES incorrect res[1]="+res[1]+" != "+0+ "res[2]="+res[2]+" != "+0x10+ "res[3]="+res[3]+" != "+0x40+ "res[4]="+res[4]+" != "+0x46+ "res[5]="+res[5]+" != "+(byte)0x86);
				} else logError("length incorrect");
			} 
			catch (UnexpectedResponseException e)
			{
		
				// Send chip type read request
				sendBootLoaderMessage(Messages.ramReadRequestMessage(0x100000FC, 0x0004));
		
				// Read chip type read response
				byte[] response = receiveBootLoaderReply(Messages.RAM_READ_RESPONSE);
		
				// Throw error if reading failed
				if (response[1] != 0x00) {
					logError(String.format("Failed to read chip type from RAM: Response should be 0x00, yet it is: 0x%02x",
							response[1]
					)
					);
					throw new RamReadFailedException();
				}
		
				chipType = ChipType.Unknown;
		
				if (response[2] == 0x00 && response[3] == 0x20) {
					chipType = ChipType.JN513X;
				} else if (response[2] == 0x10 && response[3] == 0x00) {
					chipType = ChipType.JN513XR1;
				} else if (response[2] == 0x20 && response[3] == 0x00) {
					chipType = ChipType.JN5121;
				} else {
					logError("Defaulted to chip type JN5121. Identification may be wrong.");
					chipType = ChipType.JN5121;
				}
		
				logDebug("Chip identified as " + chipType + " (received " + StringUtils.toHexString(response[2]) + " "
						+ StringUtils.toHexString(response[3]) + ")"
				);
			}
		return chipType;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */

	public FlashType getFlashType() throws Exception {
		// Send flash type read request
		sendBootLoaderMessage(Messages.flashTypeReadRequestMessage());

		// Read flash type read response
		byte[] response = receiveBootLoaderReply(Messages.FLASH_TYPE_READ_RESPONSE);

		// Throw error if reading failed
		if (response[1] != 0x00) {
			logError(String.format("Failed to read flash type: Response should be 0x00, yet it is: 0x%02x",
					response[1]
			)
			);
			throw new FlashTypeReadFailedException();
		}

		// Determine flash type
		FlashType ft = FlashType.Unknown;
		if (response[2] == (byte) 0xBF && response[3] == (byte) 0x49) {
			ft = FlashType.SST25VF010A;
		} else if (response[2] == (byte) 0x10 && response[3] == (byte) 0x10) {
			ft = FlashType.STM25P10A;
		} else if (response[2] == (byte) 0x1F && response[3] == (byte) 0x60) {
			ft = FlashType.Atmel25F512;
		} else if (response[2] == (byte) 0x12 && response[3] == (byte) 0x12) {
			ft = FlashType.STM25P40;
		} else {
			ft = FlashType.Unknown;
		}

		// log.debug("Flash is " + ft + " (response[2,3] was: " + Tools.toHexString(response[2]) + " " +
		// Tools.toHexString(response[3]) + ")");
		return ft;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	public byte[] getFlashHeader() throws Exception {
		ChipType chipType = getChipType();
		int headerStart = ChipType.getHeaderStart(chipType);
		int headerLength = ChipType.getHeaderLength(chipType);
		return readFlash(headerStart, headerLength);
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */

	public void triggerGetMacAddress(boolean rebootAfterFlashing) {
		this.rebootAfterFlashing = rebootAfterFlashing;
		logDebug("JennicDevice: trigger Mac Adress");
		if (operationInProgress()) {
			logError("Already another operation in progress (" + operation + ")");
			return;
		}
		operation = new ReadMacAddressOperation(this);
		operation.setLogIdentifier(logIdentifier);
		operation.start();

	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	@Override
	public boolean triggerProgram(IDeviceBinFile program, boolean rebootAfterFlashing) {
		this.rebootAfterFlashing = rebootAfterFlashing;
		if (operationInProgress()) {
			logError("Already another operation in progress (" + operation + ")");
			return false;
		}

		operation = new FlashProgramOperation(this, program, true);
		operation.setLogIdentifier(logIdentifier);
		operation.start();
		return true;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	@Override
	public boolean triggerReboot() throws Exception {
		logDebug("Starting reboot device thread");
		if (operationInProgress()) {
			logError("Already another operation in progress (" + operation + ")");
			return false;
		}

		operation = new RebootDeviceOperation(this);
		operation.setLogIdentifier(logIdentifier);
		operation.start();
		return true;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	@Override
	public void triggerSetMacAddress(MacAddress mac, boolean rebootAfterFlashing) {
		this.rebootAfterFlashing = rebootAfterFlashing;
		if (operationInProgress()) {
			logError("Already another operation in progress (" + operation + ")");
			return;
		}
		operation = new WriteMacAddressOperation(this, mac);
		operation.setLogIdentifier(logIdentifier);
		operation.start();
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	@Override
	public Operation getOperation() {
		if (operation == null) {
			return Operation.NONE;
		} else {
			return operation.getOperation();
		}
	}

	// ------------------------------------------------------------------------
	// --

	@Override
	public void shutdown() {
		// if (connected) {

		try {
			if (inStream != null) {
				inStream.close();
			}
		} catch (IOException e) {
			logDebug("Failed to close in-stream :" + e, e);
		}
		try {
			if (outStream != null) {
				outStream.close();
			}
		} catch (IOException e) {
			logDebug("Failed to close out-stream :" + e, e);
		}
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
			connected = false;
			serialPort = null;
		}
	}

	// }

	// ------------------------------------------------------------------------
	// --

	@Override
	public byte[] readFlash(int address, int len) throws Exception {

		// Send flash program request
		sendBootLoaderMessage(Messages.flashReadRequestMessage(address, len));

		// Read flash program response
		byte[] response = receiveBootLoaderReply(Messages.FLASH_READ_RESPONSE);

		// Remove type and success octet
		byte[] data = new byte[response.length - 2];
		System.arraycopy(response, 2, data, 0, response.length - 2);

		// Return data
		return data;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	@Override
	public byte[] writeFlash(int address, byte[] data, int offset, int len) throws Exception {
		// Send flash program request
		// logDebug("Sending program request for address " + address + " with " + data.length + " bytes");
		sendBootLoaderMessage(Messages.flashProgramRequestMessage(address, data));

		// Read flash program response
		byte[] response = receiveBootLoaderReply(Messages.FLASH_PROGRAM_RESPONSE);

		// Throw error if writing failed
		if (response[1] != 0x0) {
			logError(String
					.format("Failed to write to flash: Response should be 0x00, yet it is: 0x%02x", response[1])
			);
			throw new FlashProgramFailedException();
		} else {
			// logDebug("Received Ack");

		}

		return response;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	protected void configureFlash(ChipType chipType) throws Exception {
		logDebug("Configuring flash");

		// Only new chips need to be configured
		if (chipType != ChipType.JN5121) {
			// Determine flash type
			FlashType flashType = getFlashType();

			// Send flash configure request
			sendBootLoaderMessage(Messages.flashConfigureRequestMessage(flashType));

			// Read flash configure response
			byte[] response = receiveBootLoaderReply(Messages.FLASH_CONFIGURE_RESPONSE);

			// Throw error if configuration failed
			if (response[1] != 0x00) {
				logError(String.format("Failed to configure flash ROM: Response should be 0x00, yet it is: 0x%02x",
						response[1]
				)
				);
				throw new FlashConfigurationFailedException();
			}
		}
		logDebug("Done. Flash is configured");
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	void enableFlashErase() throws Exception {
		// logDebug("Setting FLASH status register to zero");
		sendBootLoaderMessage(Messages.statusRegisterWriteMessage((byte) 0x00)); // see
		// AN
		// -
		// 1007

		byte[] response = receiveBootLoaderReply(Messages.WRITE_SR_RESPONSE);

		if (response[1] != 0x0) {
			logError(String.format("Failed to write status register."));
			throw new FlashEraseFailedException();
		}
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	@Override
	public void eraseFlash() throws Exception {
		enableFlashErase();
		logDebug("Erasing flash");
		sendBootLoaderMessage(Messages.flashEraseRequestMessage());

		byte[] response = receiveBootLoaderReply(Messages.FLASH_ERASE_RESPONSE);

		if (response[1] != 0x0) {
			logError(String.format("Failed to erase flash."));
			throw new FlashEraseFailedException();
		}
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	@Override
	public void eraseFlash(Sectors.SectorIndex sector) throws Exception {
		enableFlashErase();
		logDebug("Erasing sector " + sector);
		sendBootLoaderMessage(Messages.sectorEraseRequestMessage(sector));

		byte[] response = receiveBootLoaderReply(Messages.SECTOR_ERASE_RESPONSE);

		if (response[1] != 0x0) {
			logError(String.format("Failed to erase flash sector."));
			throw new SectorEraseException(sector);
		}

	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	public void serialEvent(SerialPortEvent event) {
		// logDebug("Serial event");
		switch (event.getEventType()) {
			case SerialPortEvent.DATA_AVAILABLE:

				synchronized (dataAvailableMonitor) {
					// logDebug("DM");
					dataAvailableMonitor.notifyAll();
				}

				if (operation == null) {
					receive(inStream);
				} else {
					// TODO Callback Methode fr den Programming mode
				}

				break;
			default:
				logDebug("Serial event (other than data available): " + event);
				break;
		}
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public void send(MessagePacket p) throws IOException {
		// logDebug("JD: Sending " + p);

		if (operationInProgress()) {
			logError("Skipping packet. Another operation already in progress ({})", operation.getClass().getName());
			return;
		}

		byte type = (byte) (0xFF & p.getType());
		byte b[] = p.getContent();
		
		boolean iSenseStyle = true;
		
		// if the type was set to 0 send the message without iSense framing to the node
		// e.g. to Contiki or TinyOs Os
		
		// not fully implemented yet
		//if (type == 0x64)
		//	iSenseStyle = false;

		if (b == null || type > 0xFF) {
			logWarn("Skipping empty packet or type > 0xFF.");
			return;
		}
		if (b.length > 150) {
			logWarn("Skipping too large packet (length " + b.length + ")");
			return;
		}

		if (iSenseStyle == true){
			// Send start signal DLE STX
			this.outStream.write(DLE_STX);

			// Send the type escaped
			outStream.write(type);
			if (type == DLE) {
				outStream.write(DLE);
			}
	
			// Transmit each byte escaped
			for (int i = 0; i < b.length; ++i) {
				outStream.write(b[i]);
				if (b[i] == DLE) {
					outStream.write(DLE);
				}
			}
	
			// Send final DLT ETX
			outStream.write(DLE_ETX);
		} else{
			// Transmit the byte array without dle framing 
			for (int i = 0; i < b.length; ++i) {
				outStream.write(b[i]);
			}
		}
		outStream.flush();
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	private void sendBootLoaderMessage(byte[] message) throws IOException {
		// Allocate buffer for length + message + checksum
		byte[] data = new byte[message.length + 2];

		// Prepend length (of message + checksum)
		data[0] = (byte) (message.length + 1);

		// Copy message into the buffer
		System.arraycopy(message, 0, data, 1, message.length);

		// Calculate and append checksum
		data[data.length - 1] = Messages.calculateChecksum(data, 0, data.length - 1);

		// Send message
		outStream.write(data);
		outStream.flush();
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	protected byte[] receiveBootLoaderReply(int type) throws TimeoutException, UnexpectedResponseException,
			InvalidChecksumException, IOException, NullPointerException {

		waitDataAvailable(inStream, receiveTimeout);
		// Read message length
		int length = (int) inStream.read();

		// Allocate message buffer
		byte[] message = new byte[length - 1];

		// Read rest of the message (except the checksum
		for (int i = 0; i < message.length; ++i) {
			waitDataAvailable(inStream, timeoutMillis);
			message[i] = (byte) inStream.read();
		}

		// logDebug("Received boot loader msg: " + Tools.toHexString(message));

		// Read checksum
		waitDataAvailable(inStream, timeoutMillis);
		byte recvChecksum = (byte) inStream.read();

		// Concatenate length and message for checksum calculation
		byte[] fullMessage = new byte[message.length + 1];
		fullMessage[0] = (byte) length;
		System.arraycopy(message, 0, fullMessage, 1, message.length);

		// Throw exception if checksums diffe
		byte checksum = Messages.calculateChecksum(fullMessage);
		if (checksum != recvChecksum) {
			throw new InvalidChecksumException();
		}

		// Check if the response type is unexpected
		if (message[0] != type) {
			throw new UnexpectedResponseException(type, message[0]);
		}

		return message;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 * Wait at most timeoutMillis for the input stream to become available
	 *
	 * @param istream	   The stream to monitor
	 * @param timeoutMillis Milliseconds to wait until timeout, 0 for no timeout
	 * @return The number of characters available
	 * @throws IOException
	 */
	private int waitDataAvailable(InputStream istream, int timeoutMillis) throws TimeoutException, IOException {
		TimeDiff timeDiff = new TimeDiff();
		int avail = 0;

		while (isConnected() && inStream != null && (avail = inStream.available()) == 0) {
			if (timeoutMillis > 0 && timeDiff.ms() >= timeoutMillis) {
				logWarn("Timeout waiting for data (waited: " + timeDiff.ms() + ", timeoutMs:" + timeoutMillis + ")");
				throw new TimeoutException();
			}

			synchronized (dataAvailableMonitor) {
				try {
					dataAvailableMonitor.wait(50);
				} catch (InterruptedException e) {
					logError("Interrupted: " + e, e);
				}
			}
		}
		return avail;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	protected boolean waitForConnection() {
		try {
			// Send flash read request (in fact, this could be any valid message
			// to which the
			// device is supposed to respond)
			sendBootLoaderMessage(Messages.flashReadRequestMessage(0x24, 0x20));
			receiveBootLoaderReply(Messages.FLASH_READ_RESPONSE);
			logInfo("Device connection established");
			return true;
		} catch (TimeoutException to) {
			logDebug("Still waiting for a connection.");
		} catch (Exception error) {
			logWarn("Exception while waiting for connection", error);
		}

		flushReceiveBuffer();
		return false;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	protected void flushReceiveBuffer() {
		long i = 0;
		// logDebug("Flushing serial rx buffer");

		if (isConnected()) {

			try {
				while ((i = inStream.available()) > 0) {
					logDebug("Skipping " + i + " characters while flushing on the serial rx");
					inStream.skip(i);
				}
			} catch (IOException e) {
				logWarn("Error while serial rx flushing buffer: " + e, e);
			}
		}
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	@Override
	public String toString() {
		return "Device at " + serialPort != null ? serialPort.getName() : "<unknown>";
	}

	/**
	 * Returns the device's port name as String
	 *
	 * @return
	 */
	public String getPortName() {
		return port;
	}

	public SerialPort getSerialPort() {
		return serialPort;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public int[] getChannels() {
		int[] channels = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};
		return channels;
	}

	@Override
	public IDeviceBinFile loadBinFile(String fileName) throws Exception {
		return new JennicBinFile(fileName);
	}

	public boolean isConnected() {
		return connected;
	}

}
