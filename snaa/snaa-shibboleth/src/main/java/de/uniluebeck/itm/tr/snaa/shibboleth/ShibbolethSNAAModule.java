package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import eu.wisebed.shibboauth.IShibbolethAuthenticator;
import eu.wisebed.shibboauth.ShibbolethAuthenticator;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 04.11.2010
 * Time: 18:12:33
 * To change this template use File | Settings | File Templates.
 */
public class ShibbolethSNAAModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IShibbolethAuthenticator.class).to(ShibbolethAuthenticator.class);
    }
}
