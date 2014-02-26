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

package de.uniluebeck.itm.tr.snaa.dummy;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.UserAlreadyExistsException;
import de.uniluebeck.itm.tr.snaa.UserPwdMismatchException;
import de.uniluebeck.itm.tr.snaa.UserUnknownException;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.*;
import org.omg.CORBA.UnknownUserException;

import javax.annotation.Nullable;
import javax.jws.WebService;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

@WebService(
		name = "SNAA",
		endpointInterface = "eu.wisebed.api.v3.snaa.SNAA",
		portName = "SNAAPort",
		serviceName = "SNAAService",
		targetNamespace = "http://wisebed.eu/api/v3/snaa"
)
public class DummySNAA extends AbstractService implements de.uniluebeck.itm.tr.snaa.SNAAService {

	private final Random r = new SecureRandom();

	private final SNAAServiceConfig config;

	private final ServicePublisher servicePublisher;

	private ServicePublisherService jaxWsService;

	@Inject
	public DummySNAA(final SNAAServiceConfig config, final ServicePublisher servicePublisher) {
		this.config = config;
		this.servicePublisher = servicePublisher;
	}

	@Override
	public AuthenticateResponse authenticate(final Authenticate parameters)
			throws AuthenticationFault, SNAAFault_Exception {

		final List<AuthenticationTriple> authenticationData = parameters.getAuthenticationData();
		final AuthenticateResponse authenticateResponse = new AuthenticateResponse();

		for (AuthenticationTriple triple : authenticationData) {
			SecretAuthenticationKey secretAuthenticationKey = new SecretAuthenticationKey();
			secretAuthenticationKey.setUrnPrefix(triple.getUrnPrefix());
			secretAuthenticationKey.setKey(Long.toString(r.nextLong()));
			secretAuthenticationKey.setUsername(triple.getUsername());
			authenticateResponse.getSecretAuthenticationKey().add(secretAuthenticationKey);
		}

		return authenticateResponse;
	}

	@Override
	public AuthorizationResponse isAuthorized(final List<UsernameNodeUrnsMap> usernameNodeUrnsMapList,
											  final Action action)
			throws SNAAFault_Exception {

		AuthorizationResponse response = new AuthorizationResponse();
		response.setAuthorized(true);
		response.setMessage("DummySNAA will always return true");
		return response;
	}

	@Override
	public List<ValidationResult> isValid(final List<SecretAuthenticationKey> secretAuthenticationKey)
			throws SNAAFault_Exception {

		ValidationResult result = new ValidationResult();
		result.setValid(true);
		return newArrayList(result);
	}

	@Override
	protected void doStart() {
		try {
			jaxWsService = servicePublisher.createJaxWsService(config.getSnaaContextPath(), this, null);
			jaxWsService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			jaxWsService.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public String toString() {
		return "DummySNAA{" +
				"config=" + config +
				"}@" + Integer.toHexString(hashCode());
	}

	@Override
	public boolean isUserRegistrationSupported() {
		return true;
	}

	@Override
	public void add(final String email, final String password)
			throws UserAlreadyExistsException {
		// nothing to do
	}

	@Override
	public void update(final String email, final String oldPassword, final String newPassword)
			throws UserUnknownException, UserPwdMismatchException {
		// nothing to do
	}

	@Override
	public void delete(final String email, final String password)
			throws UserUnknownException, UserPwdMismatchException {
		// nothing to do
	}
}
