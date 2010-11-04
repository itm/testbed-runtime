import com.google.inject.AbstractModule;
import eu.wisebed.shibboauth.IShibbolethAuthenticator;

public class MockShibbolethSNAAModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(IShibbolethAuthenticator.class).to(MockShibbolethSNAAImpl.class);
    }
}
