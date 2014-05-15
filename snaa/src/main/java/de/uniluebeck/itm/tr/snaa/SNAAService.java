package de.uniluebeck.itm.tr.snaa;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.snaa.SNAA;

public interface SNAAService extends SNAA, Service {

	boolean isUserRegistrationSupported();

	void add(String email, String password) throws UserAlreadyExistsException;

	void update(String email, String oldPassword, String newPassword)
			throws UserUnknownException, UserPwdMismatchException;

	void delete(String email, String password) throws UserUnknownException, UserPwdMismatchException;

}
