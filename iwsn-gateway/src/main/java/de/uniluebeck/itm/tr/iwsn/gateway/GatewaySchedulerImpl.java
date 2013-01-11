package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

class GatewaySchedulerImpl implements GatewayScheduler {

	private final ScheduledExecutorService scheduledExecutorService;

	@Inject
	public GatewaySchedulerImpl(final ScheduledExecutorService scheduledExecutorService) {
		this.scheduledExecutorService = scheduledExecutorService;
	}

	public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay,
										   final TimeUnit unit) {
		return scheduledExecutorService.schedule(callable, delay, unit);
	}

	public Future<?> submit(final Runnable task) {
		return scheduledExecutorService.submit(task);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
												  final TimeUnit unit) {
		return scheduledExecutorService.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	public boolean isTerminated() {
		return scheduledExecutorService.isTerminated();
	}

	public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
		return scheduledExecutorService.schedule(command, delay, unit);
	}

	public List<Runnable> shutdownNow() {
		return scheduledExecutorService.shutdownNow();
	}

	public <T> Future<T> submit(final Runnable task, final T result) {
		return scheduledExecutorService.submit(task, result);
	}

	public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
		return scheduledExecutorService.awaitTermination(timeout, unit);
	}

	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout,
						   final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return scheduledExecutorService.invokeAny(tasks, timeout, unit);
	}

	public boolean isShutdown() {
		return scheduledExecutorService.isShutdown();
	}

	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
										 final long timeout, final TimeUnit unit) throws InterruptedException {
		return scheduledExecutorService.invokeAll(tasks, timeout, unit);
	}

	public void execute(final Runnable command) {
		scheduledExecutorService.execute(command);
	}

	public <T> Future<T> submit(final Callable<T> task) {
		return scheduledExecutorService.submit(task);
	}

	public void shutdown() {
		scheduledExecutorService.shutdown();
	}

	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return scheduledExecutorService.invokeAll(tasks);
	}

	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return scheduledExecutorService.invokeAny(tasks);
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
													 final TimeUnit unit) {
		return scheduledExecutorService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}
}
