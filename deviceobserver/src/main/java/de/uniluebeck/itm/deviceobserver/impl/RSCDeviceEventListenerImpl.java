/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

package de.uniluebeck.itm.deviceobserver.impl;

import de.uniluebeck.itm.deviceobserver.DeviceEventListener;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotConnectableException;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotDisconnectableException;
import de.uniluebeck.itm.deviceobserver.util.DeviceObserverUtils;
import de.uniluebeck.itm.rsc.drivers.core.Connection;
import de.uniluebeck.itm.rsc.drivers.core.mockdevice.MockConnection;
import de.uniluebeck.itm.rsc.drivers.isense.iSenseSerialPortConnection;
import de.uniluebeck.itm.rsc.drivers.pacemate.PacemateSerialPortConnection;
import de.uniluebeck.itm.rsc.drivers.telosb.TelosbSerialPortConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the DeviceEventListener-Interface using the
 * rsc (Remote Sensor Control) -project from the Institute of Telematics at the University of Luebeck
 */
public class RSCDeviceEventListenerImpl implements DeviceEventListener {
	private Connection connection = null;
	private final static Logger LOGGER = LoggerFactory.getLogger(RSCDeviceEventListenerImpl.class);
	private ScheduledThreadPoolExecutor connectionExecutor;
	private final long connectionTimeOut;

	public RSCDeviceEventListenerImpl() {
		this.connectionExecutor = new ScheduledThreadPoolExecutor(1);
		connectionTimeOut = 1000;
	}

	@Override
	public void connected(Map<String, String> device) throws DeviceNotConnectableException {
		LOGGER.debug("... trying to connect device: {}", device);
		createConnection(device.get(DeviceObserverUtils.KEY_DEVICE_TYPE), device.get(DeviceObserverUtils.KEY_DEVICE_PORT));
		LOGGER.debug("... successfully connected device: {}", device);
	}

	@Override
	public void disconnected(Map<String, String> device) throws DeviceNotDisconnectableException {
		LOGGER.debug("... trying to disconnect device: {}", device);
		disconnect();
		LOGGER.debug("... successfully disconnected device: {}", device);
	}

	private void disconnect() throws DeviceNotDisconnectableException {
		if (connection == null) {
			LOGGER.warn("Could not disconnect device! No connection available!");
			throw new DeviceNotDisconnectableException();
		}
		connection.shutdown(true);
	}

	private void createConnection(String deviceType, String devicePort) throws DeviceNotConnectableException {
		try {
			Future<Connection> futureConnection = connectionExecutor.submit(new ConnectionCallable(deviceType, devicePort));

			if (futureConnection != null) {
				this.connection = futureConnection.get(connectionTimeOut, TimeUnit.MILLISECONDS);
			}

		} catch (Exception e) {
			String message = "Exception in DeviceEventListener:createConnection! Cause: " + e.getLocalizedMessage();
			LOGGER.warn(message);
			throw new DeviceNotConnectableException();
		}
	}

	/**
	 * try to create instance of Connection, connect it to port and return instance
	 */
	private class ConnectionCallable implements Callable<Connection> {
		private String deviceType;
		private String devicePort;
		private Connection conn;

		public ConnectionCallable(String deviceType, String devicePort) {
			this.deviceType = deviceType;
			this.devicePort = devicePort;
		}

		@Override
		public Connection call() throws Exception {
			if (deviceType.contains("Pacemate")) {
				this.conn = new PacemateSerialPortConnection();
			} else if (deviceType.contains("iSense")) {
				this.conn = new iSenseSerialPortConnection();
			} else if (deviceType.contains("Telos")) {
				this.conn = new TelosbSerialPortConnection();
			} else if (deviceType.contains("Mock")) {
				this.conn = new MockConnection();
			}
			if (this.conn != null) {
				this.conn.connect(devicePort);
			} else {
				LOGGER.warn("Could not create connection for type: " + deviceType);
				throw new DeviceNotConnectableException();
			}
			return this.conn;
		}
	}
}
