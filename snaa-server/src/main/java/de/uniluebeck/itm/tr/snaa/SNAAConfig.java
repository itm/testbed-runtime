package de.uniluebeck.itm.tr.snaa;

import de.uniluebeck.itm.tr.common.config.CommonConfig;

import java.util.Properties;

public interface SNAAConfig extends CommonConfig {

	String getSnaaContextPath();

	SNAAType getSnaaType();

	Properties getSnaaProperties();

}
