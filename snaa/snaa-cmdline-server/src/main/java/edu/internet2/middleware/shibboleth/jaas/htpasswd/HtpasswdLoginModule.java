/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.jaas.htpasswd;

import de.uniluebeck.itm.tr.snaa.jaas.NamedPrincipal;
import edu.vt.middleware.crypt.CryptException;
import edu.vt.middleware.crypt.digest.SHA1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Map;

/**
 * JAAS login module that reads an Apache HTTPD htpasswd formatted file for its authentication information.
 * <p/>
 * This login module requires an option called "htpasswdFile" which is the location of the htpasswd file that will be
 * used. The provided login handler must also be an instance of {@link NameCallback} and {@link PasswordCallback}.
 * <p/>
 * The password is not stored within the Subject's credential sets.
 */
public class HtpasswdLoginModule implements LoginModule {

	/**
	 * Name of the JAAS login module option that gives the htpasswd file location.
	 */
	public final static String HTPASSWD_FILE_OPTION = "htpasswdFile";

	/**
	 * Class logger.
	 */
	private final Logger log = LoggerFactory.getLogger(HtpasswdLoginModule.class);

	/**
	 * Subject to populate with information.
	 */
	private Subject subject;

	/**
	 * Callback handler used to collect authentication material.
	 */
	private CallbackHandler callbackHandler;

	/**
	 * htpasswd file location.
	 */
	private String passwordFilePath;

	/**
	 * Principal created for the user.
	 */
	private Principal principal;

	/**
	 * {@inheritDoc}
	 */
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
						   Map<String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;

		passwordFilePath = (String) options.get(HTPASSWD_FILE_OPTION);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean login() throws LoginException {
		NameCallback nameCb = new NameCallback("Enter user name: ");
		PasswordCallback passCb = new PasswordCallback("Enter password: ", false);
		principal = null;

		try {
			if (callbackHandler != null) {
				callbackHandler.handle(new Callback[]{nameCb, passCb});
			}
		} catch (IOException e) {
			log.error("Unable to process callbacks", e);
			throw new LoginException(e.toString());
		} catch (UnsupportedCallbackException e) {
			log.error("Unsupported callbacks", e);
			throw new LoginException(e.toString());
		}

		boolean authenticationValid = authenticate(nameCb.getName(), passCb.getPassword());
		log.debug("Login " + (authenticationValid ? "" : "in") + "valid for username " + nameCb.getName());

		if (authenticationValid)
			principal = new NamedPrincipal(nameCb.getName());

		return authenticationValid;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean commit() throws LoginException {
		log.debug("Committing login transaction");
		subject.getPrincipals().add(principal);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean logout() throws LoginException {
		subject.getPrincipals().remove(principal);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean abort() throws LoginException {
		subject.getPrincipals().remove(principal);
		return true;
	}

	/**
	 * Authenticates the user against the htpasswd file.
	 *
	 * @param user	 user to authentication
	 * @param password user's password
	 * @return true if the user is authenticated
	 * @throws LoginException thrown if the user is not authenticated
	 */
	protected boolean authenticate(String user, char[] password) throws LoginException {
		log.debug("Attempting to authenticate {} against htpasswd file {}", user, passwordFilePath);
		BufferedReader htpasswdReader = null;
		try {
			htpasswdReader = new BufferedReader(new FileReader(passwordFilePath));
			String[] entryParts;

			for (String passwordEntry = htpasswdReader.readLine(); passwordEntry != null; passwordEntry = htpasswdReader
					.readLine()) {
				log.debug("Looking at line: " + passwordEntry);

				entryParts = passwordEntry.split(":");

				log.debug("Split line: " + Arrays.toString(entryParts));

				String htpasswdUsername = entryParts[0];
				String htpasswdPassword = entryParts[1];

				if (htpasswdUsername.equals(user)) {
					log.debug("Found line for user: {}", user);

					if (htpasswdPassword.startsWith("{SHA}")) {
						log.debug("Validating SHA-1 password for user {}", user);
						return validateSHA1Password(password, htpasswdPassword);

					} else if (htpasswdPassword.matches("^\\$.*\\$.*$")) {
						log.debug("Validating MD5 password for user {}", user);
						return validateMD5Password(password, htpasswdPassword);

					} else
						throw new LoginException("Unsupported entry type, use htpasswd with the -s for SHA option");

				}
			}

			log.debug("Unable to authenticate user {}", user);
			throw new LoginException("Unable to authenticate user");
		} catch (IOException e) {
			log.error("Unable to read password file", e);
			throw new LoginException(e.getMessage());
		} finally {
			try {
				htpasswdReader.close();
			} catch (IOException e) {

			}
		}
	}

	/**
	 * Checks if the given password matches the MD5 hashed valid password.
	 *
	 * @param givenPassword given password
	 * @param validPassword MD5 hashed valid password
	 * @return true if the password is valid
	 * @throws LoginException thrown if the given password does not match the valid password
	 */
	protected boolean validateMD5Password(char[] givenPassword, String validPassword) throws LoginException {
		throw new LoginException("MD5 passwords are not implemented");
	}

	/**
	 * Checks if the given password matches the SHA1 hashed valid password.
	 *
	 * @param givenPassword given password
	 * @param validPassword MD5 hashed valid password
	 * @return true if the password is valid
	 * @throws LoginException thrown if the given password does not match the valid password
	 */
	protected boolean validateSHA1Password(char[] givenPassword, String validPassword) throws LoginException {
		validPassword = validPassword.replace("{SHA}", "");

		try {

			SHA1 passwordDigester = new SHA1(givenPassword);
			String computedDigest = passwordDigester.getBase64String();
			boolean valid = validPassword.equals(computedDigest);

			log.debug("SHA validation, given hash   : " + validPassword);
			log.debug("SHA validation, computed hash: " + validPassword);
			log.debug("SHA validation, match        : " + valid);

			return valid;

		} catch (CryptException e) {
			log.warn("Unable to perform SHA validation: " + e, e);
			throw new LoginException(e.getMessage());
		}
	}
}