package de.uniluebeck.itm.tr.rs;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.common.config.PropertiesTypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

import java.util.Properties;

public class RSServiceConfig {

	@PropConf(
			usage = "Persistence layer to use",
			example = "GCAL/JPA/IN_MEMORY",
			defaultValue = "IN_MEMORY"
	)
	public static final String RS_PERSISTENCE_TYPE = "rs.persistence.type";

	@Inject
	@Named(RS_PERSISTENCE_TYPE)
	private RSPersistenceType rsPersistenceType;

	@PropConf(
			usage = "Persistence layer configuration .properties file",
			typeConverter = PropertiesTypeConverter.class
	)
	public static final String RS_PERSISTENCE_CONFIG = "rs.persistence.config";

	@Inject
	@Named(RS_PERSISTENCE_CONFIG)
	private Properties rsPersistenceConfig;

	@PropConf(
			usage = "Context path on which to run the RS service",
			example = "/soap/v3/rs",
			defaultValue = "/soap/v3/rs"
	)
	public static final String RS_CONTEXT_PATH = "rs.context.path";

	@Inject
	@Named(RS_CONTEXT_PATH)
	private String rsContextPath = "/soap/v3/rs";

	public String getRsContextPath() {
		return rsContextPath;
	}

	public Properties getRsPersistenceConfig() {
		return rsPersistenceConfig;
	}

	public RSPersistenceType getRsPersistenceType() {
		return rsPersistenceType;
	}
}
