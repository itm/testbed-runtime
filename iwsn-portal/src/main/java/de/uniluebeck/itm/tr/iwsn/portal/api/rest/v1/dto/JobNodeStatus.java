package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

public class JobNodeStatus {

	private int statusCode;

	private String message;

	private JobState status;

	public JobNodeStatus(final JobState status, final int statusCode, final String message) {
		this.message = message;
		this.status = status;
		this.statusCode = statusCode;
	}

	void setMessage(final String message) {
		this.message = message;
	}

	void setStatus(final JobState status) {
		this.status = status;
	}

	void setStatusCode(final int statusCode) {
		this.statusCode = statusCode;
	}

	public String getMessage() {
		return message;
	}

	public JobState getStatus() {
		return status;
	}

	public int getStatusCode() {
		return statusCode;
	}

	@Override
	public String toString() {
		return "JobNodeStatus{" +
				"status=" + status +
				", statusCode=" + statusCode +
				", message='" + message + '\'' +
				'}';
	}
}