package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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

	public WebSocketUpstreamMessage() {
	}

	public WebSocketUpstreamMessage(final String timestamp, final String sourceNodeUrn, final String payloadBase64) {
		this.timestamp = timestamp;
		this.sourceNodeUrn = sourceNodeUrn;
		this.payloadBase64 = payloadBase64;
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
