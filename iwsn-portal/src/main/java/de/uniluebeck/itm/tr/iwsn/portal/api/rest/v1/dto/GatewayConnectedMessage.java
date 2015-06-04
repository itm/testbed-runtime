package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.function.Function;

@XmlRootElement
public class GatewayConnectedMessage {

	public final String type = "gatewayConnected";

	public DateTime timestamp;

	public String hostname;

	@SuppressWarnings("UnusedDeclaration")
	public GatewayConnectedMessage() {
	}

	public GatewayConnectedMessage(final GatewayConnectedEvent event) {
		this.timestamp = new DateTime(event.getHeader().getTimestamp());
		this.hostname = event.getHostname();
	}

	public GatewayConnectedMessage(final MessageHeaderPair pair) {
		this((GatewayConnectedEvent) pair.message);
	}

	public static final Function<MessageHeaderPair, GatewayConnectedMessage> CONVERT = GatewayConnectedMessage::new;

	@Override
	public String toString() {
		return "GatewayConnectedMessage{" +
				"timestamp=" + timestamp +
				", hostname='" + hostname + '\'' +
				'}';
	}
}
