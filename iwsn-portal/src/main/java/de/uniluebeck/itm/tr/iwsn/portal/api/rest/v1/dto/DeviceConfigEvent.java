package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import org.joda.time.DateTime;

import java.util.List;
import java.util.function.Function;

public class DeviceConfigEvent {

	public String type;

	public List<String> nodeUrns;

	public DateTime timestamp;

	@SuppressWarnings("unused")
	public DeviceConfigEvent() {
		// reflection constructor
	}

	public DeviceConfigEvent(MessageHeaderPair pair) {
		switch (pair.header.getType()) {
			case EVENT_DEVICE_CONFIG_CREATED:
				this.type = "deviceConfigCreated";
				break;
			case EVENT_DEVICE_CONFIG_DELETED:
				this.type = "deviceConfigDeleted";
				break;
			case EVENT_DEVICE_CONFIG_UPDATED:
				this.type = "deviceConfigUpdated";
				break;
			default:
				throw new IllegalArgumentException("Expected device config created|deleted|updated event, got: " + pair.header.getType());
		}
		this.nodeUrns = pair.header.getNodeUrnsList();
		this.timestamp = new DateTime(pair.header.getTimestamp());
	}

	public static final Function<MessageHeaderPair, DeviceConfigEvent> CONVERT = DeviceConfigEvent::new;
}
