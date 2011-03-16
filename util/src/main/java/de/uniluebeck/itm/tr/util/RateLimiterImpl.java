package de.uniluebeck.itm.tr.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public class RateLimiterImpl implements RateLimiter {

	private final int approvalsPerTimeSlot;

	private int approvedCount;

	private int dismissedCount;

	private final TimeDiff timer;

	private final Lock countLock;

	public RateLimiterImpl(int approvalsPerTimeSlot, int slotLength, TimeUnit timeUnit) {
		this.approvalsPerTimeSlot = approvalsPerTimeSlot;
		this.timer = new TimeDiff(timeUnit.toMillis(slotLength));
		approvedCount = 0;
		dismissedCount = 0;
		countLock = new ReentrantLock();
	}

	/**
	 * check if passed objects of current time slot still below allowed approvals per time slot if so do increase approved
	 * elements if not increase dismissed elements
	 *
	 * @return boolean
	 */
	@Override
	public boolean checkIfInSlotAndCount() {
		countLock.lock();
		try {
			moveToNextSlotIfTimeout();
			if (approvedCount < approvalsPerTimeSlot) {
				approvedCount++;
				return true;
			} else {
				dismissedCount++;
				return false;
			}
		} finally {
			countLock.unlock();
		}
	}

	/**
	 * returning count of approved elements
	 *
	 * @return int
	 */
	@Override
	public int approvedCount() {
		countLock.lock();
		try {
			return this.approvedCount;
		} finally {
			countLock.unlock();
		}
	}

	/**
	 * returning count of dismissed elements
	 *
	 * @return int
	 */
	@Override
	public int dismissedCount() {
		countLock.lock();
		try {
			return this.dismissedCount;
		} finally {
			countLock.unlock();
		}
	}

	/**
	 * manually move to next slot
	 */
	@Override
	public void nextSlot() {
		timer.touch();
		approvedCount = 0;
		dismissedCount = 0;
	}

	/**
	 * check if timeout reached; if so move on to next slot
	 */
	private void moveToNextSlotIfTimeout() {
		if (timer.isTimeout()) {
			nextSlot();
		}
	}
}
