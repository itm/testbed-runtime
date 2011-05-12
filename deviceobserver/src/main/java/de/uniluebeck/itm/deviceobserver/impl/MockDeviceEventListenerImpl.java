package de.uniluebeck.itm.deviceobserver.impl;

import de.uniluebeck.itm.deviceobserver.DeviceEventListener;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotConnectableException;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotDisconnectableException;
import de.uniluebeck.itm.deviceobserver.util.DeviceObserverUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Mock-Implementation of DeviceEventListener for testing
 */
public class MockDeviceEventListenerImpl implements DeviceEventListener {
	private Object connection;
	private static Logger logger = LoggerFactory.getLogger(MockDeviceObserverImpl.class);

	@Override
	public void connected(Map<String, String> device) throws DeviceNotConnectableException {
		if (device.get(DeviceObserverUtils.KEY_DEVICE_TYPE).contains("doNotConnect")) {
			logger.warn("Device Not Connectable");
			throw new DeviceNotConnectableException("Device Not Connectable");
		}
		this.connection = new Object();
		logger.debug("Device connected");
	}

	@Override
	public void disconnected(Map<String, String> device) throws DeviceNotDisconnectableException {
		if (connection == null) {
			logger.warn("Device Not Disconnectable");
			throw new DeviceNotDisconnectableException("Device Not Disconnectable");
		}
		this.connection = null;
		logger.debug("Device disconnected");
	}
}
