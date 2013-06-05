package de.uniluebeck.itm.tr.snaa.jaas;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.snaa.SNAAProperties;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import static com.google.common.base.Throwables.propagate;

public class LoginContextFactoryImpl implements LoginContextFactory {

	private final String jaasLoginModule;

	@Inject
	public LoginContextFactoryImpl(@Named(SNAAProperties.JAAS_LOGINMODULE) final String jaasLoginModule) {
		this.jaasLoginModule = jaasLoginModule;
	}

	@Override
	public LoginContext create(final CallbackHandler callbackHandler) {
		try {
			return new LoginContext(jaasLoginModule, callbackHandler);
		} catch (LoginException e) {
			throw propagate(e);
		}
	}
}
