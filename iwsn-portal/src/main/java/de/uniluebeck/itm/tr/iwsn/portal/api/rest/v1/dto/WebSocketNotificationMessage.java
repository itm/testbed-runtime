package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WebSocketNotificationMessage {

	@XmlElement(name = "type")
	public final String type = "notification";

	@XmlElement(name = "message")
	public String message;

	@XmlElement(name = "timestamp")
	public String timestamp;

	public WebSocketNotificationMessage() {
	}

	public WebSocketNotificationMessage(final String timestamp, final String message) {
		this.timestamp = timestamp;
		this.message = message;
	}

	@Override
	public String toString() {
		return "WebSocketNotificationMessage{" +
				"timestamp='" + timestamp + '\'' +
				", message='" + message + '\'' +
				'}';
	}
}
