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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.snaa.federator.FederatorSNAA;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethProxy;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAAImpl;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

@WebService(
		name = "SNAA",
		endpointInterface = "eu.wisebed.api.v3.snaa.SNAA",
		portName = "SNAAPort",
		serviceName = "SNAAService",
		targetNamespace = "http://wisebed.eu/api/v3/snaa"
)
public class WisebedSnaaFederator implements SNAA {

	private static final Logger log = LoggerFactory.getLogger(WisebedSnaaFederator.class);

	private FederatorSNAA authorizationFederator;

	private ShibbolethSNAAImpl authenticationSnaa;

	public WisebedSnaaFederator(FederationManager<SNAA> federationManager, String secretAuthenticationKeyUrl,
								Injector injector, ShibbolethProxy shibbolethProxy) {

		authenticationSnaa = new ShibbolethSNAAImpl(
				federationManager.getUrnPrefixes(),
				secretAuthenticationKeyUrl,
				null,
				injector,
				shibbolethProxy
		);

		// authorization is delegated to the corresponding backend-SNAA using a FederatorSNAA
		authorizationFederator = new FederatorSNAA(federationManager);
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(final List<AuthenticationTriple> authenticationData)
			throws AuthenticationFault_Exception, SNAAFault_Exception {

		log.debug("WisebedSnaaFederator::authenticate delegating to internal ShibbolethSNAA instance");
		return authenticationSnaa.authenticate(authenticationData);
	}

	@Override
	public AuthorizationResponse isAuthorized(final List<UsernameNodeUrnsMap> usernameNodeUrnsMapList,
											  final Action action) throws SNAAFault_Exception {

		log.debug("WisebedSnaaFederator::isAuthorized delegating to internal FederatorSNAA instance");
		return authorizationFederator.isAuthorized(usernameNodeUrnsMapList, action);
	}

	@Override
	public eu.wisebed.api.v3.snaa.IsValidResponse.ValidationResult isValid(
			final SecretAuthenticationKey secretAuthenticationKey) throws SNAAFault_Exception {

		log.debug("WisebedSnaaFederator::isValid delegating to internal FederatorSNAA instance");
		return authorizationFederator.isValid(secretAuthenticationKey);
	}

}
