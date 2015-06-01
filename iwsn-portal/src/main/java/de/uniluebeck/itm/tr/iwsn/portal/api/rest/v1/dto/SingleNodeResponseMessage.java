package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.Response;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SingleNodeResponseMessage {

	@XmlElement(required = true)
	public final String type = "singleNodeResponse";

	@XmlElement(required = true)
	public DateTime timestamp;

	@XmlElement(required = true)
	public long requestId;

	@XmlElement(required = true)
	public String nodeUrn;

	@XmlElement(required = false)
	public String response;

	@XmlElement(required = true)
	public int statusCode;

	@XmlElement(required = false)
	public String errorMessage;

	public SingleNodeResponseMessage(final Response response, final DateTime timestamp) {
		this.timestamp = timestamp;
		this.requestId = response.getHeader().getCorrelationId();
		this.nodeUrn = response.getHeader().get;
		this.response = response.hasResponse() ? new String(response.getResponse().toByteArray()) : null;
		this.statusCode = response.getStatusCode();
		this.errorMessage = response.getErrorMessage();
	}

	@Override
	public String toString() {
		return "SingleNodeResponseMessage{" +
				"type='" + type + '\'' +
				", nodeUrn=" + nodeUrn + '\'' +
				", requestId=" + requestId +
				'}';
	}
}
