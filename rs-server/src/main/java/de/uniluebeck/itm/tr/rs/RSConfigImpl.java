package de.uniluebeck.itm.tr.rs;

import de.uniluebeck.itm.tr.common.config.CommonConfigImpl;
import de.uniluebeck.itm.tr.common.config.PropertiesOptionHandler;
import org.kohsuke.args4j.Option;

import java.util.Properties;

public class RSConfigImpl extends CommonConfigImpl implements RSConfig {

	@Option(name = "--rsPersistenceType",
			usage = "Persistence layer to use",
			required = true)
	protected RSPersistenceType rsPersistenceType;

	@Option(name = "--rsPersistenceConfig",
			usage = "Persistence layer configuration file",
			handler = PropertiesOptionHandler.class)
	protected Properties rsPersistenceConfig;

	@Option(name = "--rsContextPath",
			usage = "Context path on which to run the RS service (default: \"/soap/v3/rs\")")
	protected String rsContextPath = "/soap/v3/rs";

	@Override
	public String getRsContextPath() {
		return rsContextPath;
	}

	@Override
	public RSPersistenceType getRsPersistenceType() {
		return rsPersistenceType;
	}

	@Override
	public Properties getRsPersistenceConfig() {
		return rsPersistenceConfig;
	}

	@SuppressWarnings("unused")
	public void setRsContextPath(final String rsContextPath) {
		this.rsContextPath = rsContextPath;
	}

	@SuppressWarnings("unused")
	public void setRsPersistenceConfig(final Properties rsPersistenceConfig) {
		this.rsPersistenceConfig = rsPersistenceConfig;
	}

	@SuppressWarnings("unused")
	public void setRsPersistenceType(final RSPersistenceType rsPersistenceType) {
		this.rsPersistenceType = rsPersistenceType;
	}
}
