import eu.wisebed.shibboauth.IShibbolethAuthenticator;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MockShibbolethSNAAImpl implements IShibbolethAuthenticator{
    private String pageContent = "pageContent";
    @Override
    public String authenticate() throws Exception {
        return pageContent;
    }

    @Override
    public Map<String, List<Object>> isAuthorized(List<Cookie> cookies) throws Exception {
        //create List for unit-test-authorization
        List<Object> authorizeList = new LinkedList<Object>();
        
        Map<String, List<Object>> returnMap = new HashMap<String, List<Object>>();
        return null;
    }

    @Override
    public void setUrl(String secretAuthenticationKeyUrl) {
        //do nothing
    }

    @Override
    public void setSecretAuthenticationKey(String secretAuthenticationKey) {
        //do nothing
    }

    @Override
    public void setUsernameAtIdpDomain(String username) throws Exception {
        //do nothing
    }

    @Override
    public void setPassword(String password) {
        //do nothing
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public String getAuthenticationPageContent() {
        return pageContent;
    }

    @Override
    public CookieStore getCookieStore() {
        //TODO implementation
        return null;
    }
}
