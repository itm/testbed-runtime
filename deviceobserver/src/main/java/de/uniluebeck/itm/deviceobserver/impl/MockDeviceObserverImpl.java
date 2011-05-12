package de.uniluebeck.itm.deviceobserver.impl;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 04.05.11
 * Time: 14:32
 * To change this template use File | Settings | File Templates.
 */
public class MockDeviceObserverImpl extends DeviceObserverImpl {

	public synchronized void setCsvDevices(String[] csv) {
		super.setCsvDevices(csv);
	}

	//runs one check for connected and disconnected Devices
	public void run() {
		super.addAndRemoveListener();
	}

}
