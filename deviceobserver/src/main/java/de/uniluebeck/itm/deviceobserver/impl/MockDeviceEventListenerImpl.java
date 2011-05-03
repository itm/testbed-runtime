package de.uniluebeck.itm.deviceobserver.impl;

import de.uniluebeck.itm.deviceobserver.DeviceEventListener;
import de.uniluebeck.itm.deviceobserver.DeviceObserver;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotConnectableException;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotDisconnectableException;
import de.uniluebeck.itm.deviceobserver.util.DeviceObserverUtils;

import java.util.Map;

/**
 * Mock-Implementation of DeviceEventListener for testing
 */
public class MockDeviceEventListenerImpl implements DeviceEventListener {
	private Object connection;

	@Override
	public void connected(Map<String, String> device) throws DeviceNotConnectableException {
		if (device.get(DeviceObserverUtils.KEY_DEVICE_TYPE).contains("none")) {
			throw new DeviceNotConnectableException();
		}
		this.connection = new Object();
	}

	@Override
	public void disconnected(Map<String, String> device) throws DeviceNotDisconnectableException {
		if (connection == null) {
			throw new DeviceNotDisconnectableException();
		}
		this.connection = null;
	}
}
