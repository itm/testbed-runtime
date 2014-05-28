package de.uniluebeck.itm.tr.common;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IncrementalIdProvider implements IdProvider {

	private long lastIssued = 0;

	private final Lock lock = new ReentrantLock();

	@Override
	public Long get() {
		lock.lock();
		try {
			if (lastIssued == Integer.MAX_VALUE) {
				lastIssued = 0;
			} else {
				lastIssued += 1;
			}
			return lastIssued;
		} finally {
			lock.unlock();
		}
	}
}

