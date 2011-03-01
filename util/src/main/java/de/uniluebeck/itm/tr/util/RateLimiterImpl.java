package de.uniluebeck.itm.tr.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RateLimiterImpl implements RateLimiter {
	private int approvalsPerTimeSlot;
	private int approvedElements;
	private int dismissedElements;
	private TimeDiff timer;
	private final Lock reentrantLock;

	public RateLimiterImpl(int approvalsPerTimeSlot, int slotLength, TimeUnit timeUnit) {
		this.approvalsPerTimeSlot = approvalsPerTimeSlot;
		this.timer = new TimeDiff(timeUnit.toMillis(slotLength));
		approvedElements = 0;
		dismissedElements = 0;
		reentrantLock = new ReentrantLock();
	}

	@Override
	public boolean checkAndCount() {
		reentrantLock.lock();
		try {
			checkIfNextSlot();
			if (approvedElements < approvalsPerTimeSlot) {
				approvedElements++;
				return true;
			} else {
				dismissedElements++;
				return false;
			}
		} finally {
			reentrantLock.unlock();
		}
	}

	@Override
	public int approvedCount() {
		reentrantLock.lock();
		try {
			return this.approvedElements;
		} finally {
			reentrantLock.unlock();
		}
	}

	@Override
	public int dismissedCount() {
		reentrantLock.lock();
		try {
			return this.dismissedElements;
		} finally {
			reentrantLock.unlock();
		}
	}

	private void checkIfNextSlot() {
		if (timer.isTimeout()) {
			timer.touch();
			approvedElements = 0;
			dismissedElements = 0;
		}
	}
}
