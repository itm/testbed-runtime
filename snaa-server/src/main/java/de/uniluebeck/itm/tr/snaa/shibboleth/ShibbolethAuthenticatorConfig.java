package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.common.net.HostAndPort;
import de.uniluebeck.itm.tr.snaa.SNAAProperties;

import javax.annotation.Nullable;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

public class ShibbolethAuthenticatorConfig {

	private final String url;

	@Nullable
	private final HostAndPort proxy;

	public ShibbolethAuthenticatorConfig(final Properties properties) {
		this.url = properties.getProperty(SNAAProperties.SHIBBOLETH_URL);
		final String proxyProperty = properties.getProperty(SNAAProperties.SHIBBOLETH_PROXY);
		this.proxy = proxyProperty != null && proxyProperty.length() > 0 ?
				HostAndPort.fromString(proxyProperty) :
				null;
	}

	public ShibbolethAuthenticatorConfig(final String url,
										 @Nullable final HostAndPort proxy) {
		this.url = checkNotNull(url);
		this.proxy = proxy;
	}

	@Nullable
	public HostAndPort getProxy() {
		return proxy;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return "ShibbolethAuthenticatorConfig{" +
				"url='" + url + '\'' +
				", proxy=" + proxy +
				"} " + super.toString();
	}
}
