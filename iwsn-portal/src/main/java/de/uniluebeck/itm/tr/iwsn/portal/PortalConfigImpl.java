package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.common.config.MultimapOptionHandler;
import de.uniluebeck.itm.tr.common.config.PropertiesOptionHandler;
import de.uniluebeck.itm.tr.rs.RSConfigImpl;
import org.kohsuke.args4j.Option;

import java.net.URI;
import java.util.Properties;

public class PortalConfigImpl extends RSConfigImpl implements PortalConfig {

	@Option(name = "--overlayPort",
			usage = "Port to listen on for the internal overlay network (default: 8880)")
	private int overlayPort = 8880;

	@Option(name = "--protobufApiPort",
			usage = "Port to provide the protobuf-based API on (default: 8885)")
	private int protobufApiPort = 8885;

	@Option(name = "--rsEndpointUri",
			usage = "The endpoint URI of the reservation system (RS) service")
	private URI rsEndpointUri;

	@Option(name = "--snaaEndpointUri",
			usage = "The endpoint URL of the authentication and authorization (SNAA) service",
			required = true)
	private URI snaaEndpointUri;

	@Option(name = "--smConfiguration",
			usage = "Additional key/value pairs to be returned by SessionManagements.getConfiguration(). Multiple "
					+ "comma-separated values are allowed per key. Example "
					+ "usage: \"--options k1=k1v1,k1v2 k2=k2v1,k2v2\".",
			handler = MultimapOptionHandler.class)
	private Multimap<String, String> smConfiguration = HashMultimap.create();

	@Option(name = "--deviceDBUri",
			usage = "The URI on which the DeviceDB runs (only if --deviceDBProperties is not set and access to DeviceDB shall be executed remotely)",
			required = false
	)
	private URI deviceDBUri = null;

	@Option(name = "--deviceDBProperties",
			usage = ".properties file to initialize the DeviceDB JPA storage (alternative: --deviceDBUri)",
			required = false,
			handler = PropertiesOptionHandler.class
	)
	private Properties deviceDBProperties;

	@Option(name = "--testbedName",
			usage = "Name of the testbed to be displayed in WiseGui frontend",
			required = true
	)
	private String testbedName;

	@Override
	public Properties getDeviceDBProperties() {
		return deviceDBProperties;
	}

	@SuppressWarnings("unused")
	public void setDeviceDBProperties(final Properties deviceDBProperties) {
		this.deviceDBProperties = deviceDBProperties;
	}

	@Override
	public URI getDeviceDBUri() {
		return deviceDBUri;
	}

	@SuppressWarnings("unused")
	public void setDeviceDBUri(final URI deviceDBUri) {
		this.deviceDBUri = deviceDBUri;
	}

	@Override
	public Multimap<String, String> getSmConfiguration() {
		return smConfiguration;
	}

	@SuppressWarnings("unused")
	public void setConfiguration(final Multimap<String, String> smConfiguration) {
		this.smConfiguration = smConfiguration;
	}

	@Override
	public int getOverlayPort() {
		return overlayPort;
	}

	@SuppressWarnings("unused")
	public void setOverlayPort(final int overlayPort) {
		this.overlayPort = overlayPort;
	}

	@Override
	public int getProtobufApiPort() {
		return protobufApiPort;
	}

	@SuppressWarnings("unused")
	public void setProtobufApiPort(final int protobufApiPort) {
		this.protobufApiPort = protobufApiPort;
	}

	@Override
	public URI getRsEndpointUri() {
		return rsEndpointUri;
	}

	@SuppressWarnings("unused")
	public void setRsEndpointUri(final URI rsEndpointUri) {
		this.rsEndpointUri = rsEndpointUri;
	}

	@Override
	public URI getSnaaEndpointUri() {
		return snaaEndpointUri;
	}

	@SuppressWarnings("unused")
	public void setSnaaEndpointUri(final URI snaaEndpointUri) {
		this.snaaEndpointUri = snaaEndpointUri;
	}

	@Override
	public String getTestbedName() {
		return testbedName;
	}

	@SuppressWarnings("unused")
	public void setTestbedName(final String testbedName) {
		this.testbedName = testbedName;
	}
}
