import de.uniluebeck.itm.tr.util.RateLimiter;
import de.uniluebeck.itm.tr.util.RateLimiterImpl;
import org.junit.Test;

import java.nio.ByteBuffer;
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
			assertTrue(rateLimiter.checkAndCount());
		}
	}
	private class RateLimiterCheckDismissedRunnable implements Runnable {

		private RateLimiter rateLimiter;

		public RateLimiterCheckDismissedRunnable(RateLimiter rateLimiter){
			this.rateLimiter = rateLimiter;
		}
		@Override
		public void run() {
			assertFalse(rateLimiter.checkAndCount());
		}
	}

	@Test
	public void checkApprovedCounterForOneSlot() {
		rateLimiter = new RateLimiterImpl(10, slotLength, timeUnit);
		for (int i = 0; i < 10; i++) {
			assertTrue(rateLimiter.checkAndCount());
			assertTrue(rateLimiter.approvedCount() == i + 1);
			assertTrue(rateLimiter.dismissedCount() == 0);
		}
		assertTrue(rateLimiter.approvedCount() == 10);
		assertTrue(rateLimiter.dismissedCount() == 0);
	}

	@Test
	public void checkDismissedCounterForOneSlot() {
		rateLimiter = new RateLimiterImpl(0, slotLength, timeUnit);
		for (int i = 0; i < 10; i++) {
			assertFalse(rateLimiter.checkAndCount());
			assertTrue(rateLimiter.dismissedCount() == i + 1);
			assertTrue(rateLimiter.approvedCount() == 0);
		}
		assertTrue(rateLimiter.dismissedCount() == 10);
		assertTrue(rateLimiter.approvedCount() == 0);
	}

	@Test
	public void checkApprovedCounterForTwoSlots() throws InterruptedException {
		rateLimiter = new RateLimiterImpl(10, slotLength, timeUnit);
		long touch = System.currentTimeMillis();
		for (int i = 0; i < 10; i++) {
			assertTrue(rateLimiter.checkAndCount());
			assertTrue(rateLimiter.approvedCount() == i + 1);
			assertTrue(rateLimiter.dismissedCount() == 0);
		}
		assertFalse(rateLimiter.checkAndCount());
		assertTrue(rateLimiter.dismissedCount() == 1);
		//wait for next slot
		Thread.sleep(2000 - (System.currentTimeMillis() - touch));
		for (int i = 0; i < 10; i++) {
			assertTrue(rateLimiter.checkAndCount());
			assertTrue(rateLimiter.approvedCount() == i + 1);
			assertTrue(rateLimiter.dismissedCount() == 0);
		}
	}

	@Test
	public void checkThreadSafety() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(4);
		rateLimiter = new RateLimiterImpl(10, 2, timeUnit);
		List<Future> futures = new ArrayList<Future>();
		for (int i = 0; i < 10; i++) {
			futures.add(executorService.submit(new RateLimiterCheckApprovedRunnable(rateLimiter)));
		}
		for (int i = 0; i < 10; i++) {
			futures.add(executorService.submit(new RateLimiterCheckDismissedRunnable(rateLimiter)));
		}
		for (Future future : futures){
			Object obj = future.get();
			assertNull(obj);
		}
		assertEquals(rateLimiter.approvedCount(), 10);
		assertEquals(rateLimiter.dismissedCount(), 10);		
	}

	@Test
	public void tmp(){
		String msg = "text message";
		byte[] msgBytes = new byte[msg.getBytes().length + 2];
		System.arraycopy(msg.getBytes(), 0, msgBytes, 2, msg.getBytes().length);
		ByteBuffer buffer = ByteBuffer.wrap(msgBytes);
		buffer.put(0, (byte) 104);
	}
}
