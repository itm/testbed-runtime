package de.uniluebeck.itm.tr.federator.snaa;

import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.api.v3.snaa.ValidationResult;

import java.util.List;
import java.util.concurrent.Callable;

class IsValidCallable implements Callable<List<ValidationResult>> {

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