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

package de.uniluebeck.itm.tr.snaa.jaas;

import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import de.uniluebeck.itm.tr.util.TimedCache;
import eu.wisebed.testbed.api.snaa.authorization.IUserAuthorization;
import eu.wisebed.testbed.api.snaa.authorization.IUserAuthorization.UserDetails;
import eu.wisebed.api.snaa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static eu.wisebed.testbed.api.snaa.helpers.Helper.*;

@WebService(endpointInterface = "eu.wisebed.api.snaa.SNAA", portName = "SNAAPort", serviceName = "SNAAService", targetNamespace = "http://testbed.wisebed.eu/api/snaa/v1/")
public class JAASSNAA implements SNAA {

	private static final Logger log = LoggerFactory.getLogger(JAASSNAA.class);

	private String urnPrefix;

	private String jaasLoginModuleName;

	private IUserAuthorization authorization;

	private SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	static class AuthData {

		String username;

		Subject subject;

		private AuthData(String username, Subject subject) {
			this.username = username;
			this.subject = subject;
		}

		@Override
		public String toString() {
			return "AuthData [subject=" + subject + ", username=" + username + "]";
		}

	}

	;

	/**
	 * SecretAuthenticationKey -> Username
	 */
	private TimedCache<String, AuthData> authenticatedSessions = new TimedCache<String, AuthData>(30, TimeUnit.MINUTES);

	public JAASSNAA(String urnPrefix, String jaasLoginModuleName, IUserAuthorization authorization) {
		this.urnPrefix = urnPrefix;
		this.jaasLoginModuleName = jaasLoginModuleName;
		this.authorization = authorization;
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(
			@WebParam(name = "authenticationData", targetNamespace = "") List<AuthenticationTriple> authenticationData)
			throws AuthenticationExceptionException, SNAAExceptionException {

		assertAuthenticationCount(authenticationData, 1, 1);
		assertUrnPrefixServed(urnPrefix, authenticationData);
		AuthenticationTriple authenticationTriple = authenticationData.get(0);

		try {
			String username = authenticationTriple.getUsername();
			String password = authenticationTriple.getPassword();

			log.debug("Login for user[{}] with module [{}]", username, jaasLoginModuleName);


			LoginContext lc = new LoginContext(jaasLoginModuleName, new CredentialsCallbackHandler(username, password));

			lc.login();

			String sak = secureIdGenerator.getNextId();

			authenticatedSessions.put(sak, new AuthData(username, lc.getSubject()));

			SecretAuthenticationKey secretAuthenticationKey = new SecretAuthenticationKey();
			secretAuthenticationKey.setSecretAuthenticationKey(sak);
			secretAuthenticationKey.setUrnPrefix(authenticationTriple.getUrnPrefix());
			secretAuthenticationKey.setUsername(username);

			List<SecretAuthenticationKey> secretAuthenticationKeyList = new ArrayList<SecretAuthenticationKey>(1);
			secretAuthenticationKeyList.add(secretAuthenticationKey);

			return secretAuthenticationKeyList;

		} catch (LoginException le) {
			log.debug("LoginException: " + le, le);
			throw createAuthenticationException("Authentication failed!");
		} catch (SecurityException se) {
			log.debug("SecurityException: " + se, se);
			throw createSNAAException("Internal Server Error");
		}

	}

	@Override
	public boolean isAuthorized(
			@WebParam(name = "authenticationData", targetNamespace = "") List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "action", targetNamespace = "") Action action) throws SNAAExceptionException {
		boolean authorized = false;

		// Check the supplied authentication keys
		assertAuthenticationKeyCount(authenticationData, 1, 1);
		SecretAuthenticationKey secretAuthenticationKey = authenticationData.get(0);
		assertSAKUrnPrefixServed(urnPrefix, authenticationData);

		// Get the session from the cache of authenticated sessions
		AuthData auth = authenticatedSessions.get(secretAuthenticationKey.getSecretAuthenticationKey());

		if (auth == null || auth.username == null || secretAuthenticationKey.getUsername() == null
				|| !secretAuthenticationKey.getUsername().equals(auth.username)) {
			log.warn("Authorization failed. Invalid parameters.");
			return false;
		}

		// Perform Authorization
		if (authorization != null) {
			IUserAuthorization.UserDetails details = new UserDetails();
			details.setUsername(secretAuthenticationKey.getUsername());
            List<Object> subjects = new LinkedList<Object>();
            subjects.add(auth.subject);
			details.getUserDetails().put("subject", subjects);
			authorized = authorization.isAuthorized(action, details);
			log.debug("Authorization result[" + authorized + "] for action[" + action + "], authdata[" + details + "]");
		}

		return authorized;
	}

}
