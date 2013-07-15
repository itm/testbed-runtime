package de.uniluebeck.itm.tr.snaa.jaas;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;

public interface LoginContextFactory {

	LoginContext create(CallbackHandler callbackHandler);

}
