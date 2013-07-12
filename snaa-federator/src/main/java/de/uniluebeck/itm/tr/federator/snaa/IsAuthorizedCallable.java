package de.uniluebeck.itm.tr.federator.snaa;

import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthorizationResponse;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.List;
import java.util.concurrent.Callable;

class IsAuthorizedCallable implements Callable<AuthorizationResponse> {

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