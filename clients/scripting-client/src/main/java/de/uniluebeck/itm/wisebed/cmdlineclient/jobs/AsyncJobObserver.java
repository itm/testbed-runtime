/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.wisebed.cmdlineclient.jobs;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.tr.util.TimedCache;
import de.uniluebeck.itm.tr.util.TimedCacheListener;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.controller.RequestStatus;

public class AsyncJobObserver {

	private static final Logger log = LoggerFactory.getLogger(AsyncJobObserver.class);

	private TimedCache<String, Job> jobList = new TimedCache<String, Job>(10, TimeUnit.MINUTES);

	/**
	 * If a request status arrives before a job is submitted it is cached here. If the job is then submitted it will be
	 * first checked if the request status for the job is contained here and, in this case, the job will be assumed to be
	 * completed.
	 */
	private TimedCache<String, List<RequestStatus>> unknownRequestStatuses =
			new TimedCache<String, List<RequestStatus>>(10, TimeUnit.MINUTES);

	private final int timeout;

	private final TimeUnit unit;

	private Set<JobResultListener> listeners = new HashSet<JobResultListener>();

	private Lock lock = new ReentrantLock();

	private Condition jobListEmpty = lock.newCondition();



	public AsyncJobObserver(int timeout, TimeUnit unit) {
		this.timeout = timeout;
		this.unit = unit;
		jobList.setListener(new TimedCacheListener<String, Job>() {
			@Override
			public Tuple<Long, TimeUnit> timeout(final String key, final Job value) {
				value.timeout();
				return null;
			}
		});
	}

	public void submit(Job job) {
		submit(job, timeout, unit);
	}

	public void submit(Job job, int timeout, TimeUnit timeUnit) {

		log.debug("Submitted job with request Id {}", job.getRequestId());

		lock.lock();
		try {

			jobList.put(job.getRequestId(), job, timeout, timeUnit);
			job.setStartTime(new DateTime());

			for (JobResultListener l : listeners) {
				job.addListener(l);
			}

			List<RequestStatus> unknownRequestStatusList = unknownRequestStatuses.get(job.getRequestId());

			if (unknownRequestStatusList != null) {
				log.trace("Found cached unknown request statuses");
				unknownRequestStatuses.remove(job.getRequestId());
				for (RequestStatus requestStatus : unknownRequestStatusList) {
					receive(requestStatus);
				}
			}

		} finally {
			lock.unlock();
		}

	}

	public void receive(List<RequestStatus> status) {
		for (RequestStatus s : status) {
			receive(s);
		}
	}

	public void receive(RequestStatus status) {

		lock.lock();

		try {

			Job job = jobList.get(status.getRequestId());

			if (job != null) {
				if (job.receive(status)) {
					jobList.remove(status.getRequestId());

					if (jobList.size() == 0) {
						jobListEmpty.signalAll();
					}

				}
			} else {
				log.trace("Unkown request status received");
				List<RequestStatus> statusList = unknownRequestStatuses.get(status.getRequestId());
				if (statusList == null) {
					statusList = new LinkedList<RequestStatus>();
					unknownRequestStatuses.put(status.getRequestId(), statusList);
				}
				statusList.add(status);
			}

		} finally {
			lock.unlock();
		}

	}

	public int getPendingJobsCount() {
		return jobList.size();
	}

	public void clear() {

		lock.lock();

		try {

			if (jobList.size() > 0) {
				log.warn("Removing " + jobList.size() + " unfinished jobs from the queue");
			}
			jobList.clear();
			jobListEmpty.signal();

		} finally {
			lock.unlock();
		}
	}

	public void join() {
		join(timeout, unit);
	}

	public void join(long timeoutMillis) {
		join(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	public void join(long timeout, TimeUnit unit) {

		TimeDiff diff = new TimeDiff(TimeUnit.MILLISECONDS.convert(timeout, unit));

		lock.lock();

		try {

			while (jobList.size() > 0 && !diff.isTimeout()) {
				jobListEmpty.await(3, TimeUnit.SECONDS);
				if (log.isDebugEnabled()) {
					log.debug("Waiting for {} unfinished jobs (Timeout: {} {}):", new Object[] {jobList.size(), timeout, unit});
					for (Job job : jobList.values()) {
						log.debug("\t {} with request ID {}: {}", new Object[] {job.getJobType(), job.getRequestId(), job.getNodeIds()});
					}
				}

			}

			// discard unfinished jobs after timeout
			clear();

		} catch (InterruptedException e) {
			log.error("" + e, e);
		} finally {
			lock.unlock();
		}
	}

	public void addListener(JobResultListener listener) {
		listeners.add(listener);
	}

	public void removeListener(JobResultListener listener) {
		listeners.remove(listener);
	}
}
