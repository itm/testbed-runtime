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

package de.uniluebeck.itm.tr.federator.snaa;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@WebService(
		name = "SNAA",
		endpointInterface = "eu.wisebed.api.v3.snaa.SNAA",
		portName = "SNAAPort",
		serviceName = "SNAAService",
		targetNamespace = "http://wisebed.eu/api/v3/snaa"
)
public class DelegatingSNAAFederatorServiceImpl extends AbstractService implements SNAAFederatorService {

	private static final Logger log = LoggerFactory.getLogger(DelegatingSNAAFederatorServiceImpl.class);

	private final de.uniluebeck.itm.tr.snaa.SNAAService authorizationSnaa;

	private final de.uniluebeck.itm.tr.snaa.SNAAService authenticationSnaa;

	@Inject
	public DelegatingSNAAFederatorServiceImpl(
			@Named("authorizationSnaa") final de.uniluebeck.itm.tr.snaa.SNAAService authorizationSnaa,
			@Named("authenticationSnaa") final de.uniluebeck.itm.tr.snaa.SNAAService authenticationSnaa) {
		this.authenticationSnaa = checkNotNull(authenticationSnaa);
		this.authorizationSnaa = checkNotNull(authorizationSnaa);
	}

	@Override
	public AuthenticateResponse authenticate(final Authenticate authenticate)
			throws AuthenticationFault, SNAAFault_Exception {

		log.debug("DelegatingSNAAFederatorServiceImpl::authenticate delegating to internal ShibbolethSNAA instance");
		return authenticationSnaa.authenticate(authenticate);
	}

	@Override
	public AuthorizationResponse isAuthorized(final List<UsernameNodeUrnsMap> usernameNodeUrnsMapList,
											  final Action action) throws SNAAFault_Exception {

		log.debug("DelegatingSNAAFederatorServiceImpl::isAuthorized delegating to internal FederatorSNAA instance");
		return authorizationSnaa.isAuthorized(usernameNodeUrnsMapList, action);
	}

	@Override
	public List<ValidationResult> isValid(final List<SecretAuthenticationKey> secretAuthenticationKeys)
			throws SNAAFault_Exception {

		log.debug("DelegatingSNAAFederatorServiceImpl::isValid delegating to internal FederatorSNAA instance");
		return authorizationSnaa.isValid(secretAuthenticationKeys);
	}

	@Override
	protected void doStart() {
		try {
			authenticationSnaa.startAndWait();
			authorizationSnaa.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			authorizationSnaa.stopAndWait();
			authenticationSnaa.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}