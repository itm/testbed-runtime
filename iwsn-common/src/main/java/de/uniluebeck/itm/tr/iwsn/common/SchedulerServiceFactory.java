package de.uniluebeck.itm.tr.iwsn.common;

public interface SchedulerServiceFactory {

	/**
	 * Creates a new SchedulerService.
	 *
	 * @param workerThreads
	 * 		the number of worker threads or {@code -1} (unlimited)
	 * @param threadNamePrefix
	 * 		the prefix of the name for the scheduler thread (cf. {@link com.google.common.util.concurrent.ThreadFactoryBuilder})
	 *
	 * @return a new SchedulerService instance
	 */
	SchedulerService create(final int workerThreads, final String threadNamePrefix);

}
