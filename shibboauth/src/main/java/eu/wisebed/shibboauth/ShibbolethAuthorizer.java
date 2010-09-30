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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * --- OLD ---
 * moved into ShibbolethAuthenticator
 */

public class ShibbolethAuthorizer {

    private DefaultHttpClient httpClient;
    private String url;
    private String secretAuthenticationKey;
    private List<Cookie> cookies;
    private HttpGet httpget;
    private BasicHttpContext localContext;
    private HttpHost target;
    private HttpResponse response;
    private HttpUriRequest req;
    private URL finalURL;
    private String responseHtml;
    private String proxyHost = null;
    private int proxyPort = -1;
    private String sessionURL;
    private String authenticationPageContent;

    /**
     * @throws Exception
     */
    private void resetState() throws Exception {
        httpClient = Helper.getFakeSSLTolerantClient();
        localContext = new BasicHttpContext();

        if (proxyHost != null && proxyPort >= 0) {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }

        responseHtml = "";
        httpget = null;
        response = null;
        target = null;
        req = null;
        finalURL = null;

        setAuthenticationPageContent(null);
    }

    private CookieStore createCookieStore() throws NoSuchAlgorithmException, KeyManagementException {
        CookieStore cookieStore = new BasicCookieStore();
        for (Cookie cookie : cookies) {
            cookieStore.addCookie(cookie);
        }
        return cookieStore;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCookies(List<Cookie> cookies){
        this.cookies = cookies;
    }

    public void setSecretAuthenticationKey(String secretAuthenticationKey){
        this.secretAuthenticationKey = secretAuthenticationKey;
    }

    private URL doGet(String url, boolean followRedirects) throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, ProtocolException {
        httpget = new HttpGet(url);
        Helper.addUserAgentHeaders(httpget);

        //set the serialized cookies
        localContext.setAttribute(ClientContext.COOKIE_STORE, createCookieStore());
        response = httpClient.execute(httpget, localContext);

        target = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        req = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);

        if (followRedirects)
            followAllRedirects();

        URL finalURL = new URL("" + target + req.getRequestLine().getUri());
        responseHtml = Helper.readBody(response);

        return finalURL;
    }

    private void followAllRedirects() throws ClientProtocolException, IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, ProtocolException {
        Header loc = response.getFirstHeader("Location");

        while (loc != null) {
            URI u = new URI(loc.getValue());

            finalURL = doGet(u.toString(), false);
            loc = response.getFirstHeader("Location");
        }
    }

    public Map<String, List<String>> isAuthorized() throws Exception {
        Map<String, List<String>> authorizeMap = null;
        resetState();
        if (url == null) {
            return authorizeMap;
        }

        // Check if this session is already authenticated
        {
            finalURL = doGet(url, true);
            //check if url is correct
            if (!finalURL.equals(new URL(this.url))) {
                return authorizeMap;
            }
            //check if key is correct
            setAuthenticationPageContent(this.responseHtml);
            if (!this.getAuthenticationPageContent().trim().equals(secretAuthenticationKey)){
                return authorizeMap;
            }
        }

        sessionURL = finalURL.getProtocol() + "://" + finalURL.getHost() + "/Shibboleth.sso/Session";

        // Get session-values
        {
            finalURL = doGet(sessionURL, true);
            String escapedHtml = StringEscapeUtils.escapeHtml(responseHtml);
            String plainText = extractPlainText(escapedHtml);
            authorizeMap = extractSessionValues(plainText);
        }

        return authorizeMap;
    }

    private String extractPlainText(String escapedHtml){
        while(escapedHtml.lastIndexOf("&lt;") != -1){
            int begin = escapedHtml.lastIndexOf("&lt;");
            int end = escapedHtml.lastIndexOf("&gt;") + 3;
            StringBuffer sb = new StringBuffer();
            for (int i=0;i<escapedHtml.length();i++){
                if (i>=begin && i<= end) continue;
                sb.append(escapedHtml.charAt(i));
            }
            escapedHtml = sb.toString();
        }
        return escapedHtml;
    }

    private Map<String, List<String>> extractSessionValues(String text) throws IOException {
        HashMap<String, List<String>> map = new HashMap<String, List<String>>();
        BufferedReader br = new BufferedReader(new StringReader(text));
        for(String s = br.readLine(); s != null; s = br.readLine()){
            if (!s.contains(":")) continue;
            int split = s.indexOf(":");

            String key = s.subSequence(0, split).toString();
            String value = s.subSequence(split + 2, s.length()).toString();
            LinkedList<String> values = new LinkedList<String>();
            if (map.containsKey(key)) {
                values = (LinkedList<String>) map.remove(key);
            }
            values.add(value);
            map.put(key, values);            
        }
        return map;
    }

    private void setAuthenticationPageContent(String authenticationPageContent) {
        this.authenticationPageContent = authenticationPageContent;
    }

    public String getAuthenticationPageContent(){
        return this.authenticationPageContent;
    }
    
}
