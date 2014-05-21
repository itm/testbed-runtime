package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WebSocketDownstreamMessage {

	@XmlElement(name = "type")
	public final String type = "downstream";
	
	@XmlElement(name = "payloadBase64")
	public String payloadBase64;

	@XmlElement(name = "targetNodeUrn")
	public String targetNodeUrn;

	@SuppressWarnings("UnusedDeclaration")
	public WebSocketDownstreamMessage() {
		// for reflection purposes
	}

	@Override
	public String toString() {
		return "WebSocketDownstreamMessage{" +
				"payloadBase64='" + payloadBase64 + '\'' +
				", targetNodeUrn='" + targetNodeUrn + '\'' +
				'}';
	}
}
