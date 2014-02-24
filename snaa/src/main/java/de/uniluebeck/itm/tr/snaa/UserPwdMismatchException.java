package de.uniluebeck.itm.tr.snaa;

public class UserPwdMismatchException extends Exception {

	private final String email;

	public UserPwdMismatchException(final String email) {
		super("Passwords for user with email \"" + email + "\" don't match!");
		this.email = email;
	}
}
