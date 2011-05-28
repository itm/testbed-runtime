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

package de.uniluebeck.itm.tr.snaa.federator;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.wisebed.testbed.api.snaa.helpers.SNAAServiceHelper;
import eu.wisebed.api.snaa.*;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import static eu.wisebed.testbed.api.snaa.helpers.Helper.createSNAAException;

@WebService(endpointInterface = "eu.wisebed.api.snaa.SNAA", portName = "SNAAPort",
		serviceName = "SNAAService", targetNamespace = "http://testbed.wisebed.eu/api/snaa/v1/")
public class FederatorSNAA implements SNAA {

	protected static class AuthenticationCallable implements Callable<List<SecretAuthenticationKey>> {

		private List<AuthenticationTriple> authenticationTriples;

		private String wsEndpointUrl;

		public AuthenticationCallable(String wsEndpointUrl, List<AuthenticationTriple> authenticationTriples) {
			super();
			this.authenticationTriples = authenticationTriples;
			this.wsEndpointUrl = wsEndpointUrl;
		}

		@Override
		public List<SecretAuthenticationKey> call() throws Exception {
			SNAA federatorSNAA = SNAAServiceHelper.getSNAAService(wsEndpointUrl);
			List<SecretAuthenticationKey> saks = federatorSNAA.authenticate(authenticationTriples);
			return saks;
		}
	}

	protected static class IsAuthorizedCallable implements Callable<Boolean> {

		private List<SecretAuthenticationKey> secretAuthenticationKeys;

		private Action action;

		private String wsEndpointUrl;

		public IsAuthorizedCallable(String wsEndpointUrl, List<SecretAuthenticationKey> secretAuthenticationKeys,
									Action action) {
			super();
			this.wsEndpointUrl = wsEndpointUrl;
			this.secretAuthenticationKeys = secretAuthenticationKeys;
			this.action = action;
		}

		@Override
		public Boolean call() throws Exception {
			return SNAAServiceHelper.getSNAAService(wsEndpointUrl).isAuthorized(secretAuthenticationKeys, action);
		}

	}

	public static final QName SNAAServiceQName = new QName("http://testbed.wisebed.eu/api/snaa/v1/", "SNAAService");

	/**
	 * Web Service Endpoint URL -> Set<URN Prefixes>
	 */
	protected Map<String, Set<String>> prefixSet;

	protected ExecutorService executorService = Executors.newCachedThreadPool(
			new ThreadFactoryBuilder().setNameFormat("FederatorSNAA-Thread %d").build()
	);

	public FederatorSNAA(Map<String, Set<String>> prefixSet) {
		super();
		this.prefixSet = prefixSet;
	}

	protected Map<String, Set<AuthenticationTriple>> getIntersectionPrefixSetAT(
			List<AuthenticationTriple> authenticationData) throws SNAAExceptionException {

		if (authenticationData == null) {
			throw createSNAAException("Argument authenticationData must not be null!");
		}

		// WS Endpoint URL -> Set<URN Prefixes> for intersection of authenticationData
		Map<String, Set<AuthenticationTriple>> intersectionPrefixSet = new HashMap<String, Set<AuthenticationTriple>>();

		for (AuthenticationTriple authenticationTriple : authenticationData) {

			for (Entry<String, Set<String>> entry : prefixSet.entrySet()) {
				if (entry.getValue().contains(authenticationTriple.getUrnPrefix())) {
					Set<AuthenticationTriple> set = intersectionPrefixSet.get(authenticationTriple.getUrnPrefix());
					if (set == null) {
						set = new HashSet<AuthenticationTriple>();
						intersectionPrefixSet.put(entry.getKey(), set);
					}
					set.add(authenticationTriple);
				}
			}

			// check if federator federates the urn prefix found in the authentication triple
			boolean found = false;
			for (Set<AuthenticationTriple> triples : intersectionPrefixSet.values()) {
				for (AuthenticationTriple triple : triples) {
					if (triple.getUrnPrefix().equals(authenticationTriple.getUrnPrefix())) {
						found = true;
					}
				}
			}

			if (!found) {
				throw createSNAAException("No endpoint known for URN prefix " + authenticationTriple.getUrnPrefix());
			}

		}

		return intersectionPrefixSet;
	}

	protected Map<String, Set<SecretAuthenticationKey>> getIntersectionPrefixSetSAK(
			List<SecretAuthenticationKey> authenticationKeys) throws SNAAExceptionException {

		// WS Endpoint URL -> Set<URN Prefixes> for intersection of
		// authenticationData
		Map<String, Set<SecretAuthenticationKey>> intersectionPrefixSet =
				new HashMap<String, Set<SecretAuthenticationKey>>();

		for (SecretAuthenticationKey secretAuthenticationKey : authenticationKeys) {

			for (Entry<String, Set<String>> entry : prefixSet.entrySet()) {
				if (entry.getValue().contains(secretAuthenticationKey.getUrnPrefix())) {
					Set<SecretAuthenticationKey> set = intersectionPrefixSet
							.get(secretAuthenticationKey.getUrnPrefix());
					if (set == null) {
						set = new HashSet<SecretAuthenticationKey>();
						intersectionPrefixSet.put(secretAuthenticationKey.getUrnPrefix(), set);
					}
					set.add(secretAuthenticationKey);
				}
			}

			// check if federator federates the urn prefix found in the authentication triple
			boolean found = false;
			for (Set<SecretAuthenticationKey> triples : intersectionPrefixSet.values()) {
				for (SecretAuthenticationKey triple : triples) {
					if (triple.getUrnPrefix().equals(secretAuthenticationKey.getUrnPrefix())) {
						found = true;
					}
				}
			}

			if (!found) {
				throw createSNAAException("No endpoint known for URN prefix " + secretAuthenticationKey.getUrnPrefix());
			}

		}

		return intersectionPrefixSet;
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(
			@WebParam(name = "authenticationData", targetNamespace = "") List<AuthenticationTriple> authenticationData)
			throws AuthenticationExceptionException, SNAAExceptionException {

		Map<String, Set<AuthenticationTriple>> intersectionPrefixSet = getIntersectionPrefixSetAT(authenticationData);

		Set<Future<List<SecretAuthenticationKey>>> futures = new HashSet<Future<List<SecretAuthenticationKey>>>();

		for (String wsEndpointUrl : intersectionPrefixSet.keySet()) {
			AuthenticationCallable authenticationCallable = new AuthenticationCallable(wsEndpointUrl,
					new ArrayList<AuthenticationTriple>(intersectionPrefixSet.get(wsEndpointUrl))
			);
			Future<List<SecretAuthenticationKey>> future = executorService.submit(authenticationCallable);
			futures.add(future);
		}

		List<SecretAuthenticationKey> resultSet = new LinkedList<SecretAuthenticationKey>();

		for (Future<List<SecretAuthenticationKey>> future : futures) {
			try {
				resultSet.addAll(future.get());
			} catch (InterruptedException e) {
				SNAAException exception = new SNAAException();
				exception.setMessage(e.getMessage());
				throw new SNAAExceptionException(e.getMessage(), exception, e);
			} catch (ExecutionException e) {
				SNAAException exception = new SNAAException();
				exception.setMessage(e.getMessage());
				throw new SNAAExceptionException(e.getMessage(), exception, e);
			}
		}

		return resultSet;

	}

	@Override
	public boolean isAuthorized(
			@WebParam(name = "authenticationData", targetNamespace = "")
			List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "action", targetNamespace = "") Action action) throws SNAAExceptionException {

		if (authenticationData == null || action == null) {
			throw createSNAAException("Arguments must not be null!");
		}

		Map<String, Set<SecretAuthenticationKey>> intersectionPrefixSet =
				getIntersectionPrefixSetSAK(authenticationData);

		Set<Future<Boolean>> futures = new HashSet<Future<Boolean>>();

		for (String urnPrefix : intersectionPrefixSet.keySet()) {
			IsAuthorizedCallable authenticationCallable = new IsAuthorizedCallable(getWsnUrlFromUrnPrefix(urnPrefix),
					new ArrayList<SecretAuthenticationKey>(intersectionPrefixSet.get(urnPrefix)), action
			);
			Future<Boolean> future = executorService.submit(authenticationCallable);
			futures.add(future);
		}

		for (Future<Boolean> future : futures) {
			try {
				if (!future.get()) {
					return false;
				}
			} catch (InterruptedException e) {
				throw createSNAAException(e.getMessage());
			} catch (ExecutionException e) {
				throw createSNAAException(e.getMessage());
			}
		}

		return true;
	}

	private String getWsnUrlFromUrnPrefix(String urnPrefix) {
		for (String url : prefixSet.keySet()) {
			if (prefixSet.get(url).contains(urnPrefix)) {
				return url;
			}
		}
		return null;
	}

}
