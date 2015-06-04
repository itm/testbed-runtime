package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;

@XmlRootElement
public class WebSocketNotificationMessage {

	@XmlElement(name = "type")
	public final String type = "notification";

	@XmlElement(name = "message")
	public String message;

	@XmlElement(name = "timestamp")
	public String timestamp;

	@XmlElement(name = "nodeUrn")
	public String nodeUrn;

	@SuppressWarnings("unused")
	public WebSocketNotificationMessage() {
	}

	public WebSocketNotificationMessage(final DateTime timestamp, @Nullable final String nodeUrn, final String message) {
		this.timestamp = timestamp.toString(ISODateTimeFormat.dateTime());
		this.nodeUrn = nodeUrn;
		this.message = message;
	}

	public WebSocketNotificationMessage(final NotificationEvent event) {
		this.timestamp = new DateTime(event.getHeader().getTimestamp()).toString(ISODateTimeFormat.dateTime());
		checkArgument(event.getHeader().getNodeUrnsCount() == 1, "Expected one source node URN, got " + event.getHeader().getNodeUrnsCount());
		this.nodeUrn = event.getHeader().getNodeUrns(0);
		this.message = event.getMessage();
	}

	public WebSocketNotificationMessage(final MessageHeaderPair pair) {
		this((NotificationEvent) pair.message);
	}

	public static final Function<MessageHeaderPair, WebSocketNotificationMessage> CONVERT = WebSocketNotificationMessage::new;

	@Override
	public String toString() {
		return "WebSocketNotificationMessage{" +
				"timestamp='" + timestamp + '\'' +
				", message='" + message + '\'' +
				'}';
	}
}
