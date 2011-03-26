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

package de.uniluebeck.itm.wsn.devicedrivers.pacemate;

import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.exceptions.FlashProgramFailedException;
import de.uniluebeck.itm.wsn.devicedrivers.exceptions.InvalidChecksumException;
import de.uniluebeck.itm.wsn.devicedrivers.exceptions.TimeoutException;
import de.uniluebeck.itm.wsn.devicedrivers.exceptions.UnexpectedResponseException;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.FlashType;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.Sectors;
import gnu.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Enumeration;

public class PacemateDevice extends iSenseDeviceImpl implements SerialPortEventListener {

	private InputStream inStream = null;

	private OutputStream outStream = null;

	private SerialPort serialPort = null;

	/**
	 * This is the Start Address in the RAM to write data
	 */
	public final long startAdressInRam = 1073742336;

	/**
	 * is The LPC2136 echo on
	 */
	public boolean echo_on = true;

	/** */
	private Object dataAvailableMonitor = new Object();

	/** */
	private enum ComPortMode {
		Normal, Program
	}

	private String serialPortName;

	private boolean oldGateway = false;

	private boolean rebootAfterFlashing = true;

	private int readBaudrate = 115200;

	private int flashBaudrate = 115200;

	private int stopbits = SerialPort.STOPBITS_1;

	private int databits = SerialPort.DATABITS_8;

	private int parityBit = SerialPort.PARITY_NONE;

	private int receiveTimeout = 2000;

	private static final int MAX_RETRIES = 5;

	private boolean connected;

	public PacemateDevice(String serialPortName) {
		this.serialPortName = serialPortName;
		connect();
	}

	public boolean connect() {
		if (serialPortName == null) {
			return false;
		}
		// if(!connected){
		if (serialPortName != null && serialPort == null) {
			try {
				setSerialPort(serialPortName);
				if (serialPort == null) {
					logDebug("connect(): serialPort==null");
				}
			} catch (PortInUseException piue) {
				logDebug("Port already in use. Connection will be removed. ");
				if (serialPort != null) {
					serialPort.close();
				}
				// this.owner.removeConnection(this);
				return false;
			} catch (Exception e) {
				if (serialPort != null) {
					serialPort.close();
				}
				logDebug("Port does not exist. Connection will be removed. " + e, e);
				return false;
			}
			return true;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public void setSerialPort(String port) throws Exception {
		logDebug("PacemateDevice.setSerialPort({})", port);
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
			sp = (SerialPort) commPort;
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

	public boolean reset() {

		logDebug("Resetting device Pacemate style");
		serialPort.setDTR(true);
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
		}
		serialPort.setDTR(false);

		return true;
	}

	public boolean enterProgrammingMode() throws Exception {

		setComPort(ComPortMode.Program);

		serialPort.setRTS(true);
		Thread.sleep(200);
		serialPort.setDTR(true);
		Thread.sleep(200);
		serialPort.setDTR(false);
		Thread.sleep(200);
		serialPort.setRTS(false);
		logInfo("Entered programming mode the Pacemate style");

		// logDebug("Com port in programming mode");
		return true;
	}

	private void setComPort(ComPortMode mode) throws UnsupportedCommOperationException {
		// SettingsKey baudRateKey = (mode == ComPortMode.Program) ? SettingsKey
		// .flash_baudrate : SettingsKey.read_baudrate;
		int baudrate = (mode == ComPortMode.Program) ? flashBaudrate : readBaudrate;

		logDebug("set com port " + baudrate + " " + databits + " " + stopbits + " " + parityBit);

		serialPort.setSerialPortParams(baudrate, databits, stopbits, parityBit);

		serialPort.setDTR(false);
		serialPort.setRTS(false);
		logDebug("Setting COM-Port parameters (new style): baudrate: " + serialPort.getBaudRate());
	}

	public void leaveProgrammingMode() throws Exception {
		// logDebug("Leaving programming mode.");

		// Restore normal settings
		flushReceiveBuffer();
		setComPort(ComPortMode.Normal);

		// Reboot (if requested by the user)
		if (rebootAfterFlashing) {
			logDebug("Rebooting device");
			reset();
		}

		// logDebug("Done. Left programming mode.");
	}

	public ChipType getChipType() throws Exception {
		// Send chip type read request
		sendBootLoaderMessage(Messages.ReadPartIDRequestMessage());

		// Read chip type read response
		String response = receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);

		ChipType chipType = ChipType.Unknown;

		if (response.compareTo("196387") == 0) {
			chipType = ChipType.LPC2136;
		} else {
			logError("Defaulted to chip type LPC2136 (Pacemate). Identification may be wrong." + response);
			chipType = ChipType.LPC2136;
		}

		logDebug("Chip identified as " + chipType + " (received " + response + ")");
		return chipType;
	}

	@Override
	public FlashType getFlashType() throws Exception {
		return FlashType.Unknown;
	}

	public byte[] getFlashHeader() throws Exception {
		ChipType chipType = getChipType();
		int headerStart = ChipType.getHeaderStart(chipType);
		int headerLength = ChipType.getHeaderLength(chipType);
		return readFlash(headerStart, headerLength);
	}

	public void triggerGetMacAddress(boolean rebootAfterFlashing) {
		this.rebootAfterFlashing = rebootAfterFlashing;
		logDebug("PacemateDevice: trigger Mac Adress");
		if (operationInProgress()) {
			logError("Already another operation in progress (" + operation + ")");
			return;
		}
		operation = new ReadMacAddressOperation(this);
		operation.setLogIdentifier(logIdentifier);
		operation.start();

	}

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

	@Override
	public Operation getOperation() {
		if (operation == null) {
			return Operation.NONE;
		} else {
			return operation.getOperation();
		}
	}

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

	/**
	 * Jennic Style not of interest for pacemate
	 *
	 * @param address
	 * @param len
	 * @return
	 * @throws Exception
	 */
	@Override
	public byte[] readFlash(int address, int len) throws Exception {
		throw new FlashProgramFailedException();
	}

	/**
	 * @param address
	 * @param len
	 * @return
	 * @throws Exception
	 */
	public byte[] readFlash(long address, int len) throws Exception {

		// Send flash program request
		sendBootLoaderMessage(Messages.flashReadRequestMessage(address, len));

		// Read flash program response
		byte[] response = receiveBootLoaderReplyReadData();

		// Return data
		return response;
	}

	@Override
	public byte[] writeFlash(int address, byte[] data, int offset, int len) throws Exception {

		logError(String.format("Wrong write to flash method use writeToRam instead fro pacemate"));
		throw new FlashProgramFailedException();
	}

	/**
	 * Pacemate style
	 */
	public void writeToRAM(long address, int len) throws Exception {
		// Send flash program request
		// logDebug("Sending program request for address " + address + " with " + data.length + " bytes");
		sendBootLoaderMessage(Messages.writeToRAMRequestMessage(address, len));
		//System.out.println("send ready");
		// Read flash program response
		receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);

		// logDebug("write to RAM ok");
	}

	/**
	 * Pacemate style
	 */
	public void copyRAMToFlash(long flashAddress, long ramAddress, int len) throws Exception {
		// Send flash program request
		// logDebug("Sending program request for address " + address + " with " + data.length + " bytes");
		sendBootLoaderMessage(Messages.copyRAMToFlashRequestMessage(flashAddress, ramAddress, len));

		// Read flash program response
		receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);

		// logDebug("Copy Ram to Flash ok");
	}

	protected void configureFlash() throws Exception {
		// logDebug("Configuring flash");

		enableFlashErase();

		// Send flash configure request
		sendBootLoaderMessage(Messages.flashConfigureRequestMessage(3, 14));

		// Read flash configure response
		receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);

		// logDebug("Done. Flash is configured");
	}

	protected void configureFlash(int start, int end) throws Exception {
		// logDebug("Configuring flash");

		enableFlashErase();

		// Send flash configure request
		sendBootLoaderMessage(Messages.flashConfigureRequestMessage(start, end));

		// Read flash configure response
		receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);

		// logDebug("Done. Flash is configured");
	}

	void enableFlashErase() throws Exception {
		// logDebug("Erase Flash");
		sendBootLoaderMessage(Messages.Unlock_RequestMessage());

		receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);

	}

	@Override
	public void eraseFlash() throws Exception {
		// enableFlashErase();
		logDebug("Erasing flash");
		sendBootLoaderMessage(Messages.flashEraseRequestMessage(3, 14));

		receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);
		try {
			receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);
		} catch (TimeoutException e) {
			logDebug("one line erase response");
		}

	}

	/**
	 * Pacemate Style wie eraseSektor unnütz hier
	 */
	@Override
	public void eraseFlash(Sectors.SectorIndex sector) throws Exception {
		// enableFlashErase();
		logDebug("Erasing sector "/* + sector*/);
		sendBootLoaderMessage(Messages.flashEraseRequestMessage(3, 14));

		receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);
	}

	public void eraseFlash(int start, int end) throws Exception {
		// enableFlashErase();
		logDebug("Erasing sector "/* + sector*/);
		sendBootLoaderMessage(Messages.flashEraseRequestMessage(start, end));

		receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);
		try {
			receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);
		} catch (TimeoutException e) {
			logDebug("one line erase response");
		}
	}

	public void startWriteToRAM(int startAddress, int numberOfBytes) throws Exception {

		logDebug("Start write to RAM "/* + sector*/);
		sendBootLoaderMessage(Messages.writeToRAMRequestMessage(startAddress, numberOfBytes));

		receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);
	}

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
					// TODO Dennis/Carsten: Callback Methode fr den Programming mode
					// ?
				}

				break;
			default:
				logDebug("Serial event (other than data available): " + event);
				break;
		}
	}

	@Override
	public void send(MessagePacket p) throws IOException {
		// logDebug("JD: Sending " + p);

		if (operationInProgress()) {
			logError("Skipping packet. Another operation already in progress (" + operation.getClass().getName() + ")"
			);
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
			outStream.flush();
		} else{
			// Transmit the byte array without dle framing 
			for (int i = 0; i < b.length; ++i) {
				outStream.write(b[i]);
			}
		}
	}

	/**
	 * pacemate style
	 */
	private void sendBootLoaderMessage(byte[] message) throws IOException {
		// Allocate buffer for message + CR and LF
		byte[] data = new byte[message.length + 2];

		// Copy message into the buffer
		System.arraycopy(message, 0, data, 0, message.length);

		// add CR and LF
		data[data.length - 2] = 0x0D; // <CR>
		data[data.length - 1] = 0x0A; // <LF>

		// Print message
		// logDebug("Sending boot loader msg: " + Tools.toHexString(data));

		// Send message
		outStream.write(data);
		outStream.flush();
	}

	// ------------------------------------------------------------------------

	/**
	 * writes the byte array to the out stream pacemate style
	 *
	 * @throws NullPointerException
	 * @throws InvalidChecksumException
	 * @throws UnexpectedResponseException
	 * @throws TimeoutException
	 */
	public void sendDataMessage(byte[] dataMessage) throws IOException, TimeoutException, UnexpectedResponseException,
			InvalidChecksumException, NullPointerException {
		// Allocate buffer for message + CR and LF
		int array_length = dataMessage.length + 2;

		byte[] data = new byte[array_length];

		// Copy message into the buffer
		System.arraycopy(dataMessage, 0, data, 0, dataMessage.length);

		// add CR and LF
		data[dataMessage.length] = 0x0D; // <CR>
		data[dataMessage.length + 1] = 0x0A; // <LF>

		// Print message
		// logDebug("Sending data msg: " + Tools.toASCIIString(data));

		// Send message
		outStream.write(data);
		outStream.flush();

		receiveBootLoaderReplySendDataEcho();
	}

	/**
	 * Writes the CRC to the last two bytes of the Flash pacemate style
	 *
	 * @param crc
	 * @return everything OK
	 * @throws Exception
	 */
	public boolean writeCRCtoFlash(int crc) throws Exception {
		byte crc_bytes[] = new byte[256];
		for (int i = 0; i < 256; i++) {
			crc_bytes[i] = (byte) 0xff;
		}
		crc_bytes[254] = (byte) ((crc & 0xff00) >> 8);
		crc_bytes[255] = (byte) (crc & 0xff);

		logDebug("CRC = " + crc + " " + crc_bytes[254] + " " + crc_bytes[255]);

		try {
			configureFlash(14, 14);
		} catch (Exception e) {
			logDebug("Error while configure flash!");
			return false;
		}

		try {
			eraseFlash(14, 14);
		} catch (Exception e) {
			logDebug("Error while erasing flash!");
			return false;
		}

		try {
			writeToRAM(startAdressInRam, 256);
		} catch (Exception e) {
			logDebug("Error while write to RAM!");
			return false;
		}

		int counter = 0;

		int crc_checksum = 0;

		byte[] line = null;

		// each block is sent in parts of 20 lines a 45 bytes
		while (counter < crc_bytes.length) {
			int offset = 0;
			if (counter + 45 < crc_bytes.length) {
				line = new byte[PacemateBinFile.linesize]; // a line with 45 bytes
				System.arraycopy(crc_bytes, counter, line, 0, PacemateBinFile.linesize);
				counter = counter + PacemateBinFile.linesize;
			} else {
				if (((crc_bytes.length - counter) % 3) == 1) {
					offset = 2;
				} else if (((crc_bytes.length - counter) % 3) == 2) {
					offset = 1;
				}
				line = new byte[crc_bytes.length - counter + offset];
				line[line.length - 1] = 0;
				line[line.length - 2] = 0;
				System.arraycopy(crc_bytes, counter, line, 0, crc_bytes.length - counter);
				counter = counter + (crc_bytes.length - counter);
			}

			for (int i = 0; i < line.length; i++) {
				crc_checksum = PacemateBinFile.calcCRCChecksum(crc_checksum, line[i]);
			}

			logDebug("Sending data msg: " + StringUtils.toHexString(line));

			sendDataMessage(PacemateBinFile.encodeCRCData(line, (line.length - offset)));
		}

		try {
			sendChecksum(crc_checksum);
		} catch (Exception e) {
			logDebug("Error while sending checksum for crc!");
			return false;
		}

		// if block is completed copy data from RAM to Flash
		int crc_block_start = 0x3ff00;

		logDebug("Prepare Flash and Copy Ram to Flash 14 14 " + crc_block_start);

		try {
			configureFlash(14, 14);
			copyRAMToFlash(crc_block_start, startAdressInRam, 256);
		} catch (Exception e) {
			logDebug("Error while copy RAM to Flash!");
			return false;
		}

		return true;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 * writes the byte array to the out stream pacemate style
	 *
	 * @throws NullPointerException
	 * @throws InvalidChecksumException
	 * @throws UnexpectedResponseException
	 * @throws TimeoutException
	 */
	public void sendChecksum(long CRC) throws IOException, TimeoutException, UnexpectedResponseException,
			InvalidChecksumException, NullPointerException {

		// logDebug("Send CRC after 20 Lines or end of Block");
		sendBootLoaderMessage(Messages.writeCRCRequestMessage(CRC));

		receiveBootLoaderReplyReadCRCOK();
	}

	protected void clearStreamData() throws IOException {

		// Allocate message buffer max 255 bytes to read
		byte[] message = new byte[255];

		int index = 0;

		// Read the data
		boolean a = true;
		while ((inStream.available() > 0) && (a == true) && (index < 255)) {
			try {
				//System.out.println("************ Reading from stream");
				message[index] = (byte) inStream.read();
				//System.out.println("************ Done reading from stream");
			} catch (IOException e) {
				logError("" + e, e);
			}
			if (message[index] == -1) {
				a = false;
			} else {
				index++;
			}
		}
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 * Receive the bsl reply message to all request messages with a success answer
	 *
	 * @param type
	 * @return
	 * @throws TimeoutException
	 * @throws UnexpectedResponseException
	 * @throws InvalidChecksumException
	 * @throws IOException
	 * @throws NullPointerException
	 */
	protected String receiveBootLoaderReplySuccess(String type)
			throws TimeoutException, UnexpectedResponseException, InvalidChecksumException, IOException,
			NullPointerException {

		byte[] reply;

		if (this.echo_on == true) {
			reply = readInputStream(3);
		} else {
			reply = readInputStream(2);
		}

		String replyStr = StringUtils.toASCIIString(reply);

		// split the lines from the response message
		String[] parts = replyStr.split("<CR><LF>");

		if (this.isFlashDebugOutput()) {
			for (int j = 0; j < parts.length; j++) {
				logInfo("BL parts " + parts[j]);
			}
		}

		// does the node echo all messages or not
		if (this.echo_on == true) {
			if (parts.length >= 2) {
				if (parts[1].compareTo("0") == 0) // 0 = everything is OK
				{
					if (parts.length >= 3) {
						return parts[2];
					} else {
						return "";
					}
				}
			}
		} else {
			if (parts.length >= 1) {
				if (parts[0].compareTo("0") == 0) // 0 = everything is OK
				{
					if (parts.length >= 1) {
						return parts[1];
					} else {
						return "";
					}
				}
			}
		}

		throw new UnexpectedResponseException("Error in response *" + replyStr + "*", -1, -1);
	}


	/**
	 * Receive the BSL reply message for the autobaud / synchronize request
	 *
	 * @param type
	 * @return
	 * @throws TimeoutException
	 * @throws UnexpectedResponseException
	 * @throws InvalidChecksumException
	 * @throws IOException
	 * @throws NullPointerException
	 */
	protected byte[] receiveBootLoaderReplySynchronized(String type)
			throws TimeoutException, UnexpectedResponseException, InvalidChecksumException, IOException,
			NullPointerException {

		byte[] reply = null;
		if (type.compareTo(Messages.SYNCHRONIZED) == 0) {
			reply = readInputStream(1);
		} else {
			reply = readInputStream(3);
		}
		String replyStr = StringUtils.toASCIIString(reply);

		if ((replyStr.compareTo("Synchronized<CR><LF>") == 0)
				|| (replyStr.compareTo("Synchronized<CR><LF>OK<CR><LF>") == 0)) {
			return reply;
		} else if ((type.compareTo(Messages.SYNCHRONIZED_OK) == 0)) {
			return reply;
		}

		throw new UnexpectedResponseException("Wrong response " + StringUtils.toASCIIString(reply) + " and not " + type,
				-1, -1
		);
	}

	/**
	 * Read the echo for a line of data
	 *
	 * @return
	 * @throws TimeoutException
	 * @throws UnexpectedResponseException
	 * @throws InvalidChecksumException
	 * @throws IOException
	 * @throws NullPointerException
	 */
	protected String receiveBootLoaderReplySendDataEcho()
			throws TimeoutException, UnexpectedResponseException, InvalidChecksumException, IOException,
			NullPointerException {

		byte[] reply;

		reply = readInputStream(1);

		String replyStr = StringUtils.toASCIIString(reply);

		return replyStr;
	}

	/**
	 * Read the requested line from the Flash
	 *
	 * @return
	 * @throws TimeoutException
	 * @throws UnexpectedResponseException
	 * @throws InvalidChecksumException
	 * @throws IOException
	 * @throws NullPointerException
	 */
	protected byte[] receiveBootLoaderReplyReadData()
			throws TimeoutException, UnexpectedResponseException, InvalidChecksumException, IOException,
			NullPointerException {

		byte[] reply = null;
		if (this.echo_on) {
			reply = readInputStream(4);
		} else {
			reply = readInputStream(3);
		}

		int i = 0;
		if (this.echo_on) {
			for (i = 0; i < reply.length; i++) {
				if (reply[i] == 13) { // skip the echo and cr
					i = i + 2; // and lf as well
					break;
				}
			}
		}
		int len = (reply.length - (i + 5));

		byte[] lineFromFlash = new byte[len];

		if ((i + 3 < reply.length) && (reply[i] == 48)) { // copy the line and skip the answer and cr lf
			System.arraycopy(reply, i + 3, lineFromFlash, 0, len);
			//System.out.println(StringUtils.toASCIIString(lineFromFlash));
			return lineFromFlash;
		}

		throw new UnexpectedResponseException("Error in response *" + StringUtils.toASCIIString(reply) + "*", -1, -1);
	}

	/**
	 * Read the response to the CRC message
	 *
	 * @return
	 * @throws TimeoutException
	 * @throws UnexpectedResponseException
	 * @throws InvalidChecksumException
	 * @throws IOException
	 * @throws NullPointerException
	 */
	protected byte[] receiveBootLoaderReplyReadCRCOK()
			throws TimeoutException, UnexpectedResponseException, InvalidChecksumException, IOException,
			NullPointerException {

		byte[] reply = null;
		if (this.echo_on) {
			reply = readInputStream(2);
		} else {
			reply = readInputStream(1);
		}

		String replyStr = StringUtils.toASCIIString(reply);

		// split the lines from the response message
		String[] parts = replyStr.split("<CR><LF>");

		if (this.echo_on) {
			if (parts[1].compareTo(Messages.OK) == 0) {
				return reply;
			} else {
				logDebug("Received boot loader msg: " + replyStr);
				throw new InvalidChecksumException("Invalid checksum - resend " + replyStr);
			}
		} else {
			if (parts[0].compareTo(Messages.OK) == 0) {
				return reply;
			} else {
				logDebug("Received boot loader msg: " + replyStr);
				throw new InvalidChecksumException("Invalid checksum - resend " + replyStr);
			}
		}
	}

	/**
	 * Read from the Input stream from the Pacemate. The length of the expected pacemate reply message is given with the
	 * expected number of  cr lf chars
	 *
	 * @param CRLFcount
	 * @return
	 * @throws TimeoutException
	 * @throws IOException
	 */
	private byte[] readInputStream(int CRLFcount) throws TimeoutException, IOException {
		byte[] message = new byte[255];

		int index = 0;
		int counter = 0;
		int wait = 5;
		waitDataAvailable(inStream, timeoutMillis);

		// Read the message - read CRLFcount lines of response
		while ((index < 255) && (counter < CRLFcount)) {
			if (inStream.available() > 0) {
				message[index] = (byte) inStream.read();
				if (message[index] == 0x0a) {
					counter++;
				}
				if (message[index] != -1) {
					index++;
				}
			} else {
				// message is smaller then expected
				// check if the last line was cr lf 0 cr lf == Success message without more infos
				if (index >= 5) {
					if (checkResponseMessage(message, index)) {
						break;
					}
				}
				waitDataAvailableNoTimeoutException(inStream, 1000);
				wait--;
				if (wait == 0) {
					byte[] fullMessage = new byte[index];
					System.arraycopy(message, 0, fullMessage, 0, index);
					throw new TimeoutException("Not a complete response message from the node *" + StringUtils
							.toASCIIString(fullMessage) + "*"
					);
				}
			}
		}

		// copy to real length
		byte[] fullMessage = new byte[index];
		System.arraycopy(message, 0, fullMessage, 0, index);
		if (this.isFlashDebugOutput()) {
			logInfo("read lines " + StringUtils.toASCIIString(fullMessage));
		}
		return fullMessage;
	}

	/**
	 * Check if the last received bytes were cr lf 0 cr lf == Success message without more infos
	 *
	 * @param message
	 * @param index
	 */
	private boolean checkResponseMessage(byte[] message, int index) {
		//logInfo("Check Response "+message[index-5]+" "+message[index-4]+" "+message[index-3]+" "+message[index-2]+" "+message[index-1]);
		if ((message[index - 5] == 13)		 // cr
				&& (message[index - 4] == 10)  // lf
				&& (message[index - 3] == 48)  // 0
				&& (message[index - 2] == 13)  // cr
				&& (message[index - 1] == 10)) // lf
		{
			return true;
		}
		return false;
	}

	/**
	 * Wait at most timeoutMillis for the input stream to become available
	 *
	 * @param istream	   The stream to monitor
	 * @param timeoutMillis Milliseconds to wait until timeout, 0 for no timeout
	 * @return The number of characters available
	 * @throws TimeoutException
	 * @throws IOException
	 */
	private int waitDataAvailable(InputStream istream, int timeoutMillis) throws TimeoutException, IOException {
		TimeDiff timeDiff = new TimeDiff();
		int avail = 0;

		while (inStream != null && (avail = inStream.available()) == 0) {
			if (timeoutMillis > 0 && timeDiff.ms() >= timeoutMillis) {
				logWarn("Timeout waiting for data (waited: " + timeDiff.ms() + ", timeoutMs:" + timeoutMillis + ")");
				throw new TimeoutException();
			}

			synchronized (dataAvailableMonitor) {
				try {
					dataAvailableMonitor.wait(50);
				} catch (InterruptedException e) {
					logError("" + e, e);
				}
			}
		}
		return avail;
	}

	private int waitDataAvailableNoTimeoutException(InputStream istream, int timeoutMillis) throws IOException {
		TimeDiff timeDiff = new TimeDiff();
		int avail = 0;

		while (inStream != null && (avail = inStream.available()) == 0) {
			if (timeoutMillis > 0 && timeDiff.ms() >= timeoutMillis) {
				return 0;
			}

			synchronized (dataAvailableMonitor) {
				try {
					dataAvailableMonitor.wait(50);
				} catch (InterruptedException e) {
					logError(e.toString());
				}
			}
		}
		return avail;
	}

	protected boolean waitForConnection() {
		try {
			// Send flash read request (in fact, this could be any valid message
			// to which the
			// device is supposed to respond)
			sendBootLoaderMessage(Messages.ReadPartIDRequestMessage());
			receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);
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

	protected boolean autobaud() {
		try {
			sendBootLoaderMessage(Messages.AutoBaudRequestMessage());
			receiveBootLoaderReplySynchronized(Messages.SYNCHRONIZED);
			sendBootLoaderMessage(Messages.AutoBaudRequest2Message());
			receiveBootLoaderReplySynchronized(Messages.SYNCHRONIZED);
			sendBootLoaderMessage(Messages.AutoBaudRequest3Message());
			receiveBootLoaderReplySynchronized(Messages.SYNCHRONIZED_OK);
			logInfo("Autobaud");
		} catch (TimeoutException to) {
			logDebug("Still waiting for a connection.");
		} catch (Exception error) {
			logWarn("Exception while waiting for connection", error);
		}
		return true;
	}

	protected boolean echoOff() {
		try {
			sendBootLoaderMessage(Messages.SetEchoOffMessage());
			receiveBootLoaderReplySuccess(Messages.CMD_SUCCESS);
			logInfo("Echo off");
			echo_on = false;
		} catch (TimeoutException to) {
			logDebug("Still waiting for a connection.");
		} catch (Exception error) {
			logWarn("Exception while waiting for connection", error);
		}
		return true;
	}

	protected void flushReceiveBuffer() {
		long i = 0;
		// logDebug("Flushing serial rx buffer");

		try {
			while ((i = inStream.available()) > 0) {
				logDebug("Skipping " + i + " characters while flushing on the serial rx");
				inStream.skip(i);
			}
		} catch (IOException e) {
			logWarn("Error while serial rx flushing buffer: " + e, e);
		}
	}

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
		return serialPortName;
	}

	public SerialPort getSerialPort() {
		return serialPort;
	}

	@Override
	public int[] getChannels() {
		int[] channels = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};
		return channels;
	}

	@Override
	public IDeviceBinFile loadBinFile(String fileName) throws Exception {
		return new PacemateBinFile(fileName);
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

}
