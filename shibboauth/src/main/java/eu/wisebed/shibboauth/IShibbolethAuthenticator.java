package eu.wisebed.shibboauth;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import java.util.List;
import java.util.Map;

public interface IShibbolethAuthenticator {
    
    void authenticate() throws Exception;

    Map<String, List<Object>> isAuthorized(List<Cookie> cookies) throws Exception;

    void setUrl(String secretAuthenticationKeyUrl);

    void setUsernameAtIdpDomain(String username) throws Exception;

    void setPassword(String password);

    boolean isAuthenticated();

    String getAuthenticationPageContent();

    CookieStore getCookieStore();

    public void setProxy(String host, int port);

}
