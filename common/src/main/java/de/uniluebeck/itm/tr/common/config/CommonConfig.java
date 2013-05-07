package de.uniluebeck.itm.tr.common.config;

import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.util.TimeZone;

public interface CommonConfig {

	int getPort();

	NodeUrnPrefix getUrnPrefix();

	TimeZone getTimeZone();
}
