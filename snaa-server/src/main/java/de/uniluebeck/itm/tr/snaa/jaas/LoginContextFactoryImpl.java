package de.uniluebeck.itm.tr.snaa.jaas;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import static com.google.common.base.Throwables.propagate;

public class LoginContextFactoryImpl implements LoginContextFactory {

	private final SNAAConfig snaaConfig;

	@Inject
	public LoginContextFactoryImpl(final SNAAConfig snaaConfig) {
		this.snaaConfig = snaaConfig;
	}

	@Override
	public LoginContext create(final CallbackHandler callbackHandler) {
		try {
			return new LoginContext(snaaConfig.getJaasLoginModule(), callbackHandler);
		} catch (LoginException e) {
			throw propagate(e);
		}
	}
}
