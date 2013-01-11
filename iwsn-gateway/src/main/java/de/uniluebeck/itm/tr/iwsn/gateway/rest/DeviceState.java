package de.uniluebeck.itm.tr.iwsn.gateway.rest;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DeviceState {

	public boolean isNodeConnected;

	public boolean isNodeAlive;

	public DeviceState() {
	}

	public DeviceState(final boolean nodeAlive, final boolean nodeConnected) {
		isNodeAlive = nodeAlive;
		isNodeConnected = nodeConnected;
	}
}
