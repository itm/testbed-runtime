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

package eu.wisebed.testbed.api.snaa.helpers;

import eu.wisebed.api.snaa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static de.uniluebeck.itm.tr.util.Preconditions.assertCollectionMinCount;
import static de.uniluebeck.itm.tr.util.Preconditions.assertCollectionMinMaxCount;

public class Helper {
	private static final Logger log = LoggerFactory.getLogger(Helper.class);

	public static void assertMinAuthenticationCount(List<AuthenticationTriple> authenticationData, int minCountInclusive)
			throws SNAAExceptionException {
		try {
			assertCollectionMinCount(authenticationData, minCountInclusive);
		} catch (Exception e) {
			createSNAAException(e.getMessage());
		}
	}

	public static void assertAuthenticationCount(List<AuthenticationTriple> authenticationData, int minCountInclusive,
												 int maxCountInclusive) throws SNAAExceptionException {

		try {
			assertCollectionMinMaxCount(authenticationData, minCountInclusive, maxCountInclusive);
		} catch (Exception e) {
			createSNAAException(e.getMessage());
		}
	}

	public static void assertAuthenticationKeyCount(List<SecretAuthenticationKey> authenticationData,
													int minCountInclusive, int maxCountInclusive) throws SNAAExceptionException {

		try {
			assertCollectionMinCount(authenticationData, minCountInclusive);
		} catch (Exception e) {
			createSNAAException(e.getMessage());
		}

	}

	public static void assertUrnPrefixServed(String servedURNPrefix, List<AuthenticationTriple> authenticationData)
			throws SNAAExceptionException {
		Set<String> urnPrefixes = new HashSet<String>();
		urnPrefixes.add(servedURNPrefix);

		assertAuthenticationCount(authenticationData, 1, 1);
		assertAllUrnPrefixesServed(urnPrefixes, authenticationData);
	}

	public static void assertSAKUrnPrefixServed(String servedURNPrefix, List<SecretAuthenticationKey> authenticationData)
			throws SNAAExceptionException {
		Set<String> urnPrefixes = new HashSet<String>();
		urnPrefixes.add(servedURNPrefix);

		assertAuthenticationKeyCount(authenticationData, 1, 1);
		assertAllSAKUrnPrefixesServed(urnPrefixes, authenticationData);
	}

	public static void assertAllUrnPrefixesServed(Set<String> servedURNPrefixes,
												  List<AuthenticationTriple> authenticationData) throws SNAAExceptionException {

		for (AuthenticationTriple triple : authenticationData) {
			if (!servedURNPrefixes.contains(triple.getUrnPrefix())) {
				throw createSNAAException("Not serving urn prefix " + triple.getUrnPrefix());
			}
		}
	}

	public static void assertAllSAKUrnPrefixesServed(Set<String> servedURNPrefixes,
													 List<SecretAuthenticationKey> authenticationData) throws SNAAExceptionException {

		for (SecretAuthenticationKey key : authenticationData) {
			if (!servedURNPrefixes.contains(key.getUrnPrefix())) {
				throw createSNAAException("Not serving urn prefix " + key.getUrnPrefix());
			}
		}
	}

	/**
	 * @param msg
	 * @return
	 */
	public static SNAAExceptionException createSNAAException(String msg) {
		log.warn(msg);
		SNAAException exception = new SNAAException();
		exception.setMessage(msg);
		return new SNAAExceptionException(msg, exception);
	}

	public static AuthenticationExceptionException createAuthenticationException(String msg) {
		log.warn(msg);
		AuthenticationException exception = new AuthenticationException();
		exception.setMessage(msg);
		return new AuthenticationExceptionException(msg, exception);
	}

}
