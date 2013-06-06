package de.uniluebeck.itm.tr.snaa.shibboleth;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ShibbolethAuthenticator {
    
    void authenticate() throws Exception;

    Map<String, List<Object>> isAuthorized(List<Cookie> cookies) throws Exception;

	boolean areCookiesValid(List<Cookie> cookies) throws Exception;

    boolean isAuthenticated();

    String getAuthenticationPageContent();

    CookieStore getCookieStore();

	Collection<URL> getIDPs() throws Exception;

	void checkForTimeout() throws Exception;

	void setUserAtIdpDomain(String userAtIdpDomain);

	void setPassword(String password);
}
