package de.uniluebeck.itm.tr.snaa.shibboleth;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 07.12.2010
 * Time: 14:57:40
 * To change this template use File | Settings | File Templates.
 */
public class ShibbolethProxy {
    private String proxyHost;
    private int proxyPort;

    public ShibbolethProxy(String proxyHost, int proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyHost() {
        return proxyHost;
    }
}
