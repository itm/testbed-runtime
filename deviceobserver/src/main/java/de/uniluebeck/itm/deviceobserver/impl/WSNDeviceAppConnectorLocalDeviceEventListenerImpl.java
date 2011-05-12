package de.uniluebeck.itm.deviceobserver.impl;

import de.uniluebeck.itm.deviceobserver.DeviceEventListener;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotConnectableException;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotDisconnectableException;

import java.util.Map;

public class WSNDeviceAppConnectorLocalDeviceEventListenerImpl implements DeviceEventListener {
	private boolean isConnected = false;

	@Override
	public void connected(Map<String, String> device) throws DeviceNotConnectableException {
		if (!isConnected) {
			isConnected = true;
		}
	}

	@Override
	public void disconnected(Map<String, String> device) throws DeviceNotDisconnectableException {
		if (isConnected){
			isConnected = false;
		}
	}
}
