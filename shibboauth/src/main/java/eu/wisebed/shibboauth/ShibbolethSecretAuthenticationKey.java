package eu.wisebed.shibboauth;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import java.io.Serializable;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 08.09.2010
 * Time: 13:12:42
 * To change this template use File | Settings | File Templates.
 */
public class ShibbolethSecretAuthenticationKey implements Serializable {
    private List<LinkedHashMap<String, Object>> cookieMaps;
    private String secretAuthenticationKey;

    public ShibbolethSecretAuthenticationKey(String secretAuthenticationKey, List<Cookie> cookies){
        this.secretAuthenticationKey = secretAuthenticationKey;
        setCookieMaps(cookies);
    }

    private void setCookieMaps(List<Cookie> cookies) {
       cookieMaps = new LinkedList<LinkedHashMap<String, Object>>();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                LinkedHashMap<String, Object> cookieMap = new LinkedHashMap<String, Object>();
                cookieMap.put("name", cookie.getName());
                cookieMap.put("value", cookie.getValue());
                cookieMap.put("version", cookie.getVersion());
                cookieMap.put("domain", cookie.getDomain());
                cookieMap.put("path", cookie.getPath());
                cookieMap.put("expiryDate", cookie.getExpiryDate());
                cookieMap.put("isSecure", cookie.isSecure());
                cookieMaps.add(cookieMap);
            }
        }
    }

    public List<Cookie> getCookies() {
        List<Cookie> cookieList = new LinkedList<Cookie>();
        for (LinkedHashMap<String, Object> map : cookieMaps) {
            BasicClientCookie cookie = new BasicClientCookie((String) map.get("name"), (String) map.get("value"));
            cookie.setVersion((Integer) map.get("version"));
            cookie.setDomain((String) map.get("domain"));
            cookie.setPath((String) map.get("path"));
            cookie.setExpiryDate((Date) map.get("expiryDate"));
            cookie.setSecure((Boolean) map.get("isSecure"));
            cookieList.add(cookie);
        }
        return cookieList;
    }

    public String getSecretAuthenticationKey() {
        return secretAuthenticationKey;
    }
}
