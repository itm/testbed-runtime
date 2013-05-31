package de.uniluebeck.itm.tr.federator.snaa;

import de.uniluebeck.itm.tr.common.config.CommonConfig;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public interface SNAAFederatorConfig extends CommonConfig {

	Map<URI, Set<NodeUrnPrefix>> getFederates();

	String getSnaaContextPath();

	SNAAFederatorType getSnaaFederatorType();

	Properties getSnaaFederatorProperties();
}
