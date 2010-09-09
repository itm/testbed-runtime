package eu.wisebed.shibboauth;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 08.09.2010
 * Time: 12:55:30
 * To change this template use File | Settings | File Templates.
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

    public boolean isAuthorized() throws Exception {
        resetState();
        if (url == null) {
            return false;
        }

        // Check if this session is already authenticated
        {
            finalURL = doGet(url, true);
            //check if url is correct
            if (!finalURL.equals(new URL(this.url))) {
                return false;
            }
            //check if key is correct
            if (!this.responseHtml.equals(secretAuthenticationKey)){
                return false;
            }
        }

        return true;
    }
}
