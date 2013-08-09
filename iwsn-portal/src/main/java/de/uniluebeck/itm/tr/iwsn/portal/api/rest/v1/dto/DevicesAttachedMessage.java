package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;

@XmlRootElement
public class DevicesAttachedMessage {

	@XmlElement(name = "type")
	public final String type = "devicesAttached";

	@XmlElement(name = "nodeUrns")
	public String[] nodeUrns;

	@XmlElement(name = "timestamp")
	public String timestamp;

	@SuppressWarnings("unused")
	public DevicesAttachedMessage() {
	}

	public DevicesAttachedMessage(final DateTime timestamp, final String... nodeUrns) {
		this.nodeUrns = nodeUrns;
		this.timestamp = timestamp.toString(ISODateTimeFormat.dateTime());
	}

	public DevicesAttachedMessage(final DevicesAttachedEvent event) {
		this(new DateTime(event.getTimestamp()), event.getNodeUrnsList().toArray(new String[event.getNodeUrnsCount()]));
	}

	@Override
	public String toString() {
		return "DevicesAttachedMessage{" +
				"type='" + type + '\'' +
				", timestamp='" + timestamp + '\'' +
				", nodeUrns=" + Arrays.toString(nodeUrns) +
				"} " + super.toString();
	}
}
