package eu.wisebed.shibboauth;

import org.apache.http.cookie.Cookie;

import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 04.11.2010
 * Time: 18:10:10
 * To change this template use File | Settings | File Templates.
 */
public interface IShibbolethAuthenticator {
    String authenticate() throws Exception;

    Map<String, List<Object>> isAuthorized(List<Cookie> cookies) throws Exception;
}
