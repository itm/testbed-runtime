package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import com.google.common.base.MoreObjects;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import de.uniluebeck.itm.tr.iwsn.messages.Progress;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static com.google.common.collect.Iterables.transform;

@XmlRootElement
public class SingleNodeProgressMessage {

	@XmlElement(required = true)
	public final String type = "singleNodeProgress";

	@XmlElement(required = true)
	public DateTime timestamp;

	@XmlElement(required = true)
	public long requestId;

	@XmlElement(required = true)
	public String nodeUrn;

	@XmlElement(required = true)
	public int progressInPercent;

	@SuppressWarnings("unused")
	public SingleNodeProgressMessage() {
	}

	public SingleNodeProgressMessage(final Progress progress, final String nodeUrn) {
		this.timestamp = new DateTime(progress.getHeader().getTimestamp());
		this.requestId = progress.getHeader().getCorrelationId();
		this.nodeUrn = nodeUrn;
		this.progressInPercent = progress.getProgressInPercent();
	}

	public static Iterable<SingleNodeProgressMessage> convert(MessageHeaderPair pair) {
		Progress response = (Progress) pair.message;
		return transform(response.getHeader().getNodeUrnsList(), n -> new SingleNodeProgressMessage(response, n));
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", type)
				.add("timestamp", timestamp)
				.add("requestId", requestId)
				.add("nodeUrn", nodeUrn)
				.add("progressInPercent", progressInPercent)
				.toString();
	}
}
