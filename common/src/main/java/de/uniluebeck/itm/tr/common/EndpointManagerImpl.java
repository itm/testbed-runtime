package de.uniluebeck.itm.tr.common;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.config.CommonConfig;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public class EndpointManagerImpl implements EndpointManager {

	private final CommonConfig commonConfig;

	@Inject
	public EndpointManagerImpl(final CommonConfig commonConfig) {
		this.commonConfig = checkNotNull(commonConfig);
	}

	@Override
	public URI getSnaaEndpointUri() {
		@SuppressWarnings("StringBufferReplaceableByString")
		final StringBuilder uri = new StringBuilder()
				.append("http://")
				.append(assertNonEmpty(commonConfig.getHostname(), CommonConfig.HOSTNAME))
				.append(":")
				.append(commonConfig.getPort())
				.append(Constants.SOAP_API_V3.SNAA_CONTEXT_PATH);
		return URI.create(uri.toString());
	}

	@Override
	public URI getRsEndpointUri() {
		@SuppressWarnings("StringBufferReplaceableByString")
		final StringBuilder uri = new StringBuilder()
				.append("http://")
				.append(assertNonEmpty(commonConfig.getHostname(), CommonConfig.HOSTNAME))
				.append(":")
				.append(commonConfig.getPort())
				.append(Constants.SOAP_API_V3.RS_CONTEXT_PATH);
		return URI.create(uri.toString());
	}

	@Override
	public URI getSmEndpointUri() {
		@SuppressWarnings("StringBufferReplaceableByString")
		final StringBuilder uri = new StringBuilder()
				.append("http://")
				.append(assertNonEmpty(commonConfig.getHostname(), CommonConfig.HOSTNAME))
				.append(":")
				.append(commonConfig.getPort())
				.append(Constants.SOAP_API_V3.SM_CONTEXT_PATH);
		return URI.create(uri.toString());
	}

	@Override
	public URI getWsnEndpointUriBase() {
		@SuppressWarnings("StringBufferReplaceableByString")
		final StringBuilder uri = new StringBuilder()
				.append("http://")
				.append(assertNonEmpty(commonConfig.getHostname(), CommonConfig.HOSTNAME))
				.append(":")
				.append(commonConfig.getPort())
				.append(Constants.SOAP_API_V3.WSN_CONTEXT_PATH_BASE);
		return URI.create(uri.toString());
	}

	private String assertNonEmpty(final String param, final String paramName) {
		if (param == null || "".equals(param)) {
			throw new IllegalArgumentException(
					"Configuration parameter " + paramName + " must be set!"
			);
		}
		return param;
	}
}
