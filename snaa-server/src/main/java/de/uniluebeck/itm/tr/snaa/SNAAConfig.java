package de.uniluebeck.itm.tr.snaa;

import de.uniluebeck.itm.tr.common.config.CommonConfig;

import java.util.Properties;

public interface SNAAConfig extends CommonConfig {

	String getSnaaContextPath();

	SNAAAuthenticationType getSnaaAuthenticationType();

	SNAAAuthorizationType getSnaaAuthorizationType();

	Properties getSnaaAuthorizationConfig();

	Properties getSnaaAuthenticationConfig();

}
