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
import com.google.inject.name.Named;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.snaa.shibboleth.authorization.IUserAuthorization;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.*;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.snaa.common.SNAAHelper.*;

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

	protected final IUserAuthorization authorization;

	protected final ShibbolethAuthenticator authenticator;

	protected final String snaaContextPath;

	private ServicePublisherService jaxWsService;

	@Inject
	public ShibbolethSNAA(final ServicePublisher servicePublisher,
						  @Named("snaaContextPath") final String snaaContextPath,
						  final ServedNodeUrnPrefixesProvider urnPrefixes,
						  final IUserAuthorization authorization,
						  final ShibbolethAuthenticator authenticator) {
		this.servicePublisher = checkNotNull(servicePublisher);
		this.snaaContextPath = checkNotNull(snaaContextPath);
		this.urnPrefixes = checkNotNull(urnPrefixes);
		this.authorization = checkNotNull(authorization);
		this.authenticator = checkNotNull(authenticator);
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(final List<AuthenticationTriple> authenticationData)
			throws AuthenticationFault_Exception, SNAAFault_Exception {

		HashSet<SecretAuthenticationKey> keys = new HashSet<SecretAuthenticationKey>();
		log.debug("Starting for " + authenticationData.size() + " urns.");

		assertMinAuthenticationCount(authenticationData, 1);
		assertAllUrnPrefixesServed(urnPrefixes.get(), authenticationData);

		for (AuthenticationTriple triple : authenticationData) {
			NodeUrnPrefix urn = triple.getUrnPrefix();

			try {

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
					SecretAuthenticationKey secretAuthKey = new SecretAuthenticationKey();
					secretAuthKey.setKey(
							ShibbolethAuthenticatorSerialization.serialize(authenticator.getCookieStore().getCookies()));
					secretAuthKey.setUrnPrefix(triple.getUrnPrefix());
					secretAuthKey.setUsername(triple.getUsername());
					keys.add(secretAuthKey);
				} else {
					throw createAuthenticationFault_Exception("Authentication for urn[" + urn + "] and user["
							+ triple.getUsername() + " failed."
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

		AuthorizationResponse authorized = new AuthorizationResponse();

		authorized.setAuthorized(true);
		authorized.setMessage("ShibbolethSNAA is used for authentication only and always return 'true'");

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

		// Check if we serve all URNs
		assertAllUrnPrefixesInSAKsAreServed(urnPrefixes.get(), secretAuthenticationKeys);

		// TODO Auto-generated method stub ShibbolethSNAA#isValid(SecretAuthenticationKey)
		return null;
	}

	@Override
	protected void doStart() {
		try {
			jaxWsService = servicePublisher.createJaxWsService(snaaContextPath, this);
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
}
