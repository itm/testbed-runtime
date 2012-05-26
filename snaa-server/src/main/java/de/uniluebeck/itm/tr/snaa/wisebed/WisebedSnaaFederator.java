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

import com.google.inject.Injector;
import de.uniluebeck.itm.tr.snaa.federator.FederatorSNAA;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethProxy;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAAImpl;
import eu.wisebed.api.snaa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@WebService(endpointInterface = "eu.wisebed.api.snaa.SNAA", portName = "SNAAPort", serviceName = "SNAAService", targetNamespace = "http://testbed.wisebed.eu/api/snaa/v1/")
public class WisebedSnaaFederator implements SNAA {

	private static final Logger log = LoggerFactory.getLogger(WisebedSnaaFederator.class);

	private FederatorSNAA authorizationFederator;

	private ShibbolethSNAAImpl authenticationSnaa;

	public WisebedSnaaFederator(Map<String, Set<String>> prefixSet, String secretAuthenticationKeyUrl, Injector injector, ShibbolethProxy shibbolethProxy) {

		//Authentication is performed for the union of all prefixes by a ShibbolethSNAA
		Set<String> urnPrefixUnion = new HashSet<String>();
		for (Set<String> urnPrefixes : prefixSet.values())
			urnPrefixUnion.addAll(urnPrefixes);
		authenticationSnaa = new ShibbolethSNAAImpl(urnPrefixUnion, secretAuthenticationKeyUrl, null, injector, shibbolethProxy);

		//Authorization is delegated to the corresponding backend-SNAA using a FederatorSNAA
		authorizationFederator = new FederatorSNAA(prefixSet);
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(
			@WebParam(name = "authenticationData", targetNamespace = "") List<AuthenticationTriple> authenticationData)
			throws AuthenticationExceptionException, SNAAExceptionException {

		log.debug("WisebedSnaaFederator::authenticate delegating to internal ShibbolethSNAA instance");
        return authenticationSnaa.authenticate(authenticationData);
	}

	@Override
	public boolean isAuthorized(
			@WebParam(name = "authenticationData", targetNamespace = "") List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "action", targetNamespace = "") Action action) throws SNAAExceptionException {

		log.debug("WisebedSnaaFederator::isAuthorized delegating to internal FederatorSNAA instance");
		return authorizationFederator.isAuthorized(authenticationData, action);
	}

}
