package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.util.concurrent.ExecutorUtils;
import de.uniluebeck.itm.util.concurrent.ForwardingScheduledExecutorService;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

@SuppressWarnings("NullableProblems")
class SchedulerServiceImpl extends AbstractService implements SchedulerService {

	private ScheduledExecutorService scheduler;

	private final int workerThreads;

	private final String schedulerThreadNameFormat;

	private final String workerThreadNameFormat;

	@Inject
	SchedulerServiceImpl(@Assisted final int workerThreads,
						 @Assisted final String threadNamePrefix) {
		this.workerThreads = workerThreads;
		this.schedulerThreadNameFormat = threadNamePrefix + "-Scheduler";
		this.workerThreadNameFormat = threadNamePrefix + "-Worker %d";
	}

	@Override
	protected void doStart() {

		try {
			final ThreadFactory schedulerThreadFactory =
					new ThreadFactoryBuilder().setNameFormat(schedulerThreadNameFormat).build();

			final ThreadFactory workerThreadFactory =
					new ThreadFactoryBuilder().setNameFormat(workerThreadNameFormat).build();

			this.scheduler = new ForwardingScheduledExecutorService(
					Executors.newScheduledThreadPool(1, schedulerThreadFactory),
					(workerThreads == -1 ?
							Executors.newCachedThreadPool(workerThreadFactory) :
							Executors.newFixedThreadPool(workerThreads, workerThreadFactory))
			);

			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			ExecutorUtils.shutdown(scheduler, 1, TimeUnit.SECONDS);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay,
										   final TimeUnit unit) {
		return scheduler.schedule(callable, delay, unit);
	}

	@Override
	public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
		return scheduler.schedule(command, delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
												  final TimeUnit unit) {
		return scheduler.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
													 final TimeUnit unit) {
		return scheduler.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

	@Override
	public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
		return scheduler.awaitTermination(timeout, unit);
	}

	@Override
	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return scheduler.invokeAll(tasks);
	}

	@Override
	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
										 final long timeout, final TimeUnit unit) throws InterruptedException {
		return scheduler.invokeAll(tasks, timeout, unit);
	}

	@Override
	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return scheduler.invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout,
						   final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return scheduler.invokeAny(tasks, timeout, unit);
	}

	@Override
	public boolean isShutdown() {
		return scheduler.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return scheduler.isTerminated();
	}

	@Override
	public void shutdown() {
		scheduler.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return scheduler.shutdownNow();
	}

	@Override
	public <T> Future<T> submit(final Callable<T> task) {
		return scheduler.submit(task);
	}

	@Override
	public Future<?> submit(final Runnable task) {
		return scheduler.submit(task);
	}

	@Override
	public <T> Future<T> submit(final Runnable task, final T result) {
		return scheduler.submit(task, result);
	}

	@Override
	public void execute(final Runnable command) {
		scheduler.execute(command);
	}
}
