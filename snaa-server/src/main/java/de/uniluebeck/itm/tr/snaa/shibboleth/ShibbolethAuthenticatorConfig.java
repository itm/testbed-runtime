package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.common.net.HostAndPort;
import de.uniluebeck.itm.tr.snaa.SNAAProperties;

import javax.annotation.Nullable;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

public class ShibbolethAuthenticatorConfig {

	private final String url;

	private final String idpDomain;

	@Nullable
	private final HostAndPort proxy;

	public ShibbolethAuthenticatorConfig(final Properties properties) {
		this.url = properties.getProperty(SNAAProperties.SHIBBOLETH_URL);
		this.idpDomain = properties.getProperty(SNAAProperties.SHIBBOLETH_IDPDOMAIN);
		this.proxy = properties.getProperty(SNAAProperties.SHIBBOLETH_PROXY) == null ?
				null :
				HostAndPort.fromString(properties.getProperty(SNAAProperties.SHIBBOLETH_PROXY));
	}

	public ShibbolethAuthenticatorConfig(final String idpDomain,
										 final String url,
										 @Nullable final HostAndPort proxy) {
		this.idpDomain = checkNotNull(idpDomain);
		this.url = checkNotNull(url);
		this.proxy = checkNotNull(proxy);
	}

	public String getIdpDomain() {
		return idpDomain;
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
				"idpDomain='" + idpDomain + '\'' +
				", url='" + url + '\'' +
				", proxy=" + proxy +
				"} " + super.toString();
	}
}
