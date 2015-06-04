package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import com.google.common.collect.Iterables;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import de.uniluebeck.itm.tr.iwsn.messages.Response;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.transform;

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

	public static final Iterable<SingleNodeResponseMessage> convert(MessageHeaderPair pair) {
		Response response = (Response) pair.message;
		return transform(response.getHeader().getNodeUrnsList(), n -> new SingleNodeResponseMessage(response, n));
	}

	@SuppressWarnings("unused")
	public SingleNodeResponseMessage() {
	}

	public SingleNodeResponseMessage(final Response response, final String nodeUrn) {
		this.timestamp = new DateTime(response.getHeader().getTimestamp());
		this.requestId = response.getHeader().getCorrelationId();
		this.nodeUrn = nodeUrn;
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
