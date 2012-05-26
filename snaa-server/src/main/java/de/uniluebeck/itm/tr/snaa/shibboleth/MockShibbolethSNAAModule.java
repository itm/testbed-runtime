package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.AbstractModule;
import eu.wisebed.shibboauth.IShibbolethAuthenticator;
import eu.wisebed.shibboauth.MockShibbolethAuthenticator;

public class MockShibbolethSNAAModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(IShibbolethAuthenticator.class).to(MockShibbolethAuthenticator.class);
    }
}
