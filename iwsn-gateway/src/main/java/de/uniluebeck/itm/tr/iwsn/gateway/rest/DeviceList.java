package de.uniluebeck.itm.tr.iwsn.gateway.rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "nodeUrns")
public class DeviceList {

	@XmlElement(name = "nodeUrn")
	public List<String> nodeUrns;

	public DeviceList() {
	}

	public DeviceList(final List<String> nodeUrns) {
		this.nodeUrns = nodeUrns;
	}
}
