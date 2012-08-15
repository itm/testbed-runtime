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

package de.uniluebeck.itm.tr.snaa;

import eu.wisebed.api.v3.snaa.*;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.common.UsernameUrnPrefixPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static de.uniluebeck.itm.tr.util.Preconditions.assertCollectionMinCount;
import static de.uniluebeck.itm.tr.util.Preconditions.assertCollectionMinMaxCount;

public class SNAAHelper {

	private static final Logger log = LoggerFactory.getLogger(SNAAHelper.class);

	public static void assertMinAuthenticationCount(List<AuthenticationTriple> authenticationData,
													int minCountInclusive)
			throws SNAAFault_Exception {
		try {
			assertCollectionMinCount(authenticationData, minCountInclusive);
		} catch (Exception e) {
			createSNAAFault(e.getMessage());
		}
	}

	public static void assertAuthenticationCount(List<AuthenticationTriple> authenticationData, int minCountInclusive,
												 int maxCountInclusive) throws SNAAFault_Exception {

		try {
			assertCollectionMinMaxCount(authenticationData, minCountInclusive, maxCountInclusive);
		} catch (Exception e) {
			createSNAAFault(e.getMessage());
		}
	}

	public static void assertAuthenticationKeyCount(List<SecretAuthenticationKey> authenticationData,
													int minCountInclusive, int maxCountInclusive)
			throws SNAAFault_Exception {

		try {
			assertCollectionMinCount(authenticationData, minCountInclusive);
		} catch (Exception e) {
			createSNAAFault(e.getMessage());
		}

	}

	public static void assertElementCount(Collection<?> c, int minCountInclusive, int maxCountInclusive)
			throws SNAAFault_Exception {

		try {
			assertCollectionMinCount(c, minCountInclusive);
		} catch (Exception e) {
			createSNAAFault(e.getMessage());
		}

	}

	public static void assertUrnPrefixServed(String servedURNPrefix, List<AuthenticationTriple> authenticationData)
			throws SNAAFault_Exception {
		Set<String> urnPrefixes = new HashSet<String>();
		urnPrefixes.add(servedURNPrefix);

		assertAuthenticationCount(authenticationData, 1, 1);
		assertAllUrnPrefixesServed(urnPrefixes, authenticationData);
	}

	public static void assertSAKUrnPrefixServed(String servedURNPrefix,
												List<SecretAuthenticationKey> authenticationData)
			throws SNAAFault_Exception {
		Set<String> urnPrefixes = new HashSet<String>();
		urnPrefixes.add(servedURNPrefix);

		assertAuthenticationKeyCount(authenticationData, 1, 1);
		assertAllSAKUrnPrefixesServed(urnPrefixes, authenticationData);
	}

	public static void assertAllUrnPrefixesServed(Set<String> servedURNPrefixes,
												  List<AuthenticationTriple> authenticationData)
			throws SNAAFault_Exception {

		for (AuthenticationTriple triple : authenticationData) {
			if (!servedURNPrefixes.contains(triple.getUrnPrefix())) {
				throw createSNAAFault("Not serving urn prefix " + triple.getUrnPrefix());
			}
		}
	}

	public static void assertAllSAKUrnPrefixesServed(Set<String> servedURNPrefixes,
													 List<SecretAuthenticationKey> authenticationData)
			throws SNAAFault_Exception {

		for (SecretAuthenticationKey key : authenticationData) {
			if (!servedURNPrefixes.contains(key.getUrnPrefix())) {
				throw createSNAAFault("Not serving urn prefix " + key.getUrnPrefix());
			}
		}
	}

	/**
	 * @param msg
	 *
	 * @return
	 */
	public static SNAAFault_Exception createSNAAFault(String msg) {
		log.warn(msg);
		SNAAFault exception = new SNAAFault();
		exception.setMessage(msg);
		return new SNAAFault_Exception(msg, exception);
	}

	public static AuthenticationFault_Exception createAuthenticationFault_Exception(String msg) {
		log.warn(msg);
		AuthenticationFault exception = new AuthenticationFault();
		exception.setMessage(msg);
		return new AuthenticationFault_Exception(msg, exception);
	}
	


	// ------------------------------------------------------------------------
	/**
	 * Converts a list of secret authentication keys to a list of tuples comprising user names and
	 * urn prefixes and returns the result.
	 * 
	 * @param secretAuthenticationKeys
	 *            A list of secret authentication keys
	 * @return A list of tuples comprising user names and urn prefixes
	 */
	public static List<UsernameUrnPrefixPair> convert(final List<SecretAuthenticationKey> secretAuthenticationKeys) {
		List<UsernameUrnPrefixPair> usernamePrefixPairs = new LinkedList<UsernameUrnPrefixPair>();
		for (SecretAuthenticationKey secretAuthenticationKey : secretAuthenticationKeys) {
			UsernameUrnPrefixPair upp = new UsernameUrnPrefixPair();
			usernamePrefixPairs.add(upp);
			upp.setUsername(secretAuthenticationKey.getUsername());
			upp.setUrnPrefix(secretAuthenticationKey.getUrnPrefix());
		}
		return null;
	}

}