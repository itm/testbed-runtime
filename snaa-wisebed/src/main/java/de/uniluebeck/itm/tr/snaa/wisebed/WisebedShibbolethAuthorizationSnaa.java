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

package de.uniluebeck.itm.tr.snaa.wisebed;

import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAA;
import eu.wisebed.shibboauth.ShibbolethAuthenticator;
import eu.wisebed.testbed.api.snaa.authorization.IUserAuthorization;
import eu.wisebed.testbed.api.snaa.authorization.IUserAuthorization.ActionDetails;
import eu.wisebed.testbed.api.snaa.v1.*;
import org.apache.http.client.CookieStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static eu.wisebed.testbed.api.snaa.helpers.Helper.createAuthenticationException;
import static eu.wisebed.testbed.api.snaa.helpers.Helper.createSNAAException;

public class WisebedShibbolethAuthorizationSnaa extends ShibbolethSNAA {
	private static final Logger log = LoggerFactory.getLogger(WisebedShibbolethAuthorizationSnaa.class);

	public WisebedShibbolethAuthorizationSnaa(Set<String> urnPrefixes, String secretAuthenticationKeyUrl,
			IUserAuthorization authorization) {
		super(urnPrefixes, secretAuthenticationKeyUrl, authorization);
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(List<AuthenticationTriple> authenticationData)
			throws AuthenticationExceptionException, SNAAExceptionException {
		throw createSNAAException("No authentication service provided here, use the federator.");
	}

	@Override
	public boolean isAuthorized(List<SecretAuthenticationKey> authenticationData, Action action)
			throws SNAAExceptionException {

		boolean authorized = true;

		// Check if we serve all URNs
		for (SecretAuthenticationKey key : authenticationData) {
			if (!urnPrefixes.contains(key.getUrnPrefix())) {
				throw createSNAAException("Not serving urn prefix " + key.getUrnPrefix());
			}
		}

		// Check if we have all SecretAuthenticationKeys as sessions
		for (SecretAuthenticationKey key : authenticationData) {
			ShibbolethAuthenticator sa = new ShibbolethAuthenticator();
			CookieStore cookieStore = sa.getCookieStore();
			// TODO: Set the cookie based on the SAK

			try {

				// Check if we are still authenticated
				if (!sa.isAuthenticated()) {
					throw createAuthenticationException("No valid session for " + key);
				}

				// Perform Authorization
				if (authorization != null) {
					ActionDetails details = new ActionDetails();
					details.setUsername(sa.getUsername());
					details.getUserDetails().put("shibboauth", sa);

					// TODO Add some real authorization code here
					// 1.) Fetch session data from e.g. https://wisebed1.itm.uni-luebeck.de/Shibboleth.sso/Session

					authorized = authorization.isAuthorized(action, details);
					log.debug("Authorization result[" + authorized + "] for action[" + action + "], authdata["
							+ details + "]");
				}

			} catch (Exception e) {
				throw createSNAAException("Unknown error while re-checking session for " + key);
			}

		}

		// log.debug("Done checking authorization, result: " + authorized);
		return authorized;
	}

}
