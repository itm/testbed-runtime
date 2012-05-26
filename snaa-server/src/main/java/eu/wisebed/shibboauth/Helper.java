/*
 * Copyright (C) 2010 by Dennis Pfisterer. This is free software; you can redistribute it and/or modify it under the
 * terms of the BSD License. Refer to the licence.txt file in the root of the source tree for further details.
 */

package eu.wisebed.shibboauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.FormControlType;
import net.htmlparser.jericho.FormField;
import net.htmlparser.jericho.FormFields;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Helper {
    private static final Logger log = LoggerFactory.getLogger(Helper.class);

    /**
     * @param response
     * @return
     * @throws IOException
     */
    static String readBody(HttpResponse response) throws IOException {
        StringBuffer sb = new StringBuffer();
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            entity = new BufferedHttpEntity(entity);
            BufferedReader is = new BufferedReader(new InputStreamReader(entity.getContent()));
            for (String s = is.readLine(); s != null; s = is.readLine()) {
                sb.append(s);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * @param html
     * @param replaceParams
     * @return
     */
    static List<NameValuePair> extractFormValues(String html, List<NameValuePair> replaceParams) {
        Source s = new Source(html);

        ArrayList<NameValuePair> formparams = new ArrayList<NameValuePair>();

        FormFields formFields = s.getFormFields();
        for (FormField formField : formFields) {
            String fieldName = formField.getFormControl().getName();
            String existingValue = getValue(replaceParams, fieldName);

            if (existingValue != null) {
                if (fieldName != null && fieldName.contains("passw"))
                    log.debug("Found field [" + fieldName + "] with exitisting (password) value (not shown)");
                else
                    log.debug("Found field [" + fieldName + "] with exitisting value " + existingValue);
                formparams.add(new BasicNameValuePair(fieldName, existingValue));
            } else if (formField.getValues().size() > 0) {
                String value = formField.getFormControl().getValues().get(0);
                formparams.add(new BasicNameValuePair(fieldName, value));

                if (fieldName != null && fieldName.contains("passw"))
                    log.debug("Copy: name[" + fieldName + "]=value[*not shown here*]");
                else
                    log.debug("Copy: name[" + fieldName + "]=value[" + value + "]");
            } else
                log.debug("Ignoring empty field " + formField.getName());

        }

        return formparams;
    }

    /**
     * @param params
     * @param name
     * @return
     */
    static String getValue(List<NameValuePair> params, String name) {
        if (params == null || params.size() == 0) {
            log.debug("Supplied list of NameValuePair is null or empty, returning null");
            return null;
        }

        for (NameValuePair nvp : params)
            if (nvp.getName().equals(name))
                return nvp.getValue();

        return null;

    }

    /**
     * Read out all URLs from the WAYF form
     */
    static LinkedList<URL> extractSelectOptionURLs(String html) {
        LinkedList<URL> l = new LinkedList<URL>();
        Source s = new Source(html);
        FormFields formFields = s.getFormFields();

        for (FormField formField : formFields) {
            if (formField.getFormControl().getFormControlType() == FormControlType.SELECT_SINGLE) {
                for (Iterator<Element> it = formField.getFormControl().getOptionElementIterator(); it.hasNext();) {
                    String value = it.next().getAttributeValue("value");
                    if (value != null && value.startsWith("http")) {
                        try {
                            l.add(new URL(value));
                        } catch (MalformedURLException e) {
                            log.debug("Not adding malformed URL [" + value + "]: " + e, e);
                        }
                    }
                }
            }
        }
        return l;
    }

    /**
     * @param html
     * @return
     */
    static String getActionString(String html) {
        Source s = new Source(html);
        String action = null;

        List<StartTag> formTags = s.getAllStartTags(HTMLElementName.FORM);
        for (StartTag formTag : formTags)
            action = formTag.getAttributeValue("action");

        log.debug("Extracted action: " + action);

        return action;
    }

    /**
     * @param html
     * @return
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    static URL getActionURL(URL currentPage, String html) throws MalformedURLException, URISyntaxException {
        String action = getActionString(html);

        URI actionUri = new URI(action);

        log.debug("Action URI: " + actionUri);
        log.debug("Current Page: " + currentPage);

        if (actionUri.isAbsolute()) {
            log.debug("Action is absolute, returning " + actionUri.toURL());
            return actionUri.toURL();
        }

        String scheme = actionUri.getScheme() == null ? "" : actionUri.getScheme();
        String host = actionUri.getHost() == null ? "" : actionUri.getHost();
        String path = actionUri.getPath() == null ? "" : actionUri.getRawPath();
        int port = actionUri.getPort();
        String query = actionUri.getQuery() == null ? "" : actionUri.getRawQuery();
        String fragment = actionUri.getFragment() == null ? "" : actionUri.getRawFragment();

        if (scheme.length() == 0) {
            scheme = currentPage.toURI().getScheme();
            log.debug("Scheme is empty, using " + scheme);
        }
        if (host.length() == 0) {
            host = currentPage.getHost();
            log.debug("Host is empty, using " + host);
        }
        if (path.length() == 0) {
            path = currentPage.getPath();
            log.debug("Path is empty, using " + path);
        }
        if (port < 0) {
            if (currentPage.getPort() >= 0)
                port = currentPage.getPort();
            log.debug("Port is empty, using " + port);
        }
        if (query.length() == 0) {
            query = currentPage.getQuery();
            log.debug("Query is empty, using " + query);
        }
        if (fragment.length() == 0) {
            fragment = currentPage.toURI().getFragment();
            log.debug("Fragment is empty, using " + fragment);
        }

        log.debug("Scheme  : " + scheme);
        log.debug("Host    : " + host);
        log.debug("Path    : " + path);
        log.debug("Port    : " + port);
        log.debug("Query   : " + query);
        log.debug("Fragment: " + fragment);

        StringBuffer b = new StringBuffer();
        b.append(scheme);
        b.append("://");
        b.append(host);
        if (port != -1) {
            b.append(":");
            b.append(port);
        }

        b.append(path);

        if (query != null && query.length() > 0) {
            b.append("?");
            b.append(query);
        }

        if (fragment != null && fragment.length() > 0) {
            b.append("#");
            b.append(fragment);
        }

        log.debug("Resulting URL: " + b.toString());
        return new URL(b.toString());
    }

    public static void addUserAgentHeaders(AbstractHttpMessage m) {
        m.addHeader("User-Agent",
                "Mozilla/5.0 (Windows; U; Windows NT 6.1; nl; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13");
    }

    /**
     * @return
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    static DefaultHttpClient getFakeSSLTolerantClient() throws KeyManagementException, NoSuchAlgorithmException {
        DefaultHttpClient httpclient = new DefaultHttpClient();

        httpclient.addResponseInterceptor(new HttpResponseInterceptor() {

            public void process(final HttpResponse response, final HttpContext context) throws HttpException,
                    IOException {
                HttpEntity entity = response.getEntity();
                Header contentEncodingHeader = entity.getContentEncoding();

                if (contentEncodingHeader != null) {
                    log.debug("{} Content-Encoding header: {} = {}", contentEncodingHeader.getName(), contentEncodingHeader.getValue());
                    log.debug("{} Content-Encoding header element(s) supplied.", contentEncodingHeader.getElements());

                    for (HeaderElement headerElement : contentEncodingHeader.getElements()) {
                        log.debug("Content-Encoding Header: {} = {}", headerElement.getName(), headerElement.getValue());

                        if (headerElement.getName().equalsIgnoreCase("gzip")) {
                            response.setEntity(new GzipDecompressDecoder(response.getEntity()));
                            return;
                        }

                    }

                } else {
                    log.debug("No Content-Encoding header supplied.");
                }

            }

        });

        // First create a trust manager that won't care.
        X509TrustManager trustManager = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // Don't do anything.
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // Don't do anything.
            }

            public X509Certificate[] getAcceptedIssuers() {
                // Don't do anything.
                return null;
            }
        };

        // Now put the trust manager into an SSLContext.
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { trustManager }, null);

        SSLSocketFactory sf = new SSLSocketFactory(sslcontext);
        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        Scheme sch = new Scheme("https", sf, 443);
        httpclient.getConnectionManager().getSchemeRegistry().register(sch);

        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        httpclient.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);

        return httpclient;
    }
}
