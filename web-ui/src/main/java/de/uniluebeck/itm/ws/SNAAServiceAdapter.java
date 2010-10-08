/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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
package de.uniluebeck.itm.ws;

import de.uniluebeck.itm.exception.AuthenticationException;
import eu.wisebed.testbed.api.snaa.helpers.SNAAServiceHelper;
import eu.wisebed.testbed.api.snaa.v1.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Soenke Nommensen
 */
public class SNAAServiceAdapter implements Serializable {

    private static final String SNAA_ENDPOINT_URL = "http://wisebed.itm.uni-luebeck.de:8890/snaa?wsdl";
    private SNAA snaaService;
    private List<AuthenticationTriple> authenticationTripleList;
    private List<SecretAuthenticationKey> secretAuthenticationKeyList;

    public SNAAServiceAdapter() {
        snaaService = SNAAServiceHelper.getSNAAService(SNAA_ENDPOINT_URL);
        authenticationTripleList = new ArrayList<AuthenticationTriple>(1);
    }

    public void addAuthenticationTriple(final String user, final String urn, final String password) {
        AuthenticationTriple authenticationTriple = new AuthenticationTriple();
        authenticationTriple.setUsername(user);
        authenticationTriple.setUrnPrefix(urn);
        authenticationTriple.setPassword(password);
        authenticationTripleList.add(authenticationTriple);
    }

    public void authenticate() throws AuthenticationException {
        try {
            secretAuthenticationKeyList = snaaService.authenticate(authenticationTripleList);
        } catch (AuthenticationExceptionException ex) {
            throw new AuthenticationException("Authentication failed", ex);
        } catch (SNAAExceptionException ex) {
            throw new AuthenticationException("Authentication failed due to an error", ex);
        } catch (com.sun.xml.internal.ws.client.ClientTransportException ex) {
            throw new AuthenticationException("Operation timed out", ex);
        }
    }

    public List<SecretAuthenticationKey> getSecretAuthenticationKeyList() {
        return secretAuthenticationKeyList;
    }
}
