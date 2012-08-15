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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.common.UsernameUrnPrefixPair;
import eu.wisebed.api.v3.snaa.*;
import eu.wisebed.api.v3.snaa.IsValidResponse.ValidationResult;

import javax.jws.WebService;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import static de.uniluebeck.itm.tr.snaa.SNAAHelper.createSNAAException;

@WebService(
		endpointInterface = "eu.wisebed.api.v3.snaa.SNAA",
		portName = "SNAAPort",
		serviceName = "SNAAService",
		targetNamespace = "http://testbed.wisebed.eu/api/snaa/v1/"
)
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
			SNAA federatorSNAA = WisebedServiceHelper.getSNAAService(wsEndpointUrl);
			return federatorSNAA.authenticate(authenticationTriples);
		}
	}


	protected static class IsAuthorizedCallable implements Callable<AuthorizationResponse> {

		private List<UsernameNodeUrnsMap> userNamesNodeUrnsMaps;

		private Action action;

		private String wsEndpointUrl;


		public IsAuthorizedCallable(String wsEndpointUrl, List<UsernameNodeUrnsMap> userNamesNodeUrnsMaps,
									Action action) {
			super();
			this.wsEndpointUrl = wsEndpointUrl;
			this.userNamesNodeUrnsMaps = userNamesNodeUrnsMaps;
			this.action = action;
		}

		@Override
		public AuthorizationResponse call() throws Exception {
			return WisebedServiceHelper.getSNAAService(wsEndpointUrl).isAuthorized(userNamesNodeUrnsMaps, action);
		}

	}

	protected static class IsValidCallable implements Callable<ValidationResult> {

		private SecretAuthenticationKey secretAuthenticationKey;

		public IsValidCallable(SecretAuthenticationKey secretAuthenticationKey) {
			super();
			this.secretAuthenticationKey = secretAuthenticationKey;
		}

		@Override
		public ValidationResult call() throws Exception {
			return WisebedServiceHelper.getSNAAService(secretAuthenticationKey.getUrnPrefix())
					.isValid(secretAuthenticationKey);
		}

	}


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

	protected Map<String, Set<UsernameUrnPrefixPair>> getIntersectionPrefixSetUPP(
			List<UsernameUrnPrefixPair> usernameURNPrefixPairs) throws SNAAExceptionException {

		// WS Endpoint URL -> Set<URN Prefixes> for intersection of
		// authenticationData
		Map<String, Set<UsernameUrnPrefixPair>> intersectionPrefixSet =
				new HashMap<String, Set<UsernameUrnPrefixPair>>();

		for (UsernameUrnPrefixPair usernameURNPrefixPair : usernameURNPrefixPairs) {

			for (Entry<String, Set<String>> entry : prefixSet.entrySet()) {
				if (entry.getValue().contains(usernameURNPrefixPair.getUrnPrefix())) {
					Set<UsernameUrnPrefixPair> set = intersectionPrefixSet
							.get(usernameURNPrefixPair.getUrnPrefix());
					if (set == null) {
						set = new HashSet<UsernameUrnPrefixPair>();
						intersectionPrefixSet.put(usernameURNPrefixPair.getUrnPrefix(), set);
					}
					set.add(usernameURNPrefixPair);
				}
			}

			// check if federator federates the urn prefix found in the authentication triple
			boolean found = false;
			for (Set<UsernameUrnPrefixPair> triples : intersectionPrefixSet.values()) {
				for (UsernameUrnPrefixPair triple : triples) {
					if (triple.getUrnPrefix().equals(usernameURNPrefixPair.getUrnPrefix())) {
						found = true;
					}
				}
			}

			if (!found) {
				throw createSNAAException("No endpoint known for URN prefix " + usernameURNPrefixPair.getUrnPrefix());
			}

		}

		return intersectionPrefixSet;
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(final List<AuthenticationTriple> authenticationData)
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
	public AuthorizationResponse isAuthorized(final List<UsernameNodeUrnsMap> usernameNodeUrnsMapList,
											  final Action action) throws SNAAExceptionException {

		if (usernameNodeUrnsMapList == null || action == null) {
			throw createSNAAException("Arguments must not be null!");
		}

		AuthorizationResponse response = new AuthorizationResponse();
		response.setAuthorized(true);
		response.setMessage("");

//		Map<String, Set<UsernameUrnPrefixPair>> intersectionPrefixSet =
//				getIntersectionPrefixSetUPP(usernames);

		Set<Future<AuthorizationResponse>> futures = new HashSet<Future<AuthorizationResponse>>();

		for (UsernameNodeUrnsMap usernameNodeUrnsMap : usernameNodeUrnsMapList) {
			IsAuthorizedCallable authenticationCallable =
					new IsAuthorizedCallable(getWsnUrlFromUrnPrefix(usernameNodeUrnsMap.getUsername().getUrnPrefix()),
							Lists.newArrayList(usernameNodeUrnsMap), action
					);
			Future<AuthorizationResponse> future = executorService.submit(authenticationCallable);
			futures.add(future);
		}

		for (Future<AuthorizationResponse> future : futures) {
			try {
				AuthorizationResponse authorizationResponse = future.get();
				response.setMessage(response.getMessage() + "; " + authorizationResponse.getMessage());
				response.setAuthorized(response.isAuthorized() && authorizationResponse.isAuthorized());
			} catch (InterruptedException e) {
				throw createSNAAException(e.getMessage());
			} catch (ExecutionException e) {
				throw createSNAAException(e.getMessage());
			}
		}

		return response;
	}

	private String getWsnUrlFromUrnPrefix(String urnPrefix) {
		for (String url : prefixSet.keySet()) {
			if (prefixSet.get(url).contains(urnPrefix)) {
				return url;
			}
		}
		return null;
	}

	@Override
	public eu.wisebed.api.v3.snaa.IsValidResponse.ValidationResult isValid(
			final SecretAuthenticationKey secretAuthenticationKey) throws SNAAExceptionException {

		if (secretAuthenticationKey == null) {
			throw createSNAAException("SecretAuthenticationKey must not be null!");
		}

		try {
			Future<ValidationResult> future = executorService.submit(new IsValidCallable(secretAuthenticationKey));
			return future.get();
		} catch (InterruptedException e) {
			throw createSNAAException(e.getMessage());
		} catch (ExecutionException e) {
			throw createSNAAException(e.getMessage());
		}
	}

}
