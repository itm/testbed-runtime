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

import de.uniluebeck.itm.wsn.devicedrivers.exceptions.*;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.FlashType;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.Sectors.SectorIndex;
import gnu.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;

/**
 * @author Friedemann Wesner A device class representing a TelosB device.
 */
public class TelosbDevice extends iSenseDeviceImpl implements
		SerialPortEventListener {

	private static final Logger log = LoggerFactory
			.getLogger(TelosbDevice.class);

	/** */
	private enum ComPortMode {

		Normal, Program
	}

	;

	// iSenseTelos true = Telos node with iSense Os and uart messages with type
	// false = no packet type everything as plain text message
	// private boolean iSenseTelos = true;

	private final int BSL_DATABITS = SerialPort.DATABITS_8;

	private final int BSL_STOPBITS = SerialPort.STOPBITS_1;

	private final int BSL_PARITY_EVEN = SerialPort.PARITY_EVEN;

	private final int BSL_PARITY_NONE = SerialPort.PARITY_NONE;

	private final int MAX_OPEN_PORT_RETRIES = 10;

	/*
	 * initial baud rate for communicating over the serial port, can can only be
	 * changed temporarily later on via bsl command
	 */

	private final int READ_BAUDRATE = 115200;

	private final int FLASH_BAUDRATE = 9600;

	/* time out for opening a serial port */

	private final int PORTOPEN_TIMEOUTMS = 1000;

	/**
	 * Strings for saving device state in a memento
	 */
	public static final String MEM_PORT = "Port";

	private String serialPortName = "";

	private SerialPort serialPort = null;

	private InputStream inputStream = null;

	private OutputStream outputStream = null;

	private BSLTelosb bsl = null;

	private boolean connected;

	//private boolean verify = true;

	/**
	 * @param serialPortName
	 */
	public TelosbDevice(String serialPortName) {
		this.serialPortName = serialPortName;
		connected = false;
		connect();
	}

	private boolean connect() {
		Enumeration allIdentifiers = null;
		CommPortIdentifier portIdentifier = null;
		CommPort commPort = null;
		int tries = 0;
		boolean portOpened = false;
		boolean portFound = false;

		if (serialPort != null) {
			connected = false;
			return true;
		}

		if (serialPortName == null) {
			connected = false;
			return false;
		}

		allIdentifiers = CommPortIdentifier.getPortIdentifiers();
		while (allIdentifiers.hasMoreElements() && !portFound) {
			portIdentifier = (CommPortIdentifier) allIdentifiers.nextElement();
			if (portIdentifier.getName().equals(serialPortName)) {
				portFound = true;
			}
		}

		if (!portFound) {
			logDebug("Failed to connect to port '" + serialPortName
					+ "': port does not exist."
			);
			connected = false;
			return false;
		}

		// open port
		while (tries < MAX_OPEN_PORT_RETRIES && !portOpened) {
			try {
				tries++;
				commPort = portIdentifier.open(this.getClass().getName(),
						PORTOPEN_TIMEOUTMS
				);
				portOpened = true;
			} catch (PortInUseException e) {
				if (tries < MAX_OPEN_PORT_RETRIES) {
					logDebug("Port '" + serialPortName
							+ "' is already in use, retrying to connect..."
					);
					portOpened = false;
				} else {
					logDebug("Port '" + serialPortName
							+ "' is already in use, failed to connect."
					);
					connected = false;
					return false;
				}
			}
		}

		// cancel if opened port is no serial port
		if (!(commPort instanceof SerialPort)) {
			logDebug("Com Port '" + serialPortName
					+ "' is no serial port, will not connect."
			);
			connected = false;
			return false;
		}

		serialPort = (SerialPort) commPort;
		try {
			serialPort.setSerialPortParams(READ_BAUDRATE, BSL_DATABITS,
					BSL_STOPBITS, BSL_PARITY_NONE
			);
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		} catch (UnsupportedCommOperationException e) {
			logError("Failed to connect to port '" + serialPortName + "'. "
					+ e.getMessage(), e
			);
			connected = false;
			return false;
		}

		serialPort.setRTS(true);
		serialPort.setDTR(true);

		try {
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();

			bsl = new BSLTelosb(serialPort);
		} catch (IOException e) {
			logError("Unable to get I/O streams of port " + serialPortName
					+ ", failed to connect.", e
			);
			connected = false;
			return false;
		}

		try {
			serialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			logError("Unable to register as event listener for serial port "
					+ serialPortName, e
			);
			connected = false;
			return false;
		}
		serialPort.notifyOnDataAvailable(true);

		logDebug("Device connected to serial port " + serialPort.getName());

		connected = true;
		return true;
	}

	@Override
	public boolean enterProgrammingMode() throws TimeoutException,
			InvalidChecksumException, ReceivedIncorrectDataException,
			IOException, FlashEraseFailedException,
			UnexpectedResponseException, Exception {
		if (log.isDebugEnabled()) {
			logDebug("enterProgrammingMode()");
		}
		this.setComPort(ComPortMode.Program);

		return startBSL();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDeviceImpl#eraseFlash()
	 */

	@Override
	public void eraseFlash() throws FlashEraseFailedException, IOException {
		byte[] reply = null;

		if (bsl == null) {
			throw new NullPointerException(
					"No connection to device (bsl is null)."
			);
		}

		if (log.isDebugEnabled()) {
			logDebug("eraseFlash() (mass erase)");
		}

		// invoke boot loader
		if (!bsl.invokeBSL()) {
			throw new FlashEraseFailedException(
					"Mass erase failed: failed to invoke boot loader"
			);
		}

		try {
			// send bsl command 'mass erase'
			bsl.sendBSLCommand(BSLTelosb.CMD_MASSERASE, 0xFFFF, 0xA506, null,
					false
			);

			// receive bsl reply
			reply = bsl.receiveBSLReply();
		} catch (Exception e) {
			throw new FlashEraseFailedException("Mass erase failed: " + e);
		}

		if ((reply[0] & 0xff) == BSLTelosb.DATA_NACK) {
			throw new FlashEraseFailedException(
					"Mass erase failed: received NACK"
			);
		} else if (reply.length > 1) {
			throw new FlashEraseFailedException(
					"Mass erase failed: received unexpected response of length "
							+ reply.length
			);
		}

		// transmit default password to unlock protected commands after mass
		// erase
		try {
			if (!bsl.transmitPassword(null, false)) {
				log
						.warn("Received no ACK for password transmission after mass erase. Protected commands are still locked.");
			}
		} catch (Exception e) {
			throw new FlashEraseFailedException(
					"Error transmitting default password after mass erase: "
							+ e
			);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ishell.device.iSenseDeviceImpl#eraseFlash(ishell.device.jennic.Sectors
	 * .SectorIndex)
	 */

	@Override
	public void eraseFlash(SectorIndex sector) throws Exception {
		// TODO Auto-generated method stub
		throw new NotImplementedException(
				"Method eraseFlash() not implemented by TelosBDevice"
		);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDeviceImpl#getChipType()
	 */

	@Override
	public ChipType getChipType() throws Exception {
		return ChipType.TelosB;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDeviceImpl#getFlashType()
	 */

	@Override
	public FlashType getFlashType() throws Exception {
		return FlashType.Unknown;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDeviceImpl#getOperation()
	 */

	@Override
	public Operation getOperation() {
		if (operation == null) {
			return Operation.NONE;
		} else {
			return operation.getOperation();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDeviceImpl#leaveProgrammingMode()
	 */

	@Override
	public void leaveProgrammingMode() throws Exception {
		if (log.isDebugEnabled()) {
			logDebug("leaveProgrammingMode()");
		}
		flushReceiveBuffer();
		this.setComPort(ComPortMode.Normal);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDeviceImpl#readFlash(int, int)
	 */

	@Override
	public byte[] readFlash(int address, int len)
			throws FlashReadFailedException, IOException {
		byte[] reply = null;

		if (bsl == null) {
			throw new NullPointerException(
					"No connection to device (bsl is null)."
			);
		}

		try {
			// execute bsl patch
			bsl.executeBSLPatch();

			// receive data block
			bsl.sendBSLCommand(BSLTelosb.CMD_RXDATABLOCK, address, len, null,
					false
			);

			// receive reply
			reply = bsl.receiveBSLReply();
		} catch (Exception e) {
			throw new FlashReadFailedException(String.format(
					"Failed to read flash " + "at address 0x%02x, %d bytes: "
							+ e + ".", address, len
			)
			);
		}

		if ((0xFF & reply[0]) != BSLTelosb.DATA_ACK) {
			throw new FlashReadFailedException(String.format(
					"Failed to read flash "
							+ "at address 0x%02x, %d bytes (missing BSL ACK).",
					address, len
			)
			);
		}

		return reply;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDeviceImpl#reset()
	 */

	@Override
	public boolean reset() {
		if (bsl == null) {
			throw new NullPointerException(
					"No connection to device (bsl is null)."
			);
		}

		return bsl.reset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDeviceImpl#send(ishell.device.MessagePacket)
	 */

	@Override
	public void send(MessagePacket p) throws Exception {
		// TODO Auto-generated method stub

		if (operationInProgress()) {
			log
					.error("Skipping packet. Another operation already in progress ("
							+ operation.getClass().getName() + ")"
					);
			return;
		}

		byte type = (byte) (0xFF & p.getType());
		byte b[] = p.getContent();
		
		boolean iSenseStyle = true;
		
		// if the type was set to 0 send the message without iSense framing to the node
		// e.g. to Contiki or TinyOs Os
		//if (type == 0x64)
		//	iSenseStyle = false;

		if (b == null || type > 0xFF) {
			log.warn("Skipping empty packet or type > 0xFF.");
			return;
		}
		if (b.length > 150) {
			log.warn("Skipping too large packet (length " + b.length + ")");
			return;
		}

		if (iSenseStyle == true){
			// Send start signal DLE STX
			this.outputStream.write(DLE_STX);
	
			// Send the type escaped
			outputStream.write(type);
			if (type == DLE) {
				outputStream.write(DLE);
			}
	
			// Transmit each byte escaped
			for (int i = 0; i < b.length; ++i) {
				outputStream.write(b[i]);
				if (b[i] == DLE) {
					outputStream.write(DLE);
				}
			}
	
			// Send final DLT ETX
			outputStream.write(DLE_ETX);
			outputStream.flush();
		} else {
			// Transmit the byte array without dle framing 
			for (int i = 0; i < b.length; ++i) {
				outputStream.write(b[i]);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDeviceImpl#shutdown()
	 */

	@Override
	public void shutdown() {
		if (log.isDebugEnabled()) {
			logDebug("Shutting down device");
		}

		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				logDebug("Unable to close input stream: " + e);
			}
		}
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				logDebug("Unable to close output stream: " + e);
			}
		}

		if (serialPort != null) {
			serialPort.setRTS(true);
			serialPort.setDTR(false);

			serialPort.removeEventListener();
			serialPort.close();
			serialPort = null;
			connected = false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDeviceImpl#writeFlash(int, byte[], int, int)
	 */

	@Override
	public byte[] writeFlash(int address, byte[] bytes, int offset, int len)
			throws IOException, FlashProgramFailedException {
		byte[] reply = null;

		if (bsl == null) {
			throw new NullPointerException(
					"No connection to device (bsl is null)."
			);
		}

		try {
			// verify if block range is erased
			if (this.isFlashDebugOutput()) {
				if (!bsl.verifyBlock(address, len, null)) {
					throw new FlashProgramFailedException(
							"Failed to program flash: block range is not erased completely"
					);
				}
			}

			//if (this.isFlashDebugOutput()) {
			if (log.isDebugEnabled()) {
				logDebug(String.format("***Programming data block at address: 0x%02x, writing %d bytes.",
						address, bytes.length
				)
				);
			}
			//}

			// execute bsl patch first(only for BSL version <=1.10)
			if (this.isFlashDebugOutput()) {
				bsl.executeBSLPatch();
			}
			// program block
			bsl.sendBSLCommand(BSLTelosb.CMD_TXDATABLOCK, address, len, bytes,
					false
			);
			reply = bsl.receiveBSLReply();
			if ((reply[0] & 0xFF) != BSLTelosb.DATA_ACK) {
				throw new FlashProgramFailedException(
						"Failed to program flash: received no ACK"
				);
			}
			// verify programmed block
			if (this.isFlashDebugOutput()) {
				if (!bsl.verifyBlock(address, len, bytes)) {
					throw new FlashProgramFailedException(
							"Failed to program flash: verification of written data failed"
					);
				}
			}
		} catch (Exception e) {
			throw new FlashProgramFailedException("Failed to program flash: "
					+ e
			);
		}

		return reply;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDevice#getChannels()
	 */

	@Override
	public int[] getChannels() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDevice#triggerGetMacAddress(boolean)
	 */

	@Override
	public void triggerGetMacAddress(boolean rebootAfterFlashing)
			throws Exception {
		if (log.isDebugEnabled()) {
			log
					.debug("Device getMAC Address triggered but not yet implemented.");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ishell.device.iSenseDevice#triggerProgram(ishell.device.IDeviceBinFile,
	 * boolean)
	 */

	@Override
	public boolean triggerProgram(IDeviceBinFile program,
								  boolean rebootAfterFlashing) throws Exception {
		if (operationInProgress()) {
			logError("Already another operation in progress (" + operation
					+ ")"
			);
			return false;
		}

		operation = new TelosbFlashProgramOperation(this, program);
		operation.setLogIdentifier(logIdentifier);
		operation.start();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDevice#triggerReboot()
	 */

	@Override
	public boolean triggerReboot() throws Exception {
		if (log.isDebugEnabled()) {
			logDebug("Device reboot triggered.");
		}

		if (operationInProgress()) {
			log.warn("Another operation is already in progress (" + operation
					+ ")"
			);
			return false;
		}

		operation = new TelosbRebootDeviceOperation(this);
		operation.setLogIdentifier(logIdentifier);
		operation.start();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ishell.device.iSenseDevice#triggerSetMacAddress(ishell.device.MacAddress,
	 * boolean)
	 */

	@Override
	public void triggerSetMacAddress(MacAddress mac, boolean rebootAfterFlashing)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		switch (event.getEventType()) {
			case SerialPortEvent.DATA_AVAILABLE:

				synchronized (bsl.dataAvailableMonitor) {
					bsl.dataAvailableMonitor.notifyAll();
				}
				if (operation == null) {
					receive(inputStream);
					/*
									 * try { // Read all available characters int packetLength = 0;
									 * byte [] packet = new byte[2048];
									 *
									 * while (inputStream != null && inputStream.available() != 0 &&
									 * (packetLength + 1) < packet.length) { packet[packetLength++]
									 * = (byte) (0xFF & inputStream.read()); }
									 *
									 * // Notify listeners //MessagePlainText p = new
									 * MessagePlainText(buffer); if (packetLength == 1) // strange
									 * short content
									 * logDebug("received from telosB with strange content!"); else
									 * { if (iSenseTelos == true) { // Copy them into a buffer with
									 * correct length byte[] buffer = new byte[packetLength];
									 * System.arraycopy(packet, 0, buffer, 0, packetLength);
									 * MessagePacket p = new MessagePacket(buffer[0],buffer);
									 * notifyReceivePacket(p); logDebug("received from telosB: "+
									 * (new String(buffer,0,buffer.length-1))); } else { // Copy
									 * them into a buffer with correct length byte[] buffer = new
									 * byte[packetLength +1]; // all messages as debug because we
									 * don't have a packet type buffer[0] =
									 * SerialMonitor.logTypeDebug; System.arraycopy(packet, 0,
									 * buffer, 1, packetLength); MessagePacket p2 = new
									 * MessagePacket(PacketTypes.LOG,buffer);
									 * notifyReceivePacket(p2); logDebug("received from telosB: "+
									 * (new String(buffer,0,buffer.length-1))); }
									 *
									 * } // Reset packet information packetLength = 0;
									 *
									 * } catch (IOException error) { logDebug("Error: "+error,
									 * error); }
									 */
				}
				break;
			default:
				logDebug("Serial event (other than data available): " + event);
				break;
		}
	}

	/**
	 * @return name of the serial port
	 */
	public String getPortName() {
		return serialPortName;
	}

	/*
	 * Start up the boot loader and do initialization for programming the
	 * device. After BSL startup, flash memory is erased first, afterwards the
	 * default password is transmitted to unlock all password protected
	 * commands. TODO: For old BSL versions (<= 1.1), a necessary patch must be
	 * applied.
	 */

	private boolean startBSL() throws TimeoutException, IOException,
			UnexpectedResponseException, InvalidChecksumException,
			ReceivedIncorrectDataException {
		byte[] reply;
		int bslVersion;
		int deviceId;
		String replyString = "";

		// invoke boot loader
		log.info("Starting boot loader...");
		if (!bsl.invokeBSL()) {
			if (log.isDebugEnabled()) {
				logDebug("Failed to start boot loader.");
			}
			return false;
		}

		// perform mass erase to reset the password to default password
		log.info("Erasing flash memory...");
		log.info("Erasing flash memory...2");
		bsl.sendBSLCommand(BSLTelosb.CMD_MASSERASE, 0xFFFF, 0xA506, null, false);
		reply = bsl.receiveBSLReply();
		log.info("Erasing flash memory...3");
		if ((reply[0] & 0xff) == BSLTelosb.DATA_NACK) {
			if (log.isDebugEnabled()) {
				logError("Failed to perform mass erase, NACK received.");
			}
			return false;
		} else if (reply.length > 1) {
			if (log.isDebugEnabled()) {
				log
						.error("Failed to perform mass erase, reply length unexpected.");
			}
			return false;
		}

		// send default password
		log.info("Transmitting password...");
		if (!bsl.transmitPassword(null, false)) {
			logError("Failed to transmit password, received NACK.");
			return false;
		}

		// read boot loader version
		if (log.isDebugEnabled()) {
			logDebug("Reading BSL version...");
		}
		bsl.sendBSLCommand(BSLTelosb.CMD_RXBSLVERSION, 0, 0, null, false);
		reply = bsl.receiveBSLReply();

		if (reply.length != 16) {
			for (int i = 0; i < reply.length; i++) {
				replyString += String.format(" 0x%02x ", reply[i]);
			}
			if (log.isDebugEnabled()) {
				log
						.error("Unable to read BSL version, reply length is unexpected: "
								+ replyString
						);
			}
			return false;
		}

		deviceId = (((reply[0] & 0xFF) << 8) | (reply[1] & 0xFF));
		bslVersion = (((reply[10] & 0xFF) << 8) | (reply[11] & 0xFF));

		if (log.isDebugEnabled()) {
			logDebug(String.format(
					"Current bsl version: %02x.%02x, device id: 0x%04x",
					((bslVersion >> 8) & 0xFF), (bslVersion & 0xFF), deviceId
			)
			);
		}

		// check if patch is required
		if (bslVersion <= 0x0110) {
			logError("Current BSL version is 1.1 or below, patch is required");
			// TODO: load patch
			return false;
		}

		// change baudrate to 38000
		if (bslVersion >= 0x0160) {
			if (!bsl.changeBaudrate(BSLTelosb.BaudRate.Baud38000)) {
				log
						.warn("Could not change the baud rate, keeping initial baud rate of 9600.");
				System.out
						.println("Could not change the baud rate, keeping initial baud rate of 9600.");
			}
		}
		System.out.println("BSL end");
		return true;
	}

	private void flushReceiveBuffer() {
		long i = 0;

		try {
			while ((i = inputStream.available()) > 0) {
				if (log.isDebugEnabled()) {
					logDebug("Flushing serial rx buffer: " + i
							+ "bytes skipped."
					);
				}
				inputStream.skip(i);
			}
		} catch (IOException e) {
			log.warn("Error while serial rx flushing buffer: " + e, e);
		}
	}

	@Override
	public IDeviceBinFile loadBinFile(String fileName) throws Exception {
		return new TelosbBinFile(fileName);
	}

	private void setComPort(ComPortMode mode)
			throws UnsupportedCommOperationException {
		// SettingsKey baudRateKey = (mode == ComPortMode.Program) ? SettingsKey
		// .flash_baudrate : SettingsKey.read_baudrate;

		try {
			if (mode == ComPortMode.Program) {
				bsl.changeComPort(FLASH_BAUDRATE, BSL_PARITY_EVEN);
			} else {
				bsl.changeComPort(READ_BAUDRATE, BSL_PARITY_NONE);
			}
		} catch (IOException e) {
			logError("" + e, e);
		}
	}

	public boolean isConnected() {
		return connected;
	}

}
