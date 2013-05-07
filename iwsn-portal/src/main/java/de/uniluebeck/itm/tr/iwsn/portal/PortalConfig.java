package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.rs.RSConfig;

import java.net.URI;
import java.util.Properties;

public interface PortalConfig extends CommonConfig, RSConfig {

	Properties getDeviceDBProperties();

	URI getDeviceDBUri();

	Multimap<String, String> getSmConfiguration();

	int getOverlayPort();

	int getProtobufApiPort();

	URI getRsEndpointUri();

	URI getSnaaEndpointUri();

	String getTestbedName();
}
