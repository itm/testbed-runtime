package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.net.HostAndPort;
import de.uniluebeck.itm.tr.iwsn.common.config.ConfigWithLogging;
import de.uniluebeck.itm.tr.iwsn.common.config.HostAndPortOptionHandler;
import de.uniluebeck.itm.tr.iwsn.common.config.NodeUrnPrefixOptionHandler;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.kohsuke.args4j.Option;

import java.net.URL;

public class PortalConfig extends ConfigWithLogging {

	@Option(name = "--overlayAddress",
			usage = "Hostname and port to listen on for the internal overlay network "
					+ "(default: 0.0.0.0:8880, binding on all interfaces)",
			handler = HostAndPortOptionHandler.class)
	public HostAndPort overlayAddress = HostAndPort.fromParts("0.0.0.0", 8880);

	@Option(name = "--portalAddress",
			usage = "Hostname and port to provide the public portal services on "
					+ "(default: 0.0.0.0:8888, binding on all interfaces)",
			handler = HostAndPortOptionHandler.class)
	public HostAndPort portalAddress = HostAndPort.fromParts("0.0.0.0", 8888);

	@Option(name = "--protobufAddress",
			usage = "Hostname and port of the protobuf-based interface to be started "
					+ "(default: 0.0.0.0:8885, binding on all interfaces)",
			handler = HostAndPortOptionHandler.class)
	public HostAndPort protobufAddress = HostAndPort.fromParts("0.0.0.0", 8885);

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

}
