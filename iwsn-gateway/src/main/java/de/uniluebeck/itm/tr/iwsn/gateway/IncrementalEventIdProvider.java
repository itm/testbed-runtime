package de.uniluebeck.itm.tr.iwsn.gateway;

public class IncrementalEventIdProvider implements EventIdProvider {

	private final Object lock = new Object();

	private long lastEventId = 0;

	@Override
	public Long get() {
		synchronized (lock) {
			lastEventId = lastEventId == Long.MAX_VALUE ? 0 : lastEventId++;
			return lastEventId;
		}
	}
}
