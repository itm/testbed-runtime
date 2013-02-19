package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.iwsn.common.config.ConfigWithLogging;
import de.uniluebeck.itm.tr.iwsn.common.config.MultimapOptionHandler;
import de.uniluebeck.itm.tr.iwsn.common.config.NodeUrnPrefixOptionHandler;
import de.uniluebeck.itm.tr.iwsn.common.config.PropertiesOptionHandler;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.kohsuke.args4j.Option;

import java.net.URL;
import java.util.Properties;

public class PortalConfig extends ConfigWithLogging {

	private static final Properties DEFAULT_DEVICE_CONFIG_DB_PROPERTIES = new Properties();

	private static final String HIBERNATE_DRIVER_CLASS = "hibernate.connection.driver_class";

	private static final String HIBERNATE_CONNECTION_URL = "hibernate.connection.url";

	private static final String HIBERNATE_DIALECT = "hibernate.dialect";

	private static final String HIBERNATE_HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";

	private static final String HIBERNATE_CONNECTION_USERNAME = "hibernate.connection.username";

	private static final String HIBERNATE_CONNECTION_PASSWORD = "hibernate.connection.password";

	static {
		DEFAULT_DEVICE_CONFIG_DB_PROPERTIES.put(HIBERNATE_CONNECTION_URL, "jdbc:derby:DeviceDB;create=true");
		DEFAULT_DEVICE_CONFIG_DB_PROPERTIES.put(HIBERNATE_DRIVER_CLASS, "org.apache.derby.jdbc.EmbeddedDriver");
		DEFAULT_DEVICE_CONFIG_DB_PROPERTIES.put(HIBERNATE_DIALECT, "org.hibernate.dialect.DerbyTenSevenDialect");
		DEFAULT_DEVICE_CONFIG_DB_PROPERTIES.put(HIBERNATE_HBM2DDL_AUTO, "update");
		DEFAULT_DEVICE_CONFIG_DB_PROPERTIES.put(HIBERNATE_CONNECTION_USERNAME, "");
		DEFAULT_DEVICE_CONFIG_DB_PROPERTIES.put(HIBERNATE_CONNECTION_PASSWORD, "");
	}

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
			handler = MultimapOptionHandler.class,
			multiValued = true)
	public Multimap<String, String> options;

	@Option(name = "--deviceConfigDBProperties",
			usage = "Path to a file to configure the device configuration database",
			handler = PropertiesOptionHandler.class,
			required = false)
	public Properties deviceConfigDBProperties = DEFAULT_DEVICE_CONFIG_DB_PROPERTIES;
}
