package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GatewayConnectedMessage {

	public final String type = "gatewayConnected";

	public DateTime timestamp;

	public String hostname;

	@SuppressWarnings("UnusedDeclaration")
	public GatewayConnectedMessage() {
	}

	public GatewayConnectedMessage(final GatewayConnectedEvent event) {
		this.timestamp = new DateTime(event.getTimestamp());
		this.hostname = event.getHostname();
	}

	@Override
	public String toString() {
		return "GatewayConnectedMessage{" +
				"timestamp=" + timestamp +
				", hostname='" + hostname + '\'' +
				'}';
	}
}
