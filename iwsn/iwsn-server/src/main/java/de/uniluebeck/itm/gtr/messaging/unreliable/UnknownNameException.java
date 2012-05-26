package de.uniluebeck.itm.gtr.messaging.unreliable;


public class UnknownNameException extends RuntimeException {

	private String name;

	public UnknownNameException(final String name) {
		this.name = name;
	}

	public UnknownNameException(final String name, final String message) {
		super(message);
		this.name = name;
	}

	public UnknownNameException(final String name, final String message, final Throwable cause) {
		super(message, cause);
		this.name = name;
	}

	public UnknownNameException(final String name, final Throwable cause) {
		super(cause);
		this.name = name;
	}

}
