package de.uniluebeck.itm.tr.iwsn.common;

public interface SchedulerServiceFactory {

	/**
	 * Creates a new SchedulerService.
	 *
	 * @param workerThreads
	 * 		the number of worker threads or {@code -1} (unlimited)
	 * @param schedulerThreadNameFormat
	 * 		the name format for the scheduler thread (cf. {@link com.google.common.util.concurrent.ThreadFactoryBuilder}
	 * @param workerThreadNameFormat
	 * 		the name format for the worker threads (cf. {@link com.google.common.util.concurrent.ThreadFactoryBuilder}
	 *
	 * @return a new SchedulerService instance
	 */
	SchedulerService create(final int workerThreads,
							final String schedulerThreadNameFormat,
							final String workerThreadNameFormat);

}
