package de.uniluebeck.itm.tr.wsn.federator;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;

public class FederatorWSNConfig {

	private static final String FEDERATOR_SM_ENDPOINT_URL = "federator_sm_endpoint_url";

	private static final String FEDERATOR_RS_ENDPOINT_URL = "federator_rs_endpoint_url";

	private static final String FEDERATOR_SNAA_ENDPOINT_URL = "federator_snaa_endpoint_url";

	private static final String FED_SM_ENDPOINT_URL = ".sm_endpoint_url";

	private static final String FED_URN_PREFIXES = ".urn_prefixes";

	public static FederatorWSNConfig parse(Properties properties) {

		final Splitter splitter = Splitter.on(",").omitEmptyStrings().trimResults();
		final Iterable<String> federates = splitter.split((String) properties.get("federates"));

		final List<FederatorWSNTestbedConfig> testbedConfigs = newArrayList();
		for (String federate : federates) {

			FederatorWSNTestbedConfig testbedConfig = new FederatorWSNTestbedConfig(
					URI.create((String) properties.get(federate + FED_SM_ENDPOINT_URL)),
					ImmutableSet.<String>copyOf(splitter.split((String) properties.get(federate + FED_URN_PREFIXES)))
			);

			testbedConfigs.add(testbedConfig);
		}

		return new FederatorWSNConfig(
				URI.create((String) properties.get(FEDERATOR_SM_ENDPOINT_URL)),
				URI.create((String) properties.get(FEDERATOR_RS_ENDPOINT_URL)),
				URI.create((String) properties.get(FEDERATOR_SNAA_ENDPOINT_URL)),
				ImmutableList.copyOf(testbedConfigs)
		);

	}

	private final URI federatorSnaaEndpointUrl;

	private final URI federatorSmEndpointURL;

	private final URI federatorRsEndpointURL;

	private final ImmutableList<FederatorWSNTestbedConfig> federates;

	public FederatorWSNConfig(final URI federatorSmEndpointURL, final URI federatorRsEndpointURL,
							  final URI federatorSnaaEndpointUrl,
							  final ImmutableList<FederatorWSNTestbedConfig> federates) {

		try {
			federatorSmEndpointURL.toURL();
			federatorRsEndpointURL.toURL();
			federatorSnaaEndpointUrl.toURL();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}

		this.federatorSmEndpointURL = federatorSmEndpointURL;
		this.federatorRsEndpointURL = federatorRsEndpointURL;
		this.federatorSnaaEndpointUrl = federatorSnaaEndpointUrl;
		this.federates = federates;
	}

	/**
	 * Returns the endpoint URL of the Reservation System federator.
	 *
	 * @return the endpoint URL of the Reservation System federator
	 */
	public URI getFederatorRsEndpointURL() {
		return federatorRsEndpointURL;
	}

	/**
	 * Returns the endpoint URL of the Session Management federator.
	 *
	 * @return the endpoint URL of the Session Management federator
	 */
	public URI getFederatorSmEndpointURL() {
		return federatorSmEndpointURL;
	}

	/**
	 * Returns the endpoint URL of the SNAA federator.
	 *
	 * @return the endpoint URL of the SNAA federator
	 */
	public URI getFederatorSnaaEndpointUrl() {
		return federatorSnaaEndpointUrl;
	}

	/**
	 * Returns a list of the federated testbeds.
	 *
	 * @return a list of the federated testbeds
	 */
	public ImmutableList<FederatorWSNTestbedConfig> getFederates() {
		return federates;
	}

	@Override
	public String toString() {
		return "FederatorWSNConfig{" +
				"federatorSnaaEndpointUrl=" + federatorSnaaEndpointUrl +
				", federatorRsEndpointURL=" + federatorRsEndpointURL +
				", federatorSmEndpointURL=" + federatorSmEndpointURL +
				", federates=" + federates +
				'}';
	}
}