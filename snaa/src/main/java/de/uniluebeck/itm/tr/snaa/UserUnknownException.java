package de.uniluebeck.itm.tr.snaa;

public class UserUnknownException extends Exception {

	private final String email;

	public UserUnknownException(final String email) {
		super("User with email \"" + email + "\" is unknown!");
		this.email = email;
	}
}
