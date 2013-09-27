package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static de.uniluebeck.itm.tr.iwsn.common.Base64Helper.encode;

@XmlRootElement
public class WebSocketUpstreamMessage {

	@XmlElement(name = "type")
	public final String type = "upstream";

	@XmlElement(name = "payloadBase64")
	public String payloadBase64;

	@XmlElement(name = "sourceNodeUrn")
	public String sourceNodeUrn;

	@XmlElement(name = "timestamp")
	public String timestamp;

	@SuppressWarnings("unused")
	public WebSocketUpstreamMessage() {
	}

	public WebSocketUpstreamMessage(final UpstreamMessageEvent event) {
		this.timestamp = new DateTime(event.getTimestamp()).toString(ISODateTimeFormat.dateTime());
		this.sourceNodeUrn = event.getSourceNodeUrn();
		this.payloadBase64 = encode(event.getMessageBytes().toByteArray());
	}

	@Override
	public String toString() {
		return "WebSocketUpstreamMessage{" +
				"payloadBase64='" + payloadBase64 + "'" +
				", sourceNodeUrn='" + sourceNodeUrn + "'" +
				", timestamp='" + timestamp + "'" +
				"}";
	}
}
