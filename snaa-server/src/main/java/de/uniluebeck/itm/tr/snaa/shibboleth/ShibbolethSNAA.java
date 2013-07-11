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

package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.util.TimedCache;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.snaa.common.SNAAHelper.*;
import static de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethAuthenticatorSerialization.deserialize;
import static de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethAuthenticatorSerialization.serialize;

@WebService(
		name = "SNAA",
		endpointInterface = "eu.wisebed.api.v3.snaa.SNAA",
		portName = "SNAAPort",
		serviceName = "SNAAService",
		targetNamespace = "http://wisebed.eu/api/v3/snaa"
)
public class ShibbolethSNAA extends AbstractService implements de.uniluebeck.itm.tr.snaa.SNAAService {

	private static final Logger log = LoggerFactory.getLogger(ShibbolethSNAA.class);

	protected final ServicePublisher servicePublisher;

	protected final ServedNodeUrnPrefixesProvider urnPrefixes;

	protected final ShibbolethAuthorization authorization;

	protected final Provider<ShibbolethAuthenticator> authenticatorProvider;

	protected final TimedCache<SecretAuthenticationKey, List<Cookie>> cookieCache =
			new TimedCache<SecretAuthenticationKey, List<Cookie>>();

	private final SNAAServiceConfig snaaServiceConfig;

	private ServicePublisherService jaxWsService;

	@Inject
	public ShibbolethSNAA(final SNAAServiceConfig snaaServiceConfig,
						  final ServicePublisher servicePublisher,
						  final ServedNodeUrnPrefixesProvider urnPrefixes,
						  final ShibbolethAuthorization authorization,
						  final Provider<ShibbolethAuthenticator> authenticatorProvider) {
		this.snaaServiceConfig = checkNotNull(snaaServiceConfig);
		this.servicePublisher = checkNotNull(servicePublisher);
		this.urnPrefixes = checkNotNull(urnPrefixes);
		this.authorization = checkNotNull(authorization);
		this.authenticatorProvider = checkNotNull(authenticatorProvider);
	}

	@Override
	protected void doStart() {
		try {
			jaxWsService = servicePublisher.createJaxWsService(snaaServiceConfig.getSnaaContextPath(), this);
			jaxWsService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			if (jaxWsService != null) {
				jaxWsService.stopAndWait();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(final List<AuthenticationTriple> authenticationData)
			throws AuthenticationFault, SNAAFault_Exception {

		HashSet<SecretAuthenticationKey> keys = new HashSet<SecretAuthenticationKey>();
		log.debug("Starting for " + authenticationData.size() + " urns.");

		assertMinAuthenticationCount(authenticationData, 1);
		assertAllUrnPrefixesServed(urnPrefixes.get(), authenticationData);

		for (AuthenticationTriple triple : authenticationData) {

			NodeUrnPrefix urn = triple.getUrnPrefix();

			try {

				final String userAtIdpDomain = triple.getUsername();
				final String password = triple.getPassword();
				final ShibbolethAuthenticator authenticator = authenticatorProvider.get();
				authenticator.setUserAtIdpDomain(userAtIdpDomain);
				authenticator.setPassword(password);

				try {

					authenticator.authenticate();

				} catch (ClientProtocolException e) {

					// catch this exception as it usually means that the shibboleth server had a problem
					// wait a short amount of time and try again
					Thread.sleep(1000);
					try {
						authenticator.authenticate();
					} catch (Exception e1) {
						throw createSNAAFault("Authentication failed: the authentication system has problems "
								+ "contacting the Shibboleth server. Please try again later!"
						);
					}
				}

				if (authenticator.isAuthenticated()) {

					DateTime youngestCookieExpiration = null;

					final List<Cookie> cookies = authenticator.getCookieStore().getCookies();
					for (Cookie cookie : cookies) {
						final DateTime cookieExpiration = new DateTime(cookie.getExpiryDate());
						if (youngestCookieExpiration == null || cookieExpiration.isBefore(youngestCookieExpiration)) {
							youngestCookieExpiration = cookieExpiration;
						}
					}


					SecretAuthenticationKey secretAuthKey = new SecretAuthenticationKey();
					secretAuthKey.setKey(serialize(cookies));
					secretAuthKey.setUrnPrefix(triple.getUrnPrefix());
					secretAuthKey.setUsername(userAtIdpDomain);
					keys.add(secretAuthKey);

					cookieCache.put(secretAuthKey, cookies);

				} else {
					throw createAuthenticationFault("Authentication for urn[" + urn + "] and user["
							+ userAtIdpDomain + " failed."
					);
				}

			} catch (Exception e) {
				throw createSNAAFault("Authentication failed :" + e);
			}

		}

		log.debug("Done, returning " + keys.size() + " secret authentication key(s).");
		return new ArrayList<SecretAuthenticationKey>(keys);

	}

	@Override
	public AuthorizationResponse isAuthorized(final List<UsernameNodeUrnsMap> usernameNodeUrnsMapList,
											  final Action action) throws SNAAFault_Exception {

		// TODO implement authorization against ShibbolethAuthorization interface
		log.warn("TODO implement authorization against ShibbolethAuthorization interface");

		final AuthorizationResponse authorized = new AuthorizationResponse();
		authorized.setAuthorized(true);
		authorized.setMessage(
				"ShibbolethSNAA is currently used for authentication only and always returns 'true' for authorization"
		);

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

		assertAllUrnPrefixesInSAKsAreServed(urnPrefixes.get(), secretAuthenticationKeys);

		final List<ValidationResult> results = newArrayList();

		for (SecretAuthenticationKey key : secretAuthenticationKeys) {

			try {

				ValidationResult result = new ValidationResult();
				result.setUrnPrefix(key.getUrnPrefix());

				if (cookieCache.containsKey(key)) {

					result.setValid(true);

				} else {

					final List<Cookie> cookies;
					try {
						cookies = deserialize(key.getKey());
					} catch (Exception e) {
						throw propagate(e);
					}

					final ShibbolethAuthenticator sa = authenticatorProvider.get();
					sa.setUserAtIdpDomain(key.getUsername());
					result.setValid(sa.areCookiesValid(cookies));
				}

				results.add(result);

			} catch (Exception e) {

				log.error(e.getMessage());

				ValidationResult result = new ValidationResult();
				result.setValid(false);
				result.setUrnPrefix(key.getUrnPrefix());
				results.add(result);
			}
		}

		return results;
	}
}
