package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.net.HostAndPort;
import de.uniluebeck.itm.tr.iwsn.common.config.ConfigWithLogging;
import de.uniluebeck.itm.tr.iwsn.common.config.HostAndPortOptionHandler;
import org.kohsuke.args4j.Option;

public class GatewayConfig extends ConfigWithLogging {

	@Option(name = "--portalAddress", usage = "Hostname and port of the portal server (e.g. portal:1234)",
			required = true,
			handler = HostAndPortOptionHandler.class
	)
	public HostAndPort portalAddress;

}
