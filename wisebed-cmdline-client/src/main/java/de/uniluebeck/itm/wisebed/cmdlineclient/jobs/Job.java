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

import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.testbed.api.wsn.v211.RequestStatus;
import eu.wisebed.testbed.api.wsn.v211.Status;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Job {
	private static final Logger log = Logger.getLogger(Job.class);

	private final JobType jobType;

	private int successfulCount = 0;

	private int errorCount = 0;

	private DateTime startTime;

	private DateTime endTime;

	protected String requestId;

	protected HashSet<String> nodeIds;

	protected String description;

	protected JobResult results = null;

	private Set<JobResultListener> listeners = new HashSet<JobResultListener>();


	public enum JobType {
		areNodesAlive, resetNodes, send, flashPrograms, setVirtualLink, destroyVirtualLink
	}

	;

	public Job(String description, String requestId, List<String> nodeIds, JobType jobType) {
		this.description = description;
		this.requestId = requestId;
		this.nodeIds = new HashSet<String>(nodeIds);
		this.jobType = jobType;
		this.results = new JobResult(jobType);
	}

	public Job(String description, String requestId, String nodeIds, JobType jobType) {
		this.description = description;
		this.requestId = requestId;
		this.nodeIds = new HashSet<String>();
		this.nodeIds.add(nodeIds);
		this.jobType = jobType;
		this.results = new JobResult(jobType);
	}

	public String getRequestId() {
		return requestId;
	}

	private boolean isDone(int value) {

		if (jobType == JobType.areNodesAlive) {
			return value == 0 || value == 1;
		} else if (jobType == JobType.resetNodes) {
			return value == 1;
		} else if (jobType == JobType.send) {
			return value == 1;
		} else if (jobType == JobType.flashPrograms) {
			return value == 100;
		} else if (jobType == JobType.setVirtualLink) {
			return value == 1;
		} else if (jobType == JobType.destroyVirtualLink) {
			return value == 1;
		}

		return false;
	}

	private boolean isError(int value) {

		if (jobType == JobType.areNodesAlive) {
			return value < 0;
		} else if (jobType == JobType.resetNodes) {
			return value == 0 || value == -1;
		} else if (jobType == JobType.send) {
			return value == 0 || value == -1;
		} else if (jobType == JobType.flashPrograms) {
			return value < 0;
		} else if (jobType == JobType.setVirtualLink) {
			return value < 1;
		} else if (jobType == JobType.destroyVirtualLink) {
			return value < 1;
		}

		return false;
	}

	public boolean receive(RequestStatus status) {
		log.debug("Status update for job " + status.getRequestId() + ": " + StringUtils.jaxbMarshal(status));
		for (Status s : status.getStatus()) {
			boolean done = isDone(s.getValue());
			boolean error = isError(s.getValue());

			if (done || error) {

				boolean removed = false;
				synchronized (nodeIds) {
					removed = nodeIds.remove(s.getNodeId());
				}

				if (removed) {

					if (done) {
						successfulCount++;
						results.addResult(s.getNodeId(), done, "Done");

					} else if (error) {
						StringBuilder b = new StringBuilder();
						b.append("Job[");
						b.append(description);
						b.append("] failed for node ");
						b.append(s.getNodeId());
						b.append(" with message [");
						b.append(s.getMsg());
						b.append("]");
						log.info(b.toString());
						results.addResult(s.getNodeId(), error, b.toString());
					}
				} else {
					log.warn("Received status for unknown node " + s.getNodeId() + ": " + StringUtils.jaxbMarshal(s));
				}
			}

		}

		if (nodeIds.size() == 0) {
			log.debug(successfulCount + " nodeIds done, " + errorCount + " failed for " + this.jobType + " ["
					+ description + "] request id: " + requestId
			);

			results.setStartTime(startTime);
			results.setEndTime(endTime);
			results.setDescription(description);
			notifyListeners(results);
			return true;
		}

		return false;
	}

	public void setStartTime(DateTime startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(DateTime endTime) {
		this.endTime = endTime;
	}

	public DateTime getStartTime() {
		return startTime;
	}

	public DateTime getEndTime() {
		return endTime;
	}

	public void addListener(JobResultListener listener) {
		listeners.add(listener);
	}

	public void removeListener(JobResultListener listener) {
		listeners.remove(listener);
	}

	private void notifyListeners(JobResult result) {
		for (JobResultListener l : listeners)
			l.receiveJobResult(result);

	}

}
