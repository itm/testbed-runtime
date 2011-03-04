import de.uniluebeck.itm.tr.util.RateLimiter;
import de.uniluebeck.itm.tr.util.RateLimiterImpl;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class RateLimiterUnitTest {
	RateLimiter rateLimiter;
	int slotLength = 2;
	TimeUnit timeUnit = TimeUnit.SECONDS;

	private class RateLimiterCheckApprovedRunnable implements Runnable {
		private RateLimiter rateLimiter;

		public RateLimiterCheckApprovedRunnable(RateLimiter rateLimiter){
			this.rateLimiter = rateLimiter;
		}
		@Override
		public void run() {
			assertTrue(rateLimiter.checkIfInSlotAndCount());
		}
	}
	private class RateLimiterCheckDismissedRunnable implements Runnable {
		private RateLimiter rateLimiter;

		public RateLimiterCheckDismissedRunnable(RateLimiter rateLimiter){
			this.rateLimiter = rateLimiter;
		}
		@Override
		public void run() {
			assertFalse(rateLimiter.checkIfInSlotAndCount());
		}
	}

	@Test
	public void checkIfAllPassedObjectsSuccessfullyApprovedInOneSlot() {
		rateLimiter = new RateLimiterImpl(10, slotLength, timeUnit);
		for (int i = 0; i < 10; i++) {
			assertTrue(rateLimiter.checkIfInSlotAndCount());
			assertTrue(rateLimiter.approvedCount() == i + 1);
			assertTrue(rateLimiter.dismissedCount() == 0);
		}
		assertTrue(rateLimiter.approvedCount() == 10);
		assertTrue(rateLimiter.dismissedCount() == 0);
	}

	@Test
	public void checkIfAllPassedObjectsDismissesInOneSlot() {
		rateLimiter = new RateLimiterImpl(0, slotLength, timeUnit);
		for (int i = 0; i < 10; i++) {
			assertFalse(rateLimiter.checkIfInSlotAndCount());
			assertTrue(rateLimiter.dismissedCount() == i + 1);
			assertTrue(rateLimiter.approvedCount() == 0);
		}
		assertTrue(rateLimiter.dismissedCount() == 10);
		assertTrue(rateLimiter.approvedCount() == 0);
	}

	@Test
	public void checkIfAllPassedObjectsSuccessfullyApprovedForTwoSlots() throws InterruptedException {
		rateLimiter = new RateLimiterImpl(10, slotLength, timeUnit);
		for (int i = 0; i < 10; i++) {
			assertTrue(rateLimiter.checkIfInSlotAndCount());
			assertTrue(rateLimiter.approvedCount() == i + 1);
			assertTrue(rateLimiter.dismissedCount() == 0);
		}
		assertFalse(rateLimiter.checkIfInSlotAndCount());
		assertTrue(rateLimiter.dismissedCount() == 1);
		//move on to next slot
		rateLimiter.nextSlot();
		for (int i = 0; i < 10; i++) {
			assertTrue(rateLimiter.checkIfInSlotAndCount());
			assertTrue(rateLimiter.approvedCount() == i + 1);
			assertTrue(rateLimiter.dismissedCount() == 0);
		}
	}

//	@Test
//	public void checkThreadSafetyOfRateLimiter() throws Exception {
//		ExecutorService executorService = Executors.newFixedThreadPool(4);
//		rateLimiter = new RateLimiterImpl(10, 2, timeUnit);
//		List<Future> futures = new ArrayList<Future>();
//		for (int i = 0; i < 10; i++) {
//			futures.add(executorService.submit(new RateLimiterCheckApprovedRunnable(rateLimiter)));
//		}
//		for (int i = 0; i < 10; i++) {
//			futures.add(executorService.submit(new RateLimiterCheckDismissedRunnable(rateLimiter)));
//		}
//		for (Future future : futures){
//			Object obj = future.get();
//			assertNull(obj);
//		}
//		assertEquals(rateLimiter.approvedCount(), 10);
//		assertEquals(rateLimiter.dismissedCount(), 10);		
//	}
}
