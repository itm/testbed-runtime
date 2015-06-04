package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@XmlRootElement
public class GetChannelPipelinesResponseMessage {

	public final String type = "getChannelPipelinesResponse";

	public DateTime timestamp;

	public List<ChannelHandlerConfigurationList> pipelines;

	@SuppressWarnings("unused")
	public GetChannelPipelinesResponseMessage() {
		// reflection constructor
	}

	public GetChannelPipelinesResponseMessage(final MessageHeaderPair pair) {
		this((GetChannelPipelinesResponse) pair.message);
	}

	public GetChannelPipelinesResponseMessage(GetChannelPipelinesResponse response) {
		this.timestamp = new DateTime(response.getHeader().getTimestamp());
		Stream<ChannelHandlerConfigurationList> stream = response.getPipelinesList().stream().map(ChannelHandlerConfigurationList::new);
		this.pipelines = stream.collect(Collectors.toList());
	}

	public static final Function<MessageHeaderPair, GetChannelPipelinesResponseMessage> CONVERT = GetChannelPipelinesResponseMessage::new;
}
