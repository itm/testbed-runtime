package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import com.google.common.collect.Iterables;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.function.Function;

@XmlRootElement
public class DevicesDetachedMessage {

	@XmlElement(name = "type")
	public final String type = "devicesDetached";

	@XmlElement(name = "nodeUrns")
	public String[] nodeUrns;

	@XmlElement(name = "timestamp")
	public DateTime timestamp;

	@SuppressWarnings("unused")
	public DevicesDetachedMessage() {
	}

	public DevicesDetachedMessage(final DateTime timestamp, final String... nodeUrns) {
		this.nodeUrns = nodeUrns;
		this.timestamp = timestamp;
	}

	public DevicesDetachedMessage(final DevicesDetachedEvent event) {
		this.nodeUrns = Iterables.toArray(event.getHeader().getNodeUrnsList(), String.class);
		this.timestamp = new DateTime(event.getHeader().getTimestamp());
	}

	public DevicesDetachedMessage(final MessageHeaderPair pair) {
		this((DevicesDetachedEvent) pair.message);
	}

	public static final Function<MessageHeaderPair, DevicesDetachedMessage> CONVERT = DevicesDetachedMessage::new;

	@Override
	public String toString() {
		return "DevicesDetachedMessage{" +
				"type='" + type + '\'' +
				", timestamp='" + timestamp.toString(ISODateTimeFormat.dateTime()) + '\'' +
				", nodeUrns=" + Arrays.toString(nodeUrns) +
				"} " + super.toString();
	}
}
