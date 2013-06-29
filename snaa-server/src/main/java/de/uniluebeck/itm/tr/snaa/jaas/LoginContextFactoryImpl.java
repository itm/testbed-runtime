package de.uniluebeck.itm.tr.snaa.jaas;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import static com.google.common.base.Throwables.propagate;

public class LoginContextFactoryImpl implements LoginContextFactory {

	private final SNAAServiceConfig snaaServiceConfig;

	@Inject
	public LoginContextFactoryImpl(final SNAAServiceConfig snaaServiceConfig) {
		this.snaaServiceConfig = snaaServiceConfig;
	}

	@Override
	public LoginContext create(final CallbackHandler callbackHandler) {
		try {
			return new LoginContext(snaaServiceConfig.getJaasLoginModule(), callbackHandler);
		} catch (LoginException e) {
			throw propagate(e);
		}
	}
}
