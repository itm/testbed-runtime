package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.AbstractModule;
import eu.wisebed.shibboauth.IShibbolethAuthenticator;
import eu.wisebed.shibboauth.ShibbolethAuthenticator;

public class ShibbolethSNAAModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IShibbolethAuthenticator.class).to(ShibbolethAuthenticator.class);
    }
}
