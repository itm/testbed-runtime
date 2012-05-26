package eu.wisebed.shibboauth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 09.11.2010
 * Time: 12:58:47
 * To change this template use File | Settings | File Templates.
 */

public class MockShibbolethAuthenticator implements IShibbolethAuthenticator {
    private String pageContent = "123456";

    @Override
    public void authenticate() throws Exception {
        //do nothing
    }

    @Override
    public Map<String, List<Object>> isAuthorized(List<Cookie> cookies) throws Exception {
        return new HashMap<String, List<Object>>();
    }

    @Override
    public void setUrl(String secretAuthenticationKeyUrl) {
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
        BasicClientCookie cookie = new BasicClientCookie("_shibsession_test","test");
        cookie.setDomain("wisebed2.itm.uni-luebeck.de");
        BasicCookieStore cookieStore = new BasicCookieStore();
        cookieStore.addCookie(cookie);
        return cookieStore;
    }

    @Override
    public void setProxy(String host, int port) {
        //do nothing
    }
}
