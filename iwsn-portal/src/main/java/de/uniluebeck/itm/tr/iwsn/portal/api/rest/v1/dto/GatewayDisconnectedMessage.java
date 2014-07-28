package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.GatewayDisconnectedEvent;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

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
		this.timestamp = new DateTime(event.getTimestamp());
		this.hostname = event.getHostname();
		this.nodeUrns = event.getNodeUrnsList();
	}

	@Override
	public String toString() {
		return "GatewayDisconnectedMessage{" +
				"timestamp=" + timestamp +
				", hostname='" + hostname + '\'' +
				", nodeUrns=" + nodeUrns +
				'}';
	}
}
