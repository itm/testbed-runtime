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
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;

public class Job {

	private static final Logger log = LoggerFactory.getLogger(Job.class);

	private final JobType jobType;

	private int successfulCount = 0;

	private int errorCount = 0;

	private DateTime startTime;

	private DateTime endTime;

	protected String requestId;

	protected final HashSet<String> nodeIds;

	protected String description;

	protected JobResult results = null;

	private Set<JobResultListener> listeners = new HashSet<JobResultListener>();

	public enum JobType {

		areNodesAlive,
		resetNodes,
		send,
		flashPrograms,
		setVirtualLink,
		destroyVirtualLink,
		disableNode,
		enableNode,
		disablePhysicalLink,
		enablePhysicalLink

	}

	public Job(String description, String requestId, List<String> nodeIds, JobType jobType) {
		this.description = description;
		this.requestId = requestId;
		this.nodeIds = Sets.newHashSet(nodeIds);
		this.jobType = jobType;
		this.results = new JobResult(jobType);
	}

	public Job(String description, String requestId, String nodeId, JobType jobType) {
		this.description = description;
		this.requestId = requestId;
		this.nodeIds = Sets.newHashSet(nodeId);
		this.jobType = jobType;
		this.results = new JobResult(jobType);
	}

	public String getRequestId() {
		return requestId;
	}

	public void timeout() {
		for (JobResultListener l : listeners) {
			l.timeout();
		}
	}

	private boolean isDone(int value) {

		if (jobType == JobType.areNodesAlive) {
			return value == 1;
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
		} else if (jobType == JobType.disableNode) {
			return value == 1;
		} else if (jobType == JobType.enableNode) {
			return value == 1;
		} else if (jobType == JobType.disablePhysicalLink) {
			return value == 1;
		} else if (jobType == JobType.enablePhysicalLink) {
			return value == 1;
		}

		return false;
	}

	private boolean isError(int value) {

		if (jobType == JobType.areNodesAlive) {
			return value <= 0;
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
		} else if (jobType == JobType.disableNode) {
			return value < 1;
		} else if (jobType == JobType.enableNode) {
			return value < 1;
		} else if (jobType == JobType.disablePhysicalLink) {
			return value < 1;
		} else if (jobType == JobType.enablePhysicalLink) {
			return value < 1;
		}

		return false;
	}

	public boolean receive(RequestStatus status) {

		for (Status s : status.getStatus()) {

			boolean done = isDone(s.getValue());
			boolean error = isError(s.getValue());

			log.debug(
					"Status update from node {} for {} job with request ID {}: {}.",
					new Object[]{
							s.getNodeId(),
							jobType,
							status.getRequestId(),
							done ? s.getValue() + " (done)" : error ? s.getValue() + " (error)" : s.getValue()
					}
			);

			if (done || error) {

				boolean removed;

				synchronized (nodeIds) {
					removed = nodeIds.remove(s.getNodeId());
				}

				if (removed) {

					if (done) {

						successfulCount++;
						results.addResult(s.getNodeId(), done, s.getMsg());

					} else if (error) {

						if (log.isInfoEnabled()) {
							log.info("Job[{}] failed for node {} with message [{}]", new Object[] {
								description,
								s.getNodeId(),
								s.getMsg()
							});
						}

						results.addResult(s.getNodeId(), !error, s.getMsg());

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
		for (JobResultListener l : listeners) {
			l.receiveJobResult(result);
		}

	}

	public JobType getJobType() {
		return jobType;
	}

	public String getDescription() {
		return description;
	}

	public HashSet<String> getNodeIds() {
		return nodeIds;
	}
}
