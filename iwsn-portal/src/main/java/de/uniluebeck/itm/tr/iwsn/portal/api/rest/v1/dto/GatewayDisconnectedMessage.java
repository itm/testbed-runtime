package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.GatewayDisconnectedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.function.Function;

@XmlRootElement
public class GatewayDisconnectedMessage {

	public final String type = "gatewayDisconnected";

	public DateTime timestamp;

	public String hostname;

	public List<String> nodeUrns;

	@SuppressWarnings("unused")
	public GatewayDisconnectedMessage() {
	}

	public GatewayDisconnectedMessage(final GatewayDisconnectedEvent event) {
		this.timestamp = new DateTime(event.getHeader().getTimestamp());
		this.hostname = event.getHostname();
		this.nodeUrns = event.getHeader().getNodeUrnsList();
	}

	public GatewayDisconnectedMessage(final MessageHeaderPair pair) {
		this((GatewayDisconnectedEvent) pair.message);
	}

	public static final Function<MessageHeaderPair, GatewayDisconnectedMessage> CONVERT = GatewayDisconnectedMessage::new;

	@Override
	public String toString() {
		return "GatewayDisconnectedMessage{" +
				"timestamp=" + timestamp +
				", hostname='" + hostname + '\'' +
				", nodeUrns=" + nodeUrns +
				'}';
	}
}
