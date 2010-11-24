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

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import java.util.*;

public class SSAKSerialization {
    private static class CookieProperties {
        private static String name = "name";
        private static String value = "value";
        private static String domain = "domain";
    }

    public static Map<String, String> cookieListToMap(List<Cookie> cookies) throws CookieNotFoundException {
        Cookie shibSessionCookie = null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().startsWith("_shibsession_")) {
                shibSessionCookie = cookie;
                break;
            }
        }

        if (shibSessionCookie == null) {
            throw new CookieNotFoundException("Could not find '_shibsession_'-cookie");
        }

        //fill cookie-map
        Map<String, String> cookieMap = new HashMap<String, String>();
        cookieMap.put(CookieProperties.name, shibSessionCookie.getName());
        cookieMap.put(CookieProperties.value, shibSessionCookie.getValue());
        cookieMap.put(CookieProperties.domain, shibSessionCookie.getDomain());

        return cookieMap;
    }

    public static String serialize(Map<String, String> cookieMap) throws CookiePropertyNotFoundException {
        if (! (cookieMap.containsKey(CookieProperties.name) &&
                cookieMap.containsKey(CookieProperties.value) &&
                        cookieMap.containsKey(CookieProperties.domain)) ) {
            throw new CookiePropertyNotFoundException("Cookie-Property '" + "' not found in cookie-map");
        }

        StringBuffer out = new StringBuffer();

        //make String out of Cookie values
        // format:
        // (name=value;)*name=value@domain
        {
            out.append(cookieMap.get(CookieProperties.name)).append("=").append(cookieMap.get(CookieProperties.value)).append("@");
            out.append(cookieMap.get(CookieProperties.domain));
        }
        return out.toString();
    }

    public static List<Cookie> deserialize(String serializedString) throws NotDeserializableException {
        //make Cookies out of serialized cookie-string
        // format:
        // (name=value;)*name=value,domain

        //getting domain-value
        String[] lastEntryValues = serializedString.split("@");
        if (lastEntryValues.length != 2){
            throw new NotDeserializableException("Could not extract domain-value while deserializing '" + serializedString + "'");
        }
        String domain = lastEntryValues[1];

        //getting other key,value pairs
        String[] cookieValues = lastEntryValues[0].split(";");
        if (cookieValues.length == 0){
            throw new NotDeserializableException("Could not deserialize empty String");
        }

        List<Cookie> cookies = new LinkedList<Cookie>();

        for (String value : cookieValues) {
            String[] pair = value.split("=");
            if (pair.length != 2){
                throw new NotDeserializableException("Could deserialize key-value-pair '" + value + "'");
            }
            BasicClientCookie cookie = new BasicClientCookie(pair[0], pair[1]);
            cookie.setDomain(domain);
            cookies.add(cookie);            
        }
        return cookies;
    }

    private static class CookieNotFoundException extends Exception {
        public CookieNotFoundException(String s) {
            super(s);
        }
    }

    private static class CookiePropertyNotFoundException extends Exception {
        public CookiePropertyNotFoundException(String s) {
            super(s);
        }
    }

    private static class NotDeserializableException extends Exception {
        public NotDeserializableException(String s) {
            super(s);
        }
    }
}
