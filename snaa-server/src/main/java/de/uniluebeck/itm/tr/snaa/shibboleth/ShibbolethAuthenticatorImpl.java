/*
 * Copyright (C) 2010 by Dennis Pfisterer. This is free software; you can redistribute it and/or modify it under the
 * terms of the BSD License. Refer to the licence.txt file in the root of the source tree for further details.
 * Copyright (C) 2011 by Pierre Roux: modified under same license.
 */
package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.Inject;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ShibbolethAuthenticatorImpl implements ShibbolethAuthenticator {

	private static final Logger log = LoggerFactory.getLogger(ShibbolethAuthenticatorImpl.class);

	private static final String RELAY_STATE = "";

	private static final String SAML_REQUEST = "";

	private static final String OPEN_HTML_TAG = "&lt;";

	private static final String CLOSE_HTML_TAG = "&gt;";

	private final ShibbolethAuthenticatorConfig config;

	private String username;

	private String idpDomain;

	private String password;

	private DefaultHttpClient httpClient;

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

	private List<Cookie> cookies;

	@Inject
	public ShibbolethAuthenticatorImpl(final ShibbolethAuthenticatorConfig config) {
		this.config = config;
	}

	@Override
	public void setUserAtIdpDomain(final String userAtIdpDomain) {

		int atIndex = userAtIdpDomain.indexOf('@');
		if (atIndex == -1) {
			throw new IllegalArgumentException("Username must be like \"username@idphost\"");
		}

		this.username = userAtIdpDomain.substring(0, atIndex);
		this.idpDomain = userAtIdpDomain.substring(atIndex + 1);
	}

	@Override
	public void setPassword(final String password) {
		this.password = password;
	}

	/**
	 * @throws Exception
	 */
	private void resetState() throws Exception {
		log.debug("Resetting internal state.");
		httpClient = ShibbolethAuthenticatorHelper.getFakeSSLTolerantClient();
		localContext = new BasicHttpContext();

		if (config.getProxy() != null) {
			log.debug("Using proxy: " + config.getProxy());
			HttpHost proxy = new HttpHost(config.getProxy().getHostText(), config.getProxy().getPort());
			httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
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
		authenticationPageContent = null;

		log.debug("Current settings: {}", config);
	}

	/**
	 * @throws Exception
	 */
	@Override
	public void authenticate() throws Exception {
		resetState();

		// Sanity checks
		{
			StringBuilder sb = new StringBuilder("The following conditions are not satisfied: ");
			boolean ok = true;

			if (config.getUrl() == null || config.getUrl().length() == 0) {
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
				log.error(sb.toString());
				throw new Exception(sb.toString());
			}
		}

		// Check if this session is already authenticated (e.g., because the correct cookies were set)
		{
			URL finalURL = doGet(config.getUrl(), true);
			if (finalURL.equals(new URL(config.getUrl()))) {
				log.debug("Authentication succeeded, got the final page " + finalURL);
				log.info("Authentication succeeded");
				setAuthenticated(true);
			}
		}

		Collection<URL> wayfURLs = getWayfUrls();
		URL idp = getBestIdp(wayfURLs);

		// Post form to be redirected to IDP
		{
			performIdpRequest(idp);
		}

		// Detect if Shibboleth 2.x. If yes fill in the login form
		boolean shibbo2x;
		{

			URL currentPage = new URL("" + target + req.getRequestLine().getUri());
			URL actionUrl = ShibbolethAuthenticatorHelper.getActionURL(currentPage, responseHtml);

			// Set the form values and add existing (hidden) fields
			List<NameValuePair> formParams = new ArrayList<NameValuePair>();
			formParams.add(new BasicNameValuePair("RelayState", RELAY_STATE));
			formParams.add(new BasicNameValuePair("SAMLRequest", SAML_REQUEST));
			formParams = ShibbolethAuthenticatorHelper.extractFormValues(responseHtml, formParams);
			shibbo2x = false;
			if (ShibbolethAuthenticatorHelper
					.getValue(formParams, "RelayState") != null && ShibbolethAuthenticatorHelper
					.getValue(formParams, "SAMLRequest") != null) {
				shibbo2x = true;
				log.debug("Form action: " + actionUrl);
				doPost(actionUrl.toURI(), formParams, true);
			}

		}

		// Fill in the login form
		{

			URL currentPage = new URL("" + target + req.getRequestLine().getUri());
			URL actionUrl = ShibbolethAuthenticatorHelper.getActionURL(currentPage, responseHtml);

			// Set the form values and add existing (hidden) fields
			List<NameValuePair> formParams = new ArrayList<NameValuePair>();

			if (shibbo2x) {
				formParams.add(new BasicNameValuePair("j_username", username));
				formParams.add(new BasicNameValuePair("j_password", password));
				formParams = ShibbolethAuthenticatorHelper.extractFormValues(responseHtml, formParams);

				if (ShibbolethAuthenticatorHelper
						.getValue(formParams, "j_username") == null || ShibbolethAuthenticatorHelper
						.getValue(formParams, "j_password") == null) {
					log.error("Did not get a valid login form. Aborting.");
					throw new Exception("Did not get a valid login form (2.x). Aborting.");
				}
			} else {
				formParams.add(new BasicNameValuePair("username", username));
				formParams.add(new BasicNameValuePair("password", password));
				formParams = ShibbolethAuthenticatorHelper.extractFormValues(responseHtml, formParams);

				if (ShibbolethAuthenticatorHelper
						.getValue(formParams, "username") == null || ShibbolethAuthenticatorHelper
						.getValue(formParams, "password") == null) {
					log.error("Did not get a valid login form. Aborting.");
					throw new Exception("Did not get a valid login form. Aborting.");
				}
			}

			log.debug("Form action: " + actionUrl);
			doPost(actionUrl.toURI(), formParams, true);
		}

		// Now submit the onLoad Form with all the SAML data
		{
			List<NameValuePair> formParams = ShibbolethAuthenticatorHelper.extractFormValues(responseHtml, null);
			URL currentPage = new URL("" + target + req.getRequestLine().getUri());
			URL actionUrl = ShibbolethAuthenticatorHelper.getActionURL(currentPage, responseHtml);
			doPost(actionUrl.toURI(), formParams, true);
		}

		authenticationPageContent = responseHtml;

		// Finally check, if we have arrived on the desired page
		if (finalURL.equals(new URL(config.getUrl()))) {
			log.debug("Authentication succeeded, got the final page {}", config.getUrl());
			log.info("Authentication succeeded");
			setAuthenticated(true);
		} else {
			log.debug("Authentication failed, did not read the desired page {} but ended up at {}", config.getUrl(),
					finalURL
			);
			log.error("Authentication failed");
			setAuthenticated(false);
		}
	}

	private void performIdpRequest(URL idp)
			throws URISyntaxException, IOException, NoSuchAlgorithmException, KeyManagementException {
		URL currentPage = new URL(target + new URI(req.getRequestLine().getUri()).getPath());
		URL formURL = ShibbolethAuthenticatorHelper.getActionURL(currentPage, responseHtml);
		log.debug("Posting to be redirected to IDP. URL is " + formURL);

		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("user_idp", idp.toString()));

		doPost(formURL.toURI(), formParams, true);

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

		this.cookies = cookies;

		Map<String, List<Object>> authorizeMap = null;
		if (config.getUrl() == null) {
			return authorizeMap;
		}

		// Check if this session is already authenticated
		{
			finalURL = doGet(config.getUrl(), true);
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
			if (!finalURL.equals(new URL(config.getUrl()))) {
				return authorizeMap;
			}
		}

		final String sessionURL = finalURL.getProtocol() + "://" + finalURL.getHost() + "/Shibboleth.sso/Session";

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
		while (escapedHtml.lastIndexOf(OPEN_HTML_TAG) != -1) {
			int begin = escapedHtml.lastIndexOf(OPEN_HTML_TAG);
			int end = escapedHtml.lastIndexOf(CLOSE_HTML_TAG) + (CLOSE_HTML_TAG.length() - 1);

			StringBuilder sb = new StringBuilder();
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

	public void checkForTimeout() throws Exception {
		log.info("Rechecking authentication session");
		if (!isAuthenticated()) {
			log.error("Not authenticated yet, please invoke authenticate() first.");
			throw new Exception("Not authenticated yet, please invoke authenticate() first.");
		}
		setAuthenticated(areCookiesValid(cookies));
	}

	public boolean areCookiesValid(List<Cookie> cookies) throws Exception {

		resetState();
		this.cookies = cookies;

		URL finalURL = doGet(config.getUrl(), false);
		//responseHtml = SNAAHelper.readBody(response);
		log.debug("Status line: " + response.getStatusLine());

		// Finally check, if we have arrived on the desired page
		if (finalURL.equals(new URL(config.getUrl()))) {
			log.info("Authentication still valid, got the final page {}", config.getUrl());
			return true;
		} else {
			log.debug("Authentication invalidated, did not read the final page {} but ended up at ", config.getUrl(),
					finalURL
			);
			return false;
		}
	}

	private URL doGet(String url, boolean followRedirects)
			throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
		log.debug("Fetching " + url);
		httpget = new HttpGet(url);
		ShibbolethAuthenticatorHelper.addUserAgentHeaders(httpget);

		if (cookies != null) {
			localContext.setAttribute(ClientContext.COOKIE_STORE, createCookieStore());
		}

		response = httpClient.execute(httpget, localContext);
		target = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
		req = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);

		if (followRedirects) {
			followAllRedirects();
		}

		URL finalURL = new URL("" + target + req.getRequestLine().getUri());
		log.debug("Final URL: " + finalURL);
		responseHtml = ShibbolethAuthenticatorHelper.readBody(response);

		return finalURL;
	}

	private CookieStore createCookieStore() throws NoSuchAlgorithmException, KeyManagementException {
		CookieStore cookieStore = new BasicCookieStore();
		for (Cookie cookie : cookies) {
			cookieStore.addCookie(cookie);
		}
		return cookieStore;
	}

	private void doPost(URI uri, List<NameValuePair> formParams, boolean isFollowAllRedirects)
			throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
		httppost = new HttpPost(uri);
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams);
		httppost.setEntity(entity);
		ShibbolethAuthenticatorHelper.addUserAgentHeaders(httppost);

		response = httpClient.execute(httppost, localContext);
		target = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
		req = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);

		finalURL = new URL("" + target + req.getRequestLine().getUri());
		responseHtml = ShibbolethAuthenticatorHelper.readBody(response);

		log.debug("POST: Posting to " + uri);
		log.debug("POST: Final URL: " + finalURL);
		log.debug("POST: Status line: " + response.getStatusLine());
		log.debug("POST: Response: " + responseHtml);

		if (isFollowAllRedirects) {
			log.debug("Following all redirects");
			followAllRedirects();
		}

	}

	private Collection<URL> getWayfUrls() throws Exception {

		// Sanity checks
		{
			StringBuilder sb = new StringBuilder("The following conditions are not satisfied: ");
			boolean ok = true;

			if (config.getUrl() == null || config.getUrl().length() == 0) {
				sb.append("Url is empty or null. ");
				ok = false;
			}
			if (!ok) {
				log.error(sb.toString());
				throw new Exception(sb.toString());
			}
		}

		// Get IDPs and next Form from WAYF
		{
			URL finalURL = doGet(config.getUrl(), false);
			log.debug("WAYF: Fetching URLs from " + finalURL);
			log.debug("WAYF: Status line: " + response.getStatusLine());
			log.debug("WAYF, response html: " + response.getStatusLine());
		}

		// List<Cookie> cookies = httpClient.getCookieStore().getCookies(); System.out.println("Initial set of " +
		// cookies.size() + " cookies:"); for (Cookie c : cookies) System.out.println("  - " + c);

		LinkedList<URL> idpURLs = ShibbolethAuthenticatorHelper.extractSelectOptionURLs(responseHtml);

		for (URL u : idpURLs) {
			log.debug("WAYF: Url: " + u);
		}

		return idpURLs;
	}

	private URL getBestIdp(Collection<URL> idps) throws Exception {
		if (idps == null || idps.size() <= 0) {
			log.error("No IDPs available, unable to select the best one.");
			throw new Exception("No IDPs available, unable to select the best one.");
		}

		log.debug("Selecting best IDP for user {} from {} available ids", username, idps.size());

		URL chosenIDP = null;
		for (URL url : idps) {
			if (url.getHost().equalsIgnoreCase(idpDomain.trim())) {
				log.info("Using idp: " + url);
				chosenIDP = url;
			}
		}

		if (chosenIDP == null) {
			log.error("No IDP available for idp domain: {}", idpDomain);
			throw new Exception("No IDP available for idp domain: " + idpDomain);
		}

		log.info("Selected IDP for user {}  is {}", username, chosenIDP);
		return chosenIDP;
	}

	private void followAllRedirects()
			throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
		Header loc = response.getFirstHeader("Location");

		while (loc != null) {
			URI u = new URI(loc.getValue());
			log.debug("Redirect header: " + loc.getValue());
			log.debug("Redirect URI   : " + u);

			finalURL = doGet(u.toString(), false);
			loc = response.getFirstHeader("Location");
		}
	}

	public CookieStore getCookieStore() {
		return httpClient.getCookieStore();
	}

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
}
