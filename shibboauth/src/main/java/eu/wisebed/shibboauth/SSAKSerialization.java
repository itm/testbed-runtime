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
import org.apache.log4j.Logger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SSAKSerialization {
    private static class CookieProperties {
        private static String sp_name = "sp_name";
        private static String sp_value = "sp_value";
        private static String sp_domain = "sp_domain";

        private static String idp_name = "idp_name";
        private static String idp_value = "idp_value";
        private static String idp_domain = "idp_domain";
    }

    private static String _shibSession_ = "_shibsession_";
    private static String _idp_session = "_idp_session";

    private static final Logger log = Logger.getLogger(SSAKSerialization.class);

    private static Map<String, String> cookieListToMap(List<Cookie> cookies) throws CookieNotFoundException {
        Cookie shibSessionCookie = null;
        Cookie idpSessionCookie = null;
		for (Cookie cookie : cookies) {
            if (cookie.getName().startsWith(_shibSession_)) {
                shibSessionCookie = cookie;
            }
            if (cookie.getName().startsWith(_idp_session)) {
                idpSessionCookie = cookie;
            }
        }

        if (shibSessionCookie == null) {
            throw new CookieNotFoundException("Could not find '_shibsession_'-cookie");
        }

        //fill cookie-map
        Map<String, String> cookieMap = new HashMap<String, String>();

		if (shibSessionCookie != null) {
			cookieMap.put(CookieProperties.sp_name, shibSessionCookie.getName());
			cookieMap.put(CookieProperties.sp_value, shibSessionCookie.getValue());
			cookieMap.put(CookieProperties.sp_domain, shibSessionCookie.getDomain());
		}

		if (idpSessionCookie != null) {
			cookieMap.put(CookieProperties.idp_name, idpSessionCookie.getName());
			cookieMap.put(CookieProperties.idp_value, idpSessionCookie.getValue());
			cookieMap.put(CookieProperties.idp_domain, idpSessionCookie.getDomain());
		}

        return cookieMap;
    }

    public static String serialize(List<Cookie> cookies) throws CookiePropertyNotFoundException, CookieNotFoundException {
        Map<String, String> cookieMap = cookieListToMap(cookies);
        if (!(cookieMap.containsKey(CookieProperties.sp_name) &&
                cookieMap.containsKey(CookieProperties.sp_value) &&
                cookieMap.containsKey(CookieProperties.sp_domain))) {
            throw new CookiePropertyNotFoundException("Cookie-Property '" + "' not found in cookie-map");
        }

        StringBuffer out = new StringBuffer();

        //make String out of Cookie values
        // format:
        // (name=value;)*name=value@domain
        {
            out.append(cookieMap.get(CookieProperties.sp_name)).append("=").append(cookieMap.get(CookieProperties.sp_value)).append("@");
            out.append(cookieMap.get(CookieProperties.sp_domain)).append(";");
            out.append(cookieMap.get(CookieProperties.idp_name)).append("=").append(cookieMap.get(CookieProperties.idp_domain)).append(":");
            out.append(cookieMap.get(CookieProperties.idp_value)).append(";");
        }
        
        log.debug("Serialize cookie store to =="+out.toString());
        
        return out.toString();
    }

    public static List<Cookie> deserialize(String serializedString) throws NotDeserializableException {

    	log.debug("Deserialize string to cookie store =="+serializedString);
    	
    	// old version was 
    	//		key=value;key=value;key=value@domain
    	// new version is 
    	//		key=value@domain;key=value@domain;key=value@domain
    	
    	
        //remove whitespaces
        serializedString = serializedString.replaceAll(" ", "");
        //make Cookies out of serialized cookie-string
        // format:
        // (name=value;)*name=value@domain

        //getting domain-value
//        String[] lastEntryValues = serializedString.split("@");
//        if (lastEntryValues.length != 2) {
//            throw new NotDeserializableException("Could not extract domain-value while de-serializing '" + serializedString + "'");
//        }
//        

        //getting other key,value pairs
        String[] cookieValues = serializedString.split(";");
        if (cookieValues.length == 0) {
            throw new NotDeserializableException("Could not de-serialize empty String");
        }
        
        String domains[] = cookieValues[cookieValues.length-1].split("@");
        String domain = "";
        String name = "";
        String value = "";
        if (domains.length == 2)
        	domain = domains[1];

        List<Cookie> cookies = new LinkedList<Cookie>();

        for (String data : cookieValues) { 
        	
        	name= "";
        	value="";
        	
            if (data.startsWith(_shibSession_)){//name=value@domain
            	String[] key_value = data.split("@");
                
                if (key_value.length == 2)
                	domain = key_value[1];
                
                String[] pair = key_value[0].split("=");
                if (pair.length != 2) {
                    throw new NotDeserializableException("Could de-serialize key-value-pair '" + value + "'");
                }
                name = pair[0];
                value = pair[1];
            }
            
            if(data.startsWith(_idp_session)){//name=domain:value
            	 String[] key_value = data.split(":");
                 
                 if (key_value.length == 2)
                 	value = key_value[1];
                 
                 String[] pair = key_value[0].split("=");
                 if (pair.length != 2) {
                     throw new NotDeserializableException("Could de-serialize key-value-pair '" + value + "'");
                 }
                 name = pair[0];
                 domain = pair[1];
             }
            
            if (!((name.equals("")) && (value.equals("")))){
            	log.debug("NEW COOKIE "+name+" "+value+" "+domain);
            	BasicClientCookie cookie = new BasicClientCookie(name, value);
            	cookie.setDomain(domain);
            	cookie.setVersion(0);
            	cookie.setPath("/");
            	cookies.add(cookie);
            }
            }
           
        return cookies;
    }

    private static class CookieNotFoundException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public CookieNotFoundException(String s) {
            super(s);
        }
    }

    private static class CookiePropertyNotFoundException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public CookiePropertyNotFoundException(String s) {
            super(s);
        }
    }

    private static class NotDeserializableException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public NotDeserializableException(String s) {
            super(s);
        }
    }
}
