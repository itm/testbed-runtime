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

	public String getMessage() {
		return message;
	}

	void setMessage(final String message) {
		this.message = message;
	}

	public JobState getStatus() {
		return status;
	}

	void setStatus(final JobState status) {
		this.status = status;
	}

	public int getStatusCode() {
		return statusCode;
	}

	void setStatusCode(final int statusCode) {
		this.statusCode = statusCode;
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