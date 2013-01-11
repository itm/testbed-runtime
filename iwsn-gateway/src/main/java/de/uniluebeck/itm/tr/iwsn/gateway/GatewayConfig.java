package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.net.HostAndPort;
import de.uniluebeck.itm.tr.iwsn.common.config.ConfigWithLogging;
import de.uniluebeck.itm.tr.iwsn.common.config.HostAndPortOptionHandler;
import org.kohsuke.args4j.Option;

public class GatewayConfig extends ConfigWithLogging {

	@Option(name = "--portalOverlayAddress",
			usage = "Hostname and port on which the portal server listens for the internal overlay network "
					+ "(usually $PORTAL_HOSTNAME:8880)",
			required = true,
			handler = HostAndPortOptionHandler.class
	)
	public HostAndPort portalOverlayAddress;

	@Option(name = "--restAPI",
			usage = "If set a REST API is started (default: false)",
			required = false
	)
	public boolean restAPI = false;

	@Option(name = "--restAPIPort",
			usage = "The port for the REST API to run on (only used when --restAPI is set, default: 8080)",
			required = false
	)
	public int restAPIPort = 8080;

}
