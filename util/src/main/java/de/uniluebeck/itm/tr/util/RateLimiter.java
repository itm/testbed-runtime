package de.uniluebeck.itm.tr.util;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 28.02.11
 * Time: 17:24
 * To change this template use File | Settings | File Templates.
 */
public interface RateLimiter {

	public boolean checkIfInSlotAndCount();

	public int approvedCount();

	public int dismissedCount();

	public void nextSlot();
}
