package de.uniluebeck.itm.tr.runtime.stats;

import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RuntimeStatsApplication implements TestbedApplication {

	private static final Logger log = LoggerFactory.getLogger(RuntimeStatsApplication.class);

	public static class MovingAverage {

		private final Queue<Double> window = new LinkedList<Double>();

		private final int period;

		private double sum;

		public MovingAverage(int period) {
			this.period = period;
		}

		public void newNum(double num) {
			sum += num;
			window.add(num);
			if (window.size() > period) {
				sum -= window.remove();
			}
		}

		public double getAvg() {
			if (window.isEmpty()) return 0;
			return sum / window.size();
		}

	}

	private final MovingAverage messagesSentMovingAverage10Seconds = new MovingAverage(10);

	private final MovingAverage messagesSentMovingAverage60Seconds = new MovingAverage(60);

	private final MovingAverage messagesSentMovingAverage300Seconds = new MovingAverage(300);

	private final MovingAverage messagesDroppedMovingAverage10Seconds = new MovingAverage(10);

	private final MovingAverage messagesDroppedMovingAverage60Seconds = new MovingAverage(60);

	private final MovingAverage messagesDroppedMovingAverage300Seconds = new MovingAverage(300);

	private final MovingAverage messagesReceivedMovingAverage10Seconds = new MovingAverage(10);

	private final MovingAverage messagesReceivedMovingAverage60Seconds = new MovingAverage(60);

	private final MovingAverage messagesReceivedMovingAverage300Seconds = new MovingAverage(300);

	private final TestbedRuntime testbedRuntime;

	private volatile double messagesSentLastSecond = 0;

	private volatile double messagesDroppedLastSecond = 0;

	private volatile double messagesReceivedLastSecond = 0;

	private ScheduledFuture<?> calculatingScheduledFuture;

	private ScheduledFuture<?> loggingScheduledFuture;

	private MessageEventListener messageEventListener = new MessageEventListener() {
		@Override
		public void messageSent(Messages.Msg msg) {
			messagesSentLastSecond++;
		}

		@Override
		public void messageDropped(Messages.Msg msg) {
			messagesDroppedLastSecond++;
		}

		@Override
		public void messageReceived(Messages.Msg msg) {
			messagesReceivedLastSecond++;
		}
	};

	public RuntimeStatsApplication(TestbedRuntime testbedRuntime, String applicationName, Object configuration) {
		this.testbedRuntime = testbedRuntime;
	}

	@Override
	public String getName() {
		return RuntimeStatsApplication.class.getSimpleName();
	}

	@Override
	public void start() throws Exception {

		testbedRuntime.getMessageEventService().addListener(messageEventListener);

		calculatingScheduledFuture = testbedRuntime.getSchedulerService().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {

				double sent = messagesSentLastSecond;
				messagesSentLastSecond = 0;

				double dropped = messagesDroppedLastSecond;
				messagesDroppedLastSecond = 0;

				double received = messagesReceivedLastSecond;
				messagesReceivedLastSecond = 0;

				messagesSentMovingAverage10Seconds.newNum(sent);
				messagesSentMovingAverage60Seconds.newNum(sent);
				messagesSentMovingAverage300Seconds.newNum(sent);

				messagesDroppedMovingAverage10Seconds.newNum(dropped);
				messagesDroppedMovingAverage60Seconds.newNum(dropped);
				messagesDroppedMovingAverage300Seconds.newNum(dropped);

				messagesReceivedMovingAverage10Seconds.newNum(received);
				messagesReceivedMovingAverage60Seconds.newNum(received);
				messagesReceivedMovingAverage300Seconds.newNum(received);

			}
		}, 1, 1, TimeUnit.SECONDS);

		loggingScheduledFuture = testbedRuntime.getSchedulerService().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				log.debug("### Avg sent: {} {} {}", new Object[] {
						messagesSentMovingAverage10Seconds.getAvg(),
						messagesSentMovingAverage60Seconds.getAvg(),
						messagesSentMovingAverage300Seconds.getAvg()
				});
				log.debug("### Avg dropped: {} {} {}", new Object[] {
						messagesDroppedMovingAverage10Seconds.getAvg(),
						messagesDroppedMovingAverage60Seconds.getAvg(),
						messagesDroppedMovingAverage300Seconds.getAvg()
				});
				log.debug("### Avg received: {} {} {}", new Object[] {
						messagesReceivedMovingAverage10Seconds.getAvg(),
						messagesReceivedMovingAverage60Seconds.getAvg(),
						messagesReceivedMovingAverage300Seconds.getAvg()
				});
			}
		}, 10, 10, TimeUnit.SECONDS);
	}

	@Override
	public void stop() throws Exception {
		loggingScheduledFuture.cancel(true);
		calculatingScheduledFuture.cancel(true);
		testbedRuntime.getMessageEventService().removeListener(messageEventListener);
	}
}
