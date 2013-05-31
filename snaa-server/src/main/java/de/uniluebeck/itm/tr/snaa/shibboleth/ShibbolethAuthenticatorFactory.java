package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.name.Named;

public interface ShibbolethAuthenticatorFactory {

	ShibbolethAuthenticator create(@Named("username") String username, @Named("password") String password);

}
