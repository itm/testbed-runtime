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
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.*;
import eu.wisebed.testbed.api.snaa.authorization.IUserAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.snaa.SNAAHelper.*;

@WebService(
		name = "SNAA",
		endpointInterface = "eu.wisebed.api.v3.snaa.SNAA",
		portName = "SNAAPort",
		serviceName = "SNAAService",
		targetNamespace = "http://wisebed.eu/api/v3/snaa"
)
public class JAASSNAA implements SNAA {

	private static final Logger log = LoggerFactory.getLogger(JAASSNAA.class);

	private NodeUrnPrefix urnPrefix;

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

	/**
	 * SecretAuthenticationKey -> Username
	 */
	private TimedCache<String, AuthData> authenticatedSessions = new TimedCache<String, AuthData>(30, TimeUnit.MINUTES);

	public JAASSNAA(NodeUrnPrefix urnPrefix, String jaasLoginModuleName, IUserAuthorization authorization) {
		this.urnPrefix = urnPrefix;
		this.jaasLoginModuleName = jaasLoginModuleName;
		this.authorization = authorization;
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(final List<AuthenticationTriple> authenticationData)
			throws AuthenticationFault_Exception, SNAAFault_Exception {

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
			secretAuthenticationKey.setKey(sak);
			secretAuthenticationKey.setUrnPrefix(authenticationTriple.getUrnPrefix());
			secretAuthenticationKey.setUsername(username);

			List<SecretAuthenticationKey> secretAuthenticationKeyList = new ArrayList<SecretAuthenticationKey>(1);
			secretAuthenticationKeyList.add(secretAuthenticationKey);

			return secretAuthenticationKeyList;

		} catch (LoginException le) {
			log.debug("LoginException: " + le, le);
			throw createAuthenticationFault_Exception("Authentication failed!");
		} catch (SecurityException se) {
			log.debug("SecurityException: " + se, se);
			throw createSNAAFault("Internal Server Error");
		}

	}

	@Override
	public AuthorizationResponse isAuthorized(final List<UsernameNodeUrnsMap> usernameNodeUrnsMapList,
											  final Action action) throws SNAAFault_Exception {


		AuthorizationResponse authorized = new AuthorizationResponse();

		authorized.setAuthorized(true);
		authorized.setMessage("JAASSNAA is used for authentication only and always return 'true'");

		for (UsernameNodeUrnsMap usernameNodeUrnsMap : usernameNodeUrnsMapList) {
			for (NodeUrn nodeUrn : usernameNodeUrnsMap.getNodeUrns()) {
				PerNodeUrnAuthorizationResponse perNodeUrnAuthorizationResponse = new PerNodeUrnAuthorizationResponse();
				perNodeUrnAuthorizationResponse.setNodeUrn(nodeUrn);
				perNodeUrnAuthorizationResponse.setAuthorized(true);
				authorized.getPerNodeUrnAuthorizationResponses().add(perNodeUrnAuthorizationResponse);
			}
		}

		return authorized;
	}

	@Override
	public List<ValidationResult> isValid(final List<SecretAuthenticationKey> secretAuthenticationKeys)
			throws SNAAFault_Exception {

		// Check the supplied authentication keys
		assertSAKUrnPrefixServed(urnPrefix, secretAuthenticationKeys);

		final SecretAuthenticationKey secretAuthenticationKey = secretAuthenticationKeys.get(0);

		// Get the session from the cache of authenticated sessions
		AuthData auth = authenticatedSessions.get(secretAuthenticationKey.getKey());

		ValidationResult result = new ValidationResult();

		if (auth == null) {
			result.setValid(false);
			result.setMessage("The provides secret authentication key is not found. It is either invalid or expired.");
		} else if (secretAuthenticationKey.getUsername() == null) {
			result.setValid(false);
			result.setMessage("The user name comprised in the secret authentication key must not be 'null'.");
		} else if (auth.username == null) {
			result.setValid(false);
			result.setMessage("The user name which was provided by the original authentication is not known.");
		} else if (!secretAuthenticationKey.getUsername().equals(auth.username)) {
			result.setValid(false);
			result.setMessage(
					"The user name which was provided by the original authentication does not match the one in the secret authentication key."
			);
		} else {
			result.setValid(true);
		}

		return newArrayList(result);
	}

}
