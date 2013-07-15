package de.uniluebeck.itm.tr.federator.snaa;

import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.snaa.AuthenticationTriple;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.List;
import java.util.concurrent.Callable;

class AuthenticationCallable implements Callable<List<SecretAuthenticationKey>> {

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