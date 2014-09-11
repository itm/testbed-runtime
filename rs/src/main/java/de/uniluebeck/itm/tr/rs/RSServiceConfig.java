package de.uniluebeck.itm.tr.rs;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;
import de.uniluebeck.itm.util.propconf.converters.PropertiesTypeConverter;

import java.util.Properties;

public class RSServiceConfig {

	@PropConf(
			usage = "Persistence layer to use",
			example = "JPA/IN_MEMORY",
			defaultValue = "IN_MEMORY"
	)
	public static final String RS_TYPE = "rs.type";

	@Inject
	@Named(RS_TYPE)
	private RSType rsType;

	@PropConf(
			usage = "Persistence layer configuration .properties file",
			typeConverter = PropertiesTypeConverter.class
	)
	public static final String RS_JPA_PROPERTIES = "rs.jpa.properties";

	@Inject(optional = true)
	@Named(RS_JPA_PROPERTIES)
	private Properties rsJPAProperties;

	public Properties getRsJPAProperties() {
		return rsJPAProperties;
	}

	public RSType getRsType() {
		return rsType;
	}
}
