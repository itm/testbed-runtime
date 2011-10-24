/*
 * Copyright (C) 2010 by Dennis Pfisterer. This is free software; you can redistribute it and/or modify it under the
 * terms of the BSD License. Refer to the licence.txt file in the root of the source tree for further details.
 * Copyright (C) 2011 by Pierre Roux: modified under same license.
 */
package eu.wisebed.shibboauth;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ShibbolethAuthenticator implements IShibbolethAuthenticator {

    private static final Logger log = Logger.getLogger(ShibbolethAuthenticator.class);

    private String proxyHost = null;

    private int proxyPort = -1;

    private String url;

    private String username;

    private String password;

    private String jusername;

    private String jpassword;

    private String relaystate;

    private String samlrequest;
    
    private boolean shibbo2x;

    private String idpDomain;

    private DefaultHttpClient httpclient;

    private BasicHttpContext localContext;

    private String responseHtml = "";

    private HttpGet httpget = null;

    private HttpPost httppost = null;

    private HttpResponse response = null;

    private HttpHost target = null;

    private HttpUriRequest req = null;

    private URL finalURL;

    private boolean authenticated;

    private String authenticationPageContent;

    private String sessionURL;

    private List<Cookie> cookies;

    private static final String openHtmlTag = "&lt;";
    private static final String closeHtmlTag = "&gt;";

    /**
     * @throws Exception
     */
    private void resetState() throws Exception {
        log.debug("Resetting internal state.");
        httpclient = Helper.getFakeSSLTolerantClient();
        localContext = new BasicHttpContext();

        if (proxyHost != null && proxyPort >= 0) {
            log.debug("Using proxy: " + proxyHost + ":" + proxyPort);
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }

        responseHtml = "";
        httpget = null;
        httppost = null;
        response = null;
        target = null;
        req = null;
        finalURL = null;
        cookies = null;

        setAuthenticated(false);
        setAuthenticationPageContent(null);

        log.debug("Current settings: user[" + username + "], idpdomain[" + idpDomain + "], url[" + url + "], proxy["
                + proxyHost + ":" + proxyPort + "]");
    }

    /**
     * @throws Exception
     */
    @Override
    public void authenticate() throws Exception {
        resetState();

        // Sanity checks
        {
            StringBuffer sb = new StringBuffer("The following conditions are not satisfied: ");
            boolean ok = true;

            if (url == null || url.length() == 0) {
                sb.append("Url is empty or null. ");
                ok = false;
            }
            if (username == null || username.length() == 0) {
                sb.append("User is empty or null. ");
                ok = false;
            }
            if (idpDomain == null || idpDomain.length() == 0) {
                sb.append("IDP domain is empty or null. ");
                ok = false;
            }
            if (password == null) {
                sb.append("Password is null. ");
                ok = false;
            }
            if (!ok) {
                log.fatal(sb.toString());
                throw new Exception(sb.toString());
            }
        }

        // Check if this session is already authenticated (e.g., because the correct cookies were set)
        {
            URL finalURL = doGet(url, true);
            if (finalURL.equals(new URL(this.url))) {
                log.debug("Authentication suceeded, got the final page " + url);
                log.info("Authentication suceeded");
                setAuthenticated(true);
            }
        }

        Collection<URL> wayfURLs = getWayfUrls();
        URL idp = getBestIdp(wayfURLs);

        // Post form to be redirected to IDP
        {
            performIdpRequest(idp);
        }

        // Detect if Shibboeleth 2.x. If yes fill in the login form
        {
        
            URL currentPage = new URL("" + target + req.getRequestLine().getUri());
            URL actionUrl = Helper.getActionURL(currentPage, responseHtml);

            // Set the form values and add existing (hidden) fields
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("RelayState", relaystate));
            formparams.add(new BasicNameValuePair("SAMLRequest", samlrequest));
            formparams = Helper.extractFormValues(responseHtml, formparams);
            shibbo2x=false;
            if (Helper.getValue(formparams, "RelayState") != null && Helper.getValue(formparams, "SAMLRequest") != null) {
            	    shibbo2x=true;
            	    log.debug("Form action: " + actionUrl);
            	    doPost(actionUrl.toURI(), formparams, true);
            }

        }

        // Fill in the login form
        {
        
            URL currentPage = new URL("" + target + req.getRequestLine().getUri());
            URL actionUrl = Helper.getActionURL(currentPage, responseHtml);

            // Set the form values and add existing (hidden) fields
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            
            if (shibbo2x) {
            	    formparams.add(new BasicNameValuePair("j_username", username));
            	    formparams.add(new BasicNameValuePair("j_password", password));
            	    formparams = Helper.extractFormValues(responseHtml, formparams);
            	    
            	    if (Helper.getValue(formparams, "j_username") == null || Helper.getValue(formparams, "j_password") == null) {
            	    	    log.fatal("Did not get a valid login form. Aborting.");
            	    	    throw new Exception("Did not get a valid login form (2.x). Aborting.");
            	    }
            }
            
            else {
            	    formparams.add(new BasicNameValuePair("username", username));
            	    formparams.add(new BasicNameValuePair("password", password));
            	    formparams = Helper.extractFormValues(responseHtml, formparams);
            	    
            	    if (Helper.getValue(formparams, "username") == null || Helper.getValue(formparams, "password") == null) {
            	    	    log.fatal("Did not get a valid login form. Aborting.");
            	    	    throw new Exception("Did not get a valid login form. Aborting.");
            	    }
            }

            log.debug("Form action: " + actionUrl);
            doPost(actionUrl.toURI(), formparams, true);
        }

        // Now submit the onLoad Form with all the SAML data
        {
            List<NameValuePair> formparams = Helper.extractFormValues(responseHtml, null);
            URL currentPage = new URL("" + target + req.getRequestLine().getUri());
            URL actionUrl = Helper.getActionURL(currentPage, responseHtml);
            doPost(actionUrl.toURI(), formparams, true);
        }

        setAuthenticationPageContent(responseHtml);

        // Finally check, if we have arrived on the desired page
        if (finalURL.equals(new URL(this.url))) {
            log.debug("Authentication suceeded, got the final page " + url);
            log.info("Authentication suceeded");
            setAuthenticated(true);
        } else {
            log.debug("Authentication failed, did not read the desired page " + url + " but ended up at " + finalURL);
            log.fatal("Authentication failed");
            setAuthenticated(false);
        }
    }

    private void performIdpRequest(URL idp) throws URISyntaxException, IOException, NoSuchAlgorithmException, KeyManagementException {
        URL currentPage = new URL(target + new URI(req.getRequestLine().getUri()).getPath());
        URL formURL = Helper.getActionURL(currentPage, responseHtml);
        log.debug("Posting to be redirected to IDP. URL is " + formURL);

        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("user_idp", idp.toString()));

        doPost(formURL.toURI(), formparams, true);

        target = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        req = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);

        URL finalURL = new URL("" + target + req.getRequestLine().getUri());
        log.debug("Final URL: " + finalURL);
        log.debug("Status line: " + response.getStatusLine());
        log.debug("Response: " + responseHtml);
    }

    @Override
    public Map<String, List<Object>> isAuthorized(List<Cookie> cookies) throws Exception {
        resetState();

        setCookies(cookies);

        Map<String, List<Object>> authorizeMap = null;
        if (url == null) {
            return authorizeMap;
        }

        // Check if this session is already authenticated
        {
            finalURL = doGet(url, true);
            //check if final url is the url to be redirected
/*            if (!finalURL.equals(new URL(this.url))) {
                //if not get IdpRequest
                Collection<URL> wayfURLs = getWayfUrls();
                URL idp = getBestIdp(wayfURLs);

                // Post form to be redirected to IDP
                {
                    performIdpRequest(idp);
                }
            }*/

            //check again if final url is the url to be redirected
            //if not there is no valid authorization
            if (!finalURL.equals(new URL(this.url))) {
                return authorizeMap;
            }
        }

        sessionURL = finalURL.getProtocol() + "://" + finalURL.getHost() + "/Shibboleth.sso/Session";

        //get Session values from Sibboleth.sso/Session
        {
            finalURL = doGet(sessionURL, true);
            //transform Html-tags to escape-sequences
            String escapedHtml = StringEscapeUtils.escapeHtml(responseHtml);
            //filter out html-tags as escapedHtml-sequence            
            String plainText = extractPlainText(escapedHtml);
            authorizeMap = extractSessionValues(plainText);
        }

        return authorizeMap;
    }

    private String extractPlainText(String escapedHtml) {
        while (escapedHtml.lastIndexOf(openHtmlTag) != -1) {
            int begin = escapedHtml.lastIndexOf(openHtmlTag);
            int end = escapedHtml.lastIndexOf(closeHtmlTag) + (closeHtmlTag.length() - 1);

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < escapedHtml.length(); i++) {
                if (i >= begin && i <= end) {
                    continue;
                }
                sb.append(escapedHtml.charAt(i));
            }
            escapedHtml = sb.toString();
        }
        return escapedHtml;
    }

    private Map<String, List<Object>> extractSessionValues(String text) throws IOException {
        HashMap<String, List<Object>> map = new HashMap<String, List<Object>>();
        BufferedReader br = new BufferedReader(new StringReader(text));
        for (String s = br.readLine(); s != null; s = br.readLine()) {
            if (!s.contains(":")) {
                continue;
            }
            int splitIndex = s.indexOf(":");

            String key = s.subSequence(0, splitIndex).toString();
            String value = s.subSequence(splitIndex + 2, s.length()).toString();
            LinkedList<Object> values = new LinkedList<Object>();
            if (map.containsKey(key)) {
                values = (LinkedList<Object>) map.remove(key);
            }
            values.add(value);
            map.put(key, values);
        }
        return map;
    }

    /**
     * @throws Exception
     * @throws
     */
    public void checkForTimeout() throws Exception {
        log.info("Rechecking authentication session");
        if (!isAuthenticated()) {
            log.error("Not authenticated yet, please invoke authenticate() first.");
            throw new Exception("Not authenticated yet, please invoke authenticate() first.");
        }

        URL finalURL = doGet(url, false);
        //responseHtml = Helper.readBody(response);
        log.debug("Status line: " + response.getStatusLine());

        // Finally check, if we have arrived on the desired page
        if (finalURL.equals(new URL(this.url))) {
            log.info("Authentication still valid, got the final page " + url);
            setAuthenticated(true);
        } else {
            log
                    .debug("Authentication invalidated, did not read the final page " + url + " but ended up at "
                            + finalURL);
            setAuthenticated(false);
        }
    }

    /**
     * @param url
     * @param followRedirects
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    private URL doGet(String url, boolean followRedirects) throws ClientProtocolException, IOException,
            URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        log.debug("Fetching " + url);
        httpget = new HttpGet(url);
        Helper.addUserAgentHeaders(httpget);

        if (cookies != null) {
            localContext.setAttribute(ClientContext.COOKIE_STORE, createCookieStore());
        }

        response = httpclient.execute(httpget, localContext);
        target = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        req = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);

        if (followRedirects)
            followAllRedirects();

        URL finalURL = new URL("" + target + req.getRequestLine().getUri());
        log.debug("Final URL: " + finalURL);
        responseHtml = Helper.readBody(response);

        return finalURL;
    }

    private CookieStore createCookieStore() throws NoSuchAlgorithmException, KeyManagementException {
        CookieStore cookieStore = new BasicCookieStore();
        for (Cookie cookie : cookies) {
            cookieStore.addCookie(cookie);
        }
        return cookieStore;
    }

    /**
     * @param uri
     * @param formparams
     * @param isFollowAllRedirects
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    private void doPost(URI uri, List<NameValuePair> formparams, boolean isFollowAllRedirects)
            throws ClientProtocolException, IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        httppost = new HttpPost(uri);
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams);
        httppost.setEntity(entity);
        Helper.addUserAgentHeaders(httppost);

        response = httpclient.execute(httppost, localContext);
        target = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        req = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);

        finalURL = new URL("" + target + req.getRequestLine().getUri());
        responseHtml = Helper.readBody(response);

        log.debug("POST: Posting to " + uri);
        log.debug("POST: Final URL: " + finalURL);
        log.debug("POST: Status line: " + response.getStatusLine());
        log.debug("POST: Response: " + responseHtml);

        if (isFollowAllRedirects) {
            log.debug("Following all redirects");
            followAllRedirects();
        }

    }

    /**
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    private Collection<URL> getWayfUrls() throws Exception {

        // Sanity checks
        {
            StringBuffer sb = new StringBuffer("The following conditions are not satisfied: ");
            boolean ok = true;

            if (url == null || url.length() == 0) {
                sb.append("Url is empty or null. ");
                ok = false;
            }
            if (!ok) {
                log.fatal(sb.toString());
                throw new Exception(sb.toString());
            }
        }

        // Get IDPs and next Form from WAYF
        {
            URL finalURL = doGet(url, false);
            log.debug("WAYF: Fetching URLs from " + finalURL);
            log.debug("WAYF: Status line: " + response.getStatusLine());
            log.debug("WAYF, response html: " + response.getStatusLine());
        }

        // List<Cookie> cookies = httpclient.getCookieStore().getCookies(); System.out.println("Initial set of " +
        // cookies.size() + " cookies:"); for (Cookie c : cookies) System.out.println("  - " + c);

        LinkedList<URL> idpURLs = Helper.extractSelectOptionURLs(responseHtml);

        for (URL u : idpURLs)
            log.debug("WAYF: Url: " + u);

        return idpURLs;
    }

    /**
     * @param idps
     * @return
     * @throws Exception
     */
    private URL getBestIdp(Collection<URL> idps) throws Exception {
        if (idps == null || idps.size() <= 0) {
            log.fatal("No IDPs available, unable to select the best one.");
            throw new Exception("No IDPs available, unable to select the best one.");
        }

        log.debug("Selecting best IDP for user " + username + " from " + idps.size() + " available ids");

        URL chosenIDP = null;
        for (URL url : idps) {
            if (url.getHost().equalsIgnoreCase(idpDomain.trim())) {
                log.info("Using idp: " + url);
                chosenIDP = url;
            }
        }

        if (chosenIDP == null) {
            log.fatal("No IDP available for idp domain: " + idpDomain);
            throw new Exception("No IDP available for idp domain: " + idpDomain);
        }

        log.info("Selected IDP for user " + username + " is " + chosenIDP);
        return chosenIDP;
    }

    /**
     * @throws IOException
     * @throws ClientProtocolException
     * @throws URISyntaxException
     */
    private void followAllRedirects() throws ClientProtocolException, IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        Header loc = response.getFirstHeader("Location");

        while (loc != null) {
            URI u = new URI(loc.getValue());
            log.debug("Redirect header: " + loc.getValue());
            log.debug("Redirect URI   : " + u);

            finalURL = doGet(u.toString(), false);
            loc = response.getFirstHeader("Location");
        }
    }


    /**
     * @param user
     * @throws Exception
     */
    public void setUsernameAtIdpDomain(String user) throws Exception {

        int atIndex = user.indexOf('@');
        if (atIndex == -1) {
            log.fatal("Username must be like username@idphost, but is: '" + user + "'");
            throw new Exception("Username must be like username@idphost");
        }

        setUsername(user.substring(0, atIndex));
        setIdpDomain(user.substring(atIndex + 1));
    }

    /**
     * @return
     */
    public CookieStore getCookieStore() {
        return httpclient.getCookieStore();
    }

    /**
     * @return
     * @throws Exception
     */
    public Collection<URL> getIDPs() throws Exception {
        resetState();
        return getWayfUrls();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    private void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getAuthenticationPageContent() {
        return authenticationPageContent;
    }

    private void setAuthenticationPageContent(String authenticationPageContent) {
        this.authenticationPageContent = authenticationPageContent;
    }

    /**
     * @param host
     * @param port
     */
    public void setProxy(String host, int port) {
        this.proxyHost = host;
        this.proxyPort = port;
    }

    public void unsetProxy() {
        proxyHost = null;
        proxyPort = -1;
    }

    public void setUrl(String urlToObtain) {
        this.url = urlToObtain;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setIdpDomain(String idpDomain) {
        this.idpDomain = idpDomain;
    }

    public String getUsername() {
        return username;
    }

    private void setCookies(List<Cookie> cookies) {
        this.cookies = cookies;
    }
}
