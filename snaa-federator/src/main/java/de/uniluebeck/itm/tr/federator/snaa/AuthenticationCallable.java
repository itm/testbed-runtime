package de.uniluebeck.itm.tr.federator.snaa;

import eu.wisebed.api.v3.snaa.Authenticate;
import eu.wisebed.api.v3.snaa.AuthenticateResponse;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.concurrent.Callable;

class AuthenticationCallable implements Callable<AuthenticateResponse> {

	private final SNAA snaa;

	private final Authenticate authenticate;

	public AuthenticationCallable(SNAA snaa, Authenticate authenticate) {
		this.snaa = snaa;
		this.authenticate = authenticate;
	}

	@Override
	public AuthenticateResponse call() throws Exception {
		return snaa.authenticate(authenticate);
	}

}