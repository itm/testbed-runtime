package de.uniluebeck.itm.tr.rs;

import de.uniluebeck.itm.tr.common.config.CommonConfig;

import java.util.Properties;

public interface RSConfig extends CommonConfig {

	String getRsContextPath();

	RSPersistenceType getRsPersistenceType();

	Properties getRsPersistenceConfig();
}
