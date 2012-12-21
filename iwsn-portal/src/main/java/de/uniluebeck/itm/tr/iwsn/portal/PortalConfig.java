package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.net.HostAndPort;
import de.uniluebeck.itm.tr.iwsn.common.config.ConfigWithLogging;
import de.uniluebeck.itm.tr.iwsn.common.config.HostAndPortOptionHandler;
import org.kohsuke.args4j.Option;

public class PortalConfig extends ConfigWithLogging {

	@Option(name = "--portalAddress",
			usage = "Address for the portal to listen for the internal network (default: localhost:8080)",
			handler = HostAndPortOptionHandler.class
	)
	public HostAndPort portalAddress = HostAndPort.fromParts("localhost", 8080);

	public String urnPrefix;

	public String smEndpointUrl;

	public String wsnInstanceBaseUrl;

	public String rsSystemEndpointUrl;

	public String snaaEndpointUrl;

	public String wiseMLFilename;

	public Integer maximumDeliveryQueueSize;

	public HostAndPort protobufInterface = null;

}
