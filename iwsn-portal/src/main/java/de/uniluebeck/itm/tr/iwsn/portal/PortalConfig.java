package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.iwsn.common.config.ConfigWithLogging;
import de.uniluebeck.itm.tr.iwsn.common.config.MultimapOptionHandler;
import de.uniluebeck.itm.tr.iwsn.common.config.NodeUrnPrefixOptionHandler;
import de.uniluebeck.itm.tr.iwsn.common.config.PropertiesOptionHandler;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.kohsuke.args4j.Option;

import java.net.URI;
import java.net.URL;
import java.util.Properties;

public class PortalConfig extends ConfigWithLogging {

	@Option(name = "--overlayPort",
			usage = "Port to listen on for the internal overlay network (default: 8880)")
	public int overlayPort = 8880;

	@Option(name = "--port",
			usage = "Port to provide the public SOAP and REST APIs on (default: 8888)")
	public int port = 8888;

	@Option(name = "--protobufPort",
			usage = "Port to provide the protobuf-based API on (default: 8885)")
	public int protobufPort = 8885;

	@Option(name = "--nodeUrnPrefix",
			usage = "The node URN prefix this portal is responsible for (e.g. \"urn:wisebed:uzl1:\"",
			handler = NodeUrnPrefixOptionHandler.class)
	public NodeUrnPrefix urnPrefix;

	@Option(name = "--rsEndpointUrl",
			usage = "The endpoint URL of the reservation system (RS) service",
			required = true)
	public URL rsEndpointUrl;

	@Option(name = "--snaaEndpointUrl",
			usage = "The endpoint URL of the authentication and authorization (SNAA) service",
			required = true)
	public URL snaaEndpointUrl;

	@Option(name = "--options",
			usage = "Additional key/value pairs to pass to TR extensions. Multiple comma-separated values are allowed"
					+ " per key. Example usage: \"--options k1=k1v1,k1v2 k2=k2v1,k2v2\".",
			handler = MultimapOptionHandler.class)
	public Multimap<String, String> options;

	@Option(name = "--deviceDBUri",
			usage = "The URI on which the DeviceDB runs (only if --deviceDBProperties is not set and access to DeviceDB shall be executed remotely)",
			required = false
	)
	public URI deviceDBUri = null;

	@Option(name = "--deviceDBProperties",
			usage = ".properties file to initialize the DeviceDB JPA storage (alternative: --deviceDBUri)",
			required = false,
			handler = PropertiesOptionHandler.class
	)
	public Properties deviceDBProperties;
}
