package de.uniluebeck.itm.tr.iwsn.messages;

import static com.google.common.collect.Sets.newHashSet;

public abstract class MessageUtils {

	public static boolean equals(final DevicesAttachedEvent event1, final DevicesAttachedEvent event2) {
		return event1.getHeader().getTimestamp() == event2.getHeader().getTimestamp() &&
				newHashSet(event1.getHeader().getNodeUrnsList()).equals(newHashSet(event2.getHeader().getNodeUrnsList()));
	}

	public static boolean equals(final DevicesDetachedEvent event1, final DevicesDetachedEvent event2) {
		return event1.getHeader().getTimestamp() == event2.getHeader().getTimestamp() &&
				newHashSet(event1.getHeader().getNodeUrnsList()).equals(newHashSet(event2.getHeader().getNodeUrnsList()));
	}

	public static boolean isErrorStatusCode(final Response response) {
		return response.getStatusCode() == getUnconnectedStatusCode(response.getHeader().getType()) || response.getStatusCode() < 0;
	}

	public static int getUnconnectedStatusCode(final MessageType messageType) {
		switch (messageType) {
			case REQUEST_ARE_NODES_ALIVE:
				return 0;
			case REQUEST_ARE_NODES_CONNECTED:
				return 0;
			default:
				return -1;
		}
	}
}
