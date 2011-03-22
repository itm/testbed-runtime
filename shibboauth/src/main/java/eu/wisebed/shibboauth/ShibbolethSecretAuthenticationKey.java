/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package eu.wisebed.shibboauth;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

public class ShibbolethSecretAuthenticationKey implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private List<LinkedHashMap<String, Object>> cookieMaps;

    public ShibbolethSecretAuthenticationKey(List<Cookie> cookies){
        //get only shibsession cookie
        List<Cookie> shibSessionCookies = new LinkedList<Cookie>();
        for (Cookie cookie : cookies){
            if (cookie.getName().startsWith("_shibsession_")){
                shibSessionCookies.add(cookie);
            }
        }
        setCookieMaps(shibSessionCookies);
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

}
