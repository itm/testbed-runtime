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

import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.tr.util.TimedCache;
import eu.wisebed.testbed.api.wsn.v211.RequestStatus;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncJobObserver {

	private static final Logger log = LoggerFactory.getLogger(AsyncJobObserver.class);

	private TimedCache<String, Job> jobList = new TimedCache<String, Job>(10, TimeUnit.MINUTES);

	/**
	 * If a request status arrives before a job is submitted it is cached here. If the job is then submitted it will be
	 * first checked if the request status for the job is contained here and, in this case, the job will be assumed to be
	 * completed.
	 */
	private TimedCache<String, RequestStatus> unknownRequestStatuses =
			new TimedCache<String, RequestStatus>(10, TimeUnit.MINUTES);

	private final int timeout;

	private final TimeUnit unit;

	private Set<JobResultListener> listeners = new HashSet<JobResultListener>();

	private Lock lock = new ReentrantLock();

	private Condition jobListEmpty = lock.newCondition();

	public AsyncJobObserver(int timeout, TimeUnit unit) {
		this.timeout = timeout;
		this.unit = unit;
	}

	public void submit(Job job) {

		log.debug("Submitted job with request Id {}", job.getRequestId());

		lock.lock();
		try {

			if (unknownRequestStatuses.containsKey(job.getRequestId())) {
				unknownRequestStatuses.remove(job.getRequestId());
			} else {

				jobList.put(job.getRequestId(), job);
				job.setStartTime(new DateTime());

				for (JobResultListener l : listeners) {
					job.addListener(l);
				}
			}

		} finally {
			lock.unlock();
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
				unknownRequestStatuses.put(status.getRequestId(), status);
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
				jobListEmpty.await(1, TimeUnit.SECONDS);
				log.debug("Waiting for " + jobList.size() + " unfinished jobs (Timeout: " + timeout + " "
						+ unit + ")"
				);
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
