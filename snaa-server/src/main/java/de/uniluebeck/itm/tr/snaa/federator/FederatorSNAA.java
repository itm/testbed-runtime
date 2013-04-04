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
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.common.UsernameUrnPrefixPair;
import eu.wisebed.api.v3.snaa.*;

import javax.jws.WebService;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.snaa.SNAAHelper.createSNAAFault;

@WebService(
		name = "SNAA",
		endpointInterface = "eu.wisebed.api.v3.snaa.SNAA",
		portName = "SNAAPort",
		serviceName = "SNAAService",
		targetNamespace = "http://wisebed.eu/api/v3/snaa"
)
public class FederatorSNAA implements SNAA {

	protected static class AuthenticationCallable implements Callable<List<SecretAuthenticationKey>> {

		private final SNAA snaa;

		private final List<AuthenticationTriple> authenticationTriples;

		public AuthenticationCallable(SNAA snaa, List<AuthenticationTriple> authenticationTriples) {
			this.snaa = snaa;
			this.authenticationTriples = authenticationTriples;
		}

		@Override
		public List<SecretAuthenticationKey> call() throws Exception {
			return snaa.authenticate(authenticationTriples);
		}
	}

	protected static class IsAuthorizedCallable implements Callable<AuthorizationResponse> {

		private final SNAA snaa;

		private final List<UsernameNodeUrnsMap> userNamesNodeUrnsMaps;

		private final Action action;

		public IsAuthorizedCallable(final SNAA snaa,
									final List<UsernameNodeUrnsMap> userNamesNodeUrnsMaps,
									final Action action) {
			this.snaa = snaa;
			this.userNamesNodeUrnsMaps = userNamesNodeUrnsMaps;
			this.action = action;
		}

		@Override
		public AuthorizationResponse call() throws Exception {
			return snaa.isAuthorized(userNamesNodeUrnsMaps, action);
		}
	}

	protected static class IsValidCallable implements Callable<List<ValidationResult>> {

		private final SNAA snaa;

		private final List<SecretAuthenticationKey> secretAuthenticationKeys;

		public IsValidCallable(final SNAA snaa, final List<SecretAuthenticationKey> secretAuthenticationKeys) {
			this.snaa = snaa;
			this.secretAuthenticationKeys = secretAuthenticationKeys;
		}

		@Override
		public List<ValidationResult> call() throws Exception {
			return snaa.isValid(secretAuthenticationKeys);
		}
	}

	private final FederationManager<SNAA> federationManager;

	protected final ExecutorService executorService = Executors
			.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("FederatorSNAA-Thread %d").build());

	public FederatorSNAA(final FederationManager<SNAA> federationManager) {
		this.federationManager = federationManager;
	}

	protected Map<SNAA, Set<AuthenticationTriple>> getIntersectionPrefixSetAT(
			List<AuthenticationTriple> authenticationData) throws SNAAFault_Exception {

		checkNotNull(authenticationData, "Parameter authenticationData must not be null!");

		// SNAA Endpoint URL -> Set<URN Prefixes> for intersection of authenticationData
		Map<SNAA, Set<AuthenticationTriple>> intersectionPrefixSet = newHashMap();

		for (AuthenticationTriple at : authenticationData) {

			// check if federator federates the urn prefix found in the authentication triple
			if (!federationManager.servesUrnPrefix(at.getUrnPrefix())) {
				throw createSNAAFault("No endpoint known for URN prefix " + at.getUrnPrefix());
			}

			final SNAA endpoint = federationManager.getEndpointByUrnPrefix(at.getUrnPrefix());

			Set<AuthenticationTriple> set = intersectionPrefixSet.get(endpoint);
			if (set == null) {
				set = newHashSet();
				intersectionPrefixSet.put(endpoint, set);
			}
			set.add(at);
		}

		return intersectionPrefixSet;
	}

	protected Map<SNAA, Set<SecretAuthenticationKey>> getIntersectionPrefixSetSAK(
			List<SecretAuthenticationKey> authenticationKeys) throws SNAAFault_Exception {

		checkNotNull(authenticationKeys, "Parameter authenticationKeys must not be null!");

		// WS Endpoint URL -> Set<URN Prefixes> for intersection of authenticationData
		Map<SNAA, Set<SecretAuthenticationKey>> intersectionPrefixSet = newHashMap();

		for (SecretAuthenticationKey sak : authenticationKeys) {

			// check if federator federates the urn prefix found in the authentication triple
			if (!federationManager.servesUrnPrefix(sak.getUrnPrefix())) {
				throw createSNAAFault("No endpoint known for URN prefix " + sak.getUrnPrefix());
			}

			final SNAA endpoint = federationManager.getEndpointByUrnPrefix(sak.getUrnPrefix());

			Set<SecretAuthenticationKey> set = intersectionPrefixSet.get(endpoint);
			if (set == null) {
				set = newHashSet();
				intersectionPrefixSet.put(endpoint, set);
			}
			set.add(sak);
		}

		return intersectionPrefixSet;
	}

	protected Map<SNAA, Set<UsernameUrnPrefixPair>> getIntersectionPrefixSetUPP(
			List<UsernameUrnPrefixPair> usernameURNPrefixPairs) throws SNAAFault_Exception {

		// WS Endpoint URL -> Set<URN Prefixes> for intersection of
		// authenticationData
		Map<SNAA, Set<UsernameUrnPrefixPair>> intersectionPrefixSet = newHashMap();

		for (UsernameUrnPrefixPair pair : usernameURNPrefixPairs) {

			// check if federator federates the urn prefix found in the authentication triple
			if (!federationManager.servesUrnPrefix(pair.getUrnPrefix())) {
				throw createSNAAFault("No endpoint known for URN prefix " + pair.getUrnPrefix());
			}

			final SNAA endpoint = federationManager.getEndpointByUrnPrefix(pair.getUrnPrefix());

			Set<UsernameUrnPrefixPair> set = intersectionPrefixSet.get(endpoint);
			if (set == null) {
				set = newHashSet();
				intersectionPrefixSet.put(endpoint, set);
			}
			set.add(pair);
		}

		return intersectionPrefixSet;
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(final List<AuthenticationTriple> authenticationData)
			throws AuthenticationFault_Exception, SNAAFault_Exception {

		Map<SNAA, Set<AuthenticationTriple>> intersectionPrefixSet = getIntersectionPrefixSetAT(authenticationData);

		Set<Future<List<SecretAuthenticationKey>>> futures = new HashSet<Future<List<SecretAuthenticationKey>>>();

		for (SNAA snaa : intersectionPrefixSet.keySet()) {

			final ArrayList<AuthenticationTriple> atList = newArrayList(intersectionPrefixSet.get(snaa));
			final AuthenticationCallable callable = new AuthenticationCallable(snaa, atList);

			futures.add(executorService.submit(callable));
		}

		final List<SecretAuthenticationKey> resultSet = newArrayList();

		for (Future<List<SecretAuthenticationKey>> future : futures) {
			try {
				resultSet.addAll(future.get());
			} catch (Exception e) {
				SNAAFault exception = new SNAAFault();
				exception.setMessage(e.getMessage());
				throw new SNAAFault_Exception(e.getMessage(), exception, e);
			}
		}

		return resultSet;

	}

	@Override
	public AuthorizationResponse isAuthorized(final List<UsernameNodeUrnsMap> usernameNodeUrnsMapList,
											  final Action action) throws SNAAFault_Exception {

		if (usernameNodeUrnsMapList == null || action == null) {
			throw createSNAAFault("Arguments must not be null!");
		}

		AuthorizationResponse response = new AuthorizationResponse();
		response.setAuthorized(true);
		response.setMessage("");

//		Map<String, Set<UsernameUrnPrefixPair>> intersectionPrefixSet =
//				getIntersectionPrefixSetUPP(usernames);

		Set<Future<AuthorizationResponse>> futures = new HashSet<Future<AuthorizationResponse>>();

		for (UsernameNodeUrnsMap usernameNodeUrnsMap : usernameNodeUrnsMapList) {

			final NodeUrnPrefix urnPrefix = usernameNodeUrnsMap.getUsername().getUrnPrefix();
			final SNAA snaa = federationManager.getEndpointByUrnPrefix(urnPrefix);

			IsAuthorizedCallable callable = new IsAuthorizedCallable(snaa, newArrayList(usernameNodeUrnsMap), action);

			futures.add(executorService.submit(callable));
		}

		for (Future<AuthorizationResponse> future : futures) {
			try {
				AuthorizationResponse authorizationResponse = future.get();
				response.setMessage(response.getMessage() + "; " + authorizationResponse.getMessage());
				response.setAuthorized(response.isAuthorized() && authorizationResponse.isAuthorized());
				response.getPerNodeUrnAuthorizationResponses().addAll(
						authorizationResponse.getPerNodeUrnAuthorizationResponses()
				);
			} catch (InterruptedException e) {
				throw createSNAAFault(e.getMessage());
			} catch (ExecutionException e) {
				throw createSNAAFault(e.getMessage());
			}
		}

		return response;
	}

	@Override
	public List<ValidationResult> isValid(final List<SecretAuthenticationKey> secretAuthenticationKeys)
			throws SNAAFault_Exception {

		try {

			checkNotNull(secretAuthenticationKeys, "SecretAuthenticationKey list must not be null!");

			final Set<Future<List<ValidationResult>>> futures = newHashSet();

			for (SecretAuthenticationKey secretAuthenticationKey : secretAuthenticationKeys) {

				final SNAA snaa = federationManager.getEndpointByUrnPrefix(secretAuthenticationKey.getUrnPrefix());
				final IsValidCallable callable = new IsValidCallable(snaa, newArrayList(secretAuthenticationKey));

				futures.add(executorService.submit(callable));
			}

			final List<ValidationResult> result = newArrayList();

			for (Future<List<ValidationResult>> future : futures) {
				result.addAll(future.get());
			}

			return result;

		} catch (Exception e) {
			throw createSNAAFault(e.getMessage());
		}
	}
}
