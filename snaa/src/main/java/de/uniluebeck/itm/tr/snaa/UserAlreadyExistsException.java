package de.uniluebeck.itm.tr.snaa;

public class UserAlreadyExistsException extends Exception {

	private final String email;

	public UserAlreadyExistsException(final String email) {
		super("A user with email \"" + email + "\" already exists!");
		this.email = email;
	}
}
