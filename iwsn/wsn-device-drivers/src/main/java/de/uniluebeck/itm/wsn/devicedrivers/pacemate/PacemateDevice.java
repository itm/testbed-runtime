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
	private static final Logger log = LoggerFactory.getLogger(PacemateDevice.class);

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

	;

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
				if (serialPort == null)
					log.debug("connect(): serialPort==null");
			} catch (PortInUseException piue) {
				log.debug("Port already in use. Connection will be removed. ");
				if (serialPort != null)
					serialPort.close();
				// this.owner.removeConnection(this);
				return false;
			} catch (Exception e) {
				if (serialPort != null)
					serialPort.close();
				log.debug("Port does not exist. Connection will be removed. " + e, e);
				return false;
			}
			return true;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public void setSerialPort(String port) throws Exception {
		Enumeration e = CommPortIdentifier.getPortIdentifiers();
		SerialPort sp = null;
		for (; e.hasMoreElements();) {
			CommPortIdentifier cpi = (CommPortIdentifier) e.nextElement();
			if (cpi.getName().equals(port)) {
				CommPort commPort = null;
				for (int i = 0; i < MAX_RETRIES; i++) {
					try {
						commPort = cpi.open(this.getClass().getName(), 1000);
						break;
					} catch (PortInUseException piue) {
						log.debug("Port in Use Retrying to connect");
						if (i >= MAX_RETRIES - 1)
							throw (piue);
						Thread.sleep(200);
					}
				}
				if (commPort instanceof SerialPort)
					sp = (SerialPort) commPort;// cpi.open("iShell", 1000);
				else
					log.debug("Port is no SerialPort");
				break;
			}
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

		log.debug("Resetting device Pacemate style");
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
		log.error("Entered programming mode the Pacemate style");

		// log.debug("Com port in programming mode");
		return true;
	}

	private void setComPort(ComPortMode mode) throws UnsupportedCommOperationException {
		// SettingsKey baudRateKey = (mode == ComPortMode.Program) ? SettingsKey
		// .flash_baudrate : SettingsKey.read_baudrate;
		int baudrate = (mode == ComPortMode.Program) ? flashBaudrate : readBaudrate;

		log.debug("set com port " + baudrate + " " + databits + " " + stopbits + " " + parityBit);

		serialPort.setSerialPortParams(baudrate, databits, stopbits, parityBit);

		serialPort.setDTR(false);
		serialPort.setRTS(false);
		// log.debug("Setting COM-Port parameters (new style): baudrate: " + baudrate);
	}

	public void leaveProgrammingMode() throws Exception {
		// log.debug("Leaving programming mode.");

		// Restore normal settings
		flushReceiveBuffer();
		setComPort(ComPortMode.Normal);

		// Reboot (if requested by the user)
		if (rebootAfterFlashing) {
			log.debug("Rebooting device");
			reset();
		}

		// log.debug("Done. Left programming mode.");
	}

	public ChipType getChipType() throws Exception {
		// Send chip type read request
		sendBootLoaderMessage(Messages.ReadPartIDRequestMessage());

		// Read chip type read response
		byte[] response = receiveBootLoaderReply(Messages.CMD_SUCCESS);

		ChipType chipType = ChipType.Unknown;
		int chipid = 0;

		if (response.length > 6) {
			int i = 6;
			while ((i < response.length) && (response[i] != 0xd)) {
				chipid = chipid * 10;
				chipid = chipid + (response[i] - 0x30);
				i++;
			}
		}

		if (chipid == 196387)
			chipType = ChipType.LPC2136;
		else {
			log.error("Defaulted to chip type LPC2136 (Pacemate). Identification may be wrong." + chipid);
			chipType = ChipType.LPC2136;
		}

		log.debug("Chip identified as " + chipType + " (received " + chipid + ")");
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
		log.debug("PacemateDevice: trigger Mac Adress");
		if (operationInProgress()) {
			log.error("Already another operation in progress (" + operation + ")");
			return;
		}
		operation = new ReadMacAddressOperation(this);
		operation.start();

	}

	@Override
	public boolean triggerProgram(IDeviceBinFile program, boolean rebootAfterFlashing) {
		this.rebootAfterFlashing = rebootAfterFlashing;
		if (operationInProgress()) {
			log.error("Already another operation in progress (" + operation + ")");
			return false;
		}

		operation = new FlashProgramOperation(this, program, true);
		operation.start();
		return true;
	}

	@Override
	public boolean triggerReboot() throws Exception {
		log.debug("Starting reboot device thread");
		if (operationInProgress()) {
			log.error("Already another operation in progress (" + operation + ")");
			return false;
		}

		operation = new RebootDeviceOperation(this);
		operation.start();
		return true;
	}

	@Override
	public void triggerSetMacAddress(MacAddress mac, boolean rebootAfterFlashing) {
		this.rebootAfterFlashing = rebootAfterFlashing;
		if (operationInProgress()) {
			log.error("Already another operation in progress (" + operation + ")");
			return;
		}
		operation = new WriteMacAddressOperation(this, mac);
		operation.start();
	}

	@Override
	public Operation getOperation() {
		if (operation == null)
			return Operation.NONE;
		else
			return operation.getOperation();
	}

	@Override
	public void shutdown() {
		// if (connected) {

		try {
			if (inStream != null)
				inStream.close();
		} catch (IOException e) {
			log.debug("Failed to close in-stream :" + e, e);
		}
		try {
			if (outStream != null)
				outStream.close();
		} catch (IOException e) {
			log.debug("Failed to close out-stream :" + e, e);
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
		byte[] response = receiveBootLoaderReply(Messages.DATA);

		// Return data
		return response;
	}

	@Override
	public byte[] writeFlash(int address, byte[] data, int offset, int len) throws Exception {

		log.error(String.format("Wrong write to flash method use writeToRam instead fro pacemate"));
		throw new FlashProgramFailedException();
	}

	/**
	 * Pacemate style
	 */
	public byte[] writeToRAM(long address, int len) throws Exception {
		// Send flash program request
		// log.debug("Sending program request for address " + address + " with " + data.length + " bytes");
		sendBootLoaderMessage(Messages.writeToRAMRequestMessage(address, len));

		// Read flash program response
		byte[] response = receiveBootLoaderReply(Messages.CMD_SUCCESS);

		// log.debug("write to RAM ok");

		return response;
	}

	/**
	 * Pacemate style
	 */
	public byte[] copyRAMToFlash(long flashAddress, long ramAddress, int len) throws Exception {
		// Send flash program request
		// log.debug("Sending program request for address " + address + " with " + data.length + " bytes");
		sendBootLoaderMessage(Messages.copyRAMToFlashRequestMessage(flashAddress, ramAddress, len));

		// Read flash program response
		byte[] response = receiveBootLoaderReply(Messages.CMD_SUCCESS);

		// log.debug("Copy Ram to Flash ok");

		return response;
	}

	protected void configureFlash() throws Exception {
		// log.debug("Configuring flash");

		enableFlashErase();

		// Send flash configure request
		sendBootLoaderMessage(Messages.flashConfigureRequestMessage(3, 14));

		// Read flash configure response
		receiveBootLoaderReply(Messages.CMD_SUCCESS);

		// log.debug("Done. Flash is configured");
	}

	protected void configureFlash(int start, int end) throws Exception {
		// log.debug("Configuring flash");

		enableFlashErase();

		// Send flash configure request
		sendBootLoaderMessage(Messages.flashConfigureRequestMessage(start, end));

		// Read flash configure response
		receiveBootLoaderReply(Messages.CMD_SUCCESS);

		// log.debug("Done. Flash is configured");
	}

	void enableFlashErase() throws Exception {
		// log.debug("Erase Flash");
		sendBootLoaderMessage(Messages.Unlock_RequestMessage());

		receiveBootLoaderReply(Messages.CMD_SUCCESS);

	}

	@Override
	public void eraseFlash() throws Exception {
		// enableFlashErase();
		log.debug("Erasing flash");
		sendBootLoaderMessage(Messages.flashEraseRequestMessage(3, 14));

		receiveBootLoaderReply(Messages.CMD_SUCCESS);
		try {
			receiveBootLoaderReply(Messages.CMD_SUCCESS);
		} catch (TimeoutException e) {
			log.debug("one line erase response");
		}

	}

	/**
	 * Pacemate Style wie eraseSektor unnütz hier
	 */
	@Override
	public void eraseFlash(Sectors.SectorIndex sector) throws Exception {
		// enableFlashErase();
		log.debug("Erasing sector "/* + sector*/);
		sendBootLoaderMessage(Messages.flashEraseRequestMessage(3, 14));

		receiveBootLoaderReply(Messages.CMD_SUCCESS);
	}

	public void eraseFlash(int start, int end) throws Exception {
		// enableFlashErase();
		log.debug("Erasing sector "/* + sector*/);
		sendBootLoaderMessage(Messages.flashEraseRequestMessage(start, end));

		receiveBootLoaderReply(Messages.CMD_SUCCESS);
		try {
			receiveBootLoaderReply(Messages.CMD_SUCCESS);
		} catch (TimeoutException e) {
			log.debug("one line erase response");
		}
	}

	public void startWriteToRAM(int startAddress, int numberOfBytes) throws Exception {

		log.debug("Start write to RAM "/* + sector*/);
		sendBootLoaderMessage(Messages.writeToRAMRequestMessage(startAddress, numberOfBytes));

		receiveBootLoaderReply(Messages.CMD_SUCCESS);
	}

	public void serialEvent(SerialPortEvent event) {
		// log.debug("Serial event");
		switch (event.getEventType()) {
			case SerialPortEvent.DATA_AVAILABLE:

				synchronized (dataAvailableMonitor) {
					// log.debug("DM");
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
				log.debug("Serial event (other than data available): " + event);
				break;
		}
	}

	@Override
	public void send(MessagePacket p) throws IOException {
		// log.debug("JD: Sending " + p);

		if (operationInProgress()) {
			log.error("Skipping packet. Another operation already in progress (" + operation.getClass().getName() + ")");
			return;
		}

		byte type = (byte) (0xFF & p.getType());
		byte b[] = p.getContent();

		if (b == null || type > 0xFF) {
			log.warn("Skipping empty packet or type > 0xFF.");
			return;
		}
		if (b.length > 150) {
			log.warn("Skipping too large packet (length " + b.length + ")");
			return;
		}

		// Send start signal DLE STX
		this.outStream.write(DLE_STX);

		// Send the type escaped
		outStream.write(type);
		if (type == DLE)
			outStream.write(DLE);

		// Transmit each byte escaped
		for (int i = 0; i < b.length; ++i) {
			outStream.write(b[i]);
			if (b[i] == DLE)
				outStream.write(DLE);
		}
		// Compute CRC over packet (including the type)
		/*
		byte btemp[] = new byte[b.length+1];
		btemp[0] = type;
		for (int i = 0; i < b.length; i++)
			btemp[i+1] = b[i];
		
		byte crc[] = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA};
		for (int i=0; i < btemp.length; i++)
			crc[i%4] = (byte)(0xFF & (crc[i%4]^btemp[i]));
		//System.out.println("crc: " + " " + (int)(0xFF & crc[0])+ " " + (int)(0xFF & crc[1])+ " " + (int)(0xFF & crc[2])+ " " + (int)(0xFF & crc[3]));
		for (int i = 0; i<4; i++)
		{
			outStream.write(crc[i]);
			if (crc[i] == DLE)
				outStream.write(DLE);
		}*/
		// End: Compute CRC over packet

		// Send final DLT ETX
		outStream.write(DLE_ETX);
		outStream.flush();
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
		// log.debug("Sending boot loader msg: " + Tools.toHexString(data));

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
		// log.debug("Sending data msg: " + Tools.toASCIIString(data));

		// Send message
		outStream.write(data);
		outStream.flush();

		receiveBootLoaderReply(Messages.DATA_ECHO);
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
		for (int i = 0; i < 256; i++)
			crc_bytes[i] = (byte) 0xff;
		crc_bytes[254] = (byte) ((crc & 0xff00) >> 8);
		crc_bytes[255] = (byte) (crc & 0xff);

		log.debug("CRC = " + crc + " " + crc_bytes[254] + " " + crc_bytes[255]);

		try {
			configureFlash(14, 14);
		} catch (Exception e) {
			log.debug("Error while configure flash!");
			return false;
		}

		try {
			eraseFlash(14, 14);
		} catch (Exception e) {
			log.debug("Error while erasing flash!");
			return false;
		}

		try {
			writeToRAM(startAdressInRam, 256);
		} catch (Exception e) {
			log.debug("Error while write to RAM!");
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
				if (((crc_bytes.length - counter) % 3) == 1)
					offset = 2;
				else if (((crc_bytes.length - counter) % 3) == 2)
					offset = 1;
				line = new byte[crc_bytes.length - counter + offset];
				line[line.length - 1] = 0;
				line[line.length - 2] = 0;
				System.arraycopy(crc_bytes, counter, line, 0, crc_bytes.length - counter);
				counter = counter + (crc_bytes.length - counter);
			}

			for (int i = 0; i < line.length; i++)
				crc_checksum = PacemateBinFile.calcCRCChecksum(crc_checksum, line[i]);

			if (log.isDebugEnabled()) {
				log.debug("Sending data msg: " + StringUtils.toHexString(line));
			}

			sendDataMessage(PacemateBinFile.encodeCRCData(line, (line.length - offset)));
		}

		try {
			sendChecksum(crc_checksum);
		} catch (Exception e) {
			log.debug("Error while sending checksum for crc!");
			return false;
		}

		// if block is completed copy data from RAM to Flash
		int crc_block_start = 0x3ff00;

		log.debug("Prepare Flash and Copy Ram to Flash 14 14 " + crc_block_start);

		try {
			configureFlash(14, 14);
			copyRAMToFlash(crc_block_start, startAdressInRam, 256);
		} catch (Exception e) {
			log.debug("Error while copy RAM to Flash!");
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

		// log.debug("Send CRC after 20 Lines or end of Block");
		sendBootLoaderMessage(Messages.writeCRCRequestMessage(CRC));

		receiveBootLoaderReply(Messages.OK);
	}

	protected void clearStreamData() {

		// Allocate message buffer max 255 bytes to read
		byte[] message = new byte[255];

		int index = 0;

		// Read the data
		boolean a = true;
		while ((a == true) && (index < 255)) {
			try {
				message[index] = (byte) inStream.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (message[index] == -1)
				a = false;
			else
				index++;
		}
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	protected byte[] receiveBootLoaderReply(String type) throws TimeoutException, UnexpectedResponseException,
			InvalidChecksumException, IOException, NullPointerException {

		// Allocate message buffer max 255 bytes to read
		byte[] message = new byte[255];

		int index = 0;

		String returnCode = "";

		int response_data_start = 0;

		waitDataAvailable(inStream, timeoutMillis);

		int read_returnCode = 1;

		if (echo_on == false)
			read_returnCode = 2;

		// Read the message
		while (index < 255) {
			message[index] = (byte) inStream.read();
			if (message[index] == -1)
				break;
			if (read_returnCode == 2) {
				if (message[index] == 0x0d) {
					read_returnCode = 3;
					response_data_start = index + 2;
				} else
					returnCode = returnCode + (char) message[index];
			}
			if ((message[index] == 0x0a) && (read_returnCode == 1))
				read_returnCode = 2;
			index++;
		}

		// copy to real length
		byte[] fullMessage = new byte[index];
		System.arraycopy(message, 0, fullMessage, 0, index);

		//if (this.isFlashDebugOutput()){ 
			if (log.isDebugEnabled()) {
					log.debug("Received boot loader msg: " + StringUtils.toHexString(fullMessage));
			}
		//}

		if (returnCode.length() == 0)
			return message;

		if (type == Messages.DATA_ECHO)
			return message;

		int returnCodeInt = -1;

		try {
			returnCodeInt = new Integer(returnCode).intValue();
		} catch (Exception e) {
			returnCodeInt = -1;
		}

		// log.debug("Received boot loader msg: " + returnCode +" "+ Messages.getErrorMessage(returnCodeInt));

		if ((type.compareTo(Messages.DATA) == 0) && (returnCode.compareTo(Messages.CMD_SUCCESS) == 0)) {
			byte[] dataMessage = new byte[index - response_data_start];
			System.arraycopy(message, response_data_start, dataMessage, 0, index - response_data_start);
			return dataMessage;
		}

		// Check if the response type is unexpected
		if (type.compareTo(Messages.CMD_SUCCESS) == 0) {
			if (returnCode.compareTo(Messages.CMD_SUCCESS) == 0)
				return message;
			else {
				log.debug("Received boot loader msg: " + returnCode);
				throw new UnexpectedResponseException(new Integer(type).intValue(), new Integer(returnCode).intValue());
			}
		} else if ((type.compareTo(Messages.SYNCHRONIZED) == 0) && (message[0] == 0x53))
			return message;

		else if ((type.compareTo(Messages.SYNCHRONIZED_OK) == 0))
			return message;

		else if (type.compareTo(Messages.OK) == 0) {
			if (returnCode.compareTo(Messages.OK) == 0)
				return message;
			else {
				log.debug("Received boot loader msg: " + returnCode);
				throw new InvalidChecksumException("Invalid checksum - resend");
			}
		} else if (type.compareTo(Messages.ECHO_RESPONSE) == 0)
			return message;

		return message;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 * Wait at most timeoutMillis for the input stream to become available
	 *
	 * @param istream	   The stream to monitor
	 * @param timeoutMillis Milliseconds to wait until timeout, 0 for no timeout
	 * @param minCharacters The minimal amount of available characters
	 * @return The number of characters available
	 * @throws IOException
	 */
	private int waitDataAvailable(InputStream istream, int timeoutMillis) throws TimeoutException, IOException {
		TimeDiff timeDiff = new TimeDiff();
		int avail = 0;

		while (inStream != null && (avail = inStream.available()) == 0) {
			if (timeoutMillis > 0 && timeDiff.ms() >= timeoutMillis) {
				log.warn("Timeout waiting for data (waited: " + timeDiff.ms() + ", timeoutMs:" + timeoutMillis + ")");
				throw new TimeoutException();
			}

			synchronized (dataAvailableMonitor) {
				try {
					dataAvailableMonitor.wait(50);
				} catch (InterruptedException e) {
					log.error("" + e, e);
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
			receiveBootLoaderReply(Messages.CMD_SUCCESS);
			log.info("Device connection established");
			return true;
		} catch (TimeoutException to) {
			log.debug("Still waiting for a connection.");
		} catch (Exception error) {
			log.warn("Exception while waiting for connection", error);
		}

		flushReceiveBuffer();
		return false;
	}

	protected boolean autobaud() {
		try {
			sendBootLoaderMessage(Messages.AutoBaudRequestMessage());
			receiveBootLoaderReply(Messages.SYNCHRONIZED);
			sendBootLoaderMessage(Messages.AutoBaudRequest2Message());
			receiveBootLoaderReply(Messages.SYNCHRONIZED);
			sendBootLoaderMessage(Messages.AutoBaudRequest3Message());
			receiveBootLoaderReply(Messages.OK);
			log.info("Autobaud");
		} catch (TimeoutException to) {
			log.debug("Still waiting for a connection.");
		} catch (Exception error) {
			log.warn("Exception while waiting for connection", error);
		}
		return true;
	}

	protected boolean echoOff() {
		try {
			sendBootLoaderMessage(Messages.SetEchoOffMessage());
			receiveBootLoaderReply(Messages.ECHO_RESPONSE);
			log.info("Echo off");
			echo_on = false;
		} catch (TimeoutException to) {
			log.debug("Still waiting for a connection.");
		} catch (Exception error) {
			log.warn("Exception while waiting for connection", error);
		}
		return true;
	}

	protected void flushReceiveBuffer() {
		long i = 0;
		// log.debug("Flushing serial rx buffer");

		try {
			while ((i = inStream.available()) > 0) {
				log.debug("Skipping " + i + " characters while flushing on the serial rx");
				inStream.skip(i);
			}
		} catch (IOException e) {
			log.warn("Error while serial rx flushing buffer: " + e, e);
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

	public boolean isConnected() {
		return connected;
	}

}
