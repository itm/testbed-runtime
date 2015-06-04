package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import com.google.common.collect.Iterables;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.function.Function;

@XmlRootElement
public class DevicesAttachedMessage {

	@XmlElement(name = "type")
	public final String type = "devicesAttached";

	@XmlElement(name = "nodeUrns")
	public String[] nodeUrns;

	@XmlElement(name = "timestamp")
	public DateTime timestamp;

	@SuppressWarnings("unused")
	public DevicesAttachedMessage() {
	}

	public DevicesAttachedMessage(final DateTime timestamp, final String... nodeUrns) {
		this.nodeUrns = nodeUrns;
		this.timestamp = timestamp;
	}

	public DevicesAttachedMessage(final DevicesAttachedEvent event) {
		this.nodeUrns = Iterables.toArray(event.getHeader().getNodeUrnsList(), String.class);
		this.timestamp = new DateTime(event.getHeader().getTimestamp());
	}

	public DevicesAttachedMessage(final MessageHeaderPair pair) {
		this((DevicesAttachedEvent) pair.message);
	}

	public static final Function<MessageHeaderPair, DevicesAttachedMessage> CONVERT = DevicesAttachedMessage::new;

	@Override
	public String toString() {
		return "DevicesAttachedMessage{" +
				"type='" + type + '\'' +
				", timestamp='" + timestamp.toString(ISODateTimeFormat.dateTime()) + '\'' +
				", nodeUrns=" + Arrays.toString(nodeUrns) +
				"} " + super.toString();
	}
}
