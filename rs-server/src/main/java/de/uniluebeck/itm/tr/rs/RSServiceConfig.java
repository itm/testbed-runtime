package de.uniluebeck.itm.tr.rs;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.converters.PropertiesTypeConverter;
import de.uniluebeck.itm.util.propconf.converters.URITypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

import java.net.URI;
import java.util.Properties;

public class RSServiceConfig {

	@PropConf(
			usage = "Persistence layer to use",
			example = "GCAL/JPA/IN_MEMORY/REMOTE",
			defaultValue = "IN_MEMORY"
	)
	public static final String RS_TYPE = "rs.type";

	@Inject
	@Named(RS_TYPE)
	private RSType rsType;

	@PropConf(
			usage = "Context path on which to run the RS service",
			example = "/soap/v3/rs",
			defaultValue = "/soap/v3/rs"
	)
	public static final String RS_CONTEXT_PATH = "rs.context.path";

	@Inject
	@Named(RS_CONTEXT_PATH)
	private String rsContextPath = "/soap/v3/rs";

	@PropConf(
			usage = "Persistence layer configuration .properties file",
			typeConverter = PropertiesTypeConverter.class
	)
	public static final String RS_JPA_PROPERTIES = "rs.jpa.properties";

	@Inject(optional = true)
	@Named(RS_JPA_PROPERTIES)
	private Properties rsJPAProperties;

	@PropConf(
			usage = "The username of the Google account to be used (only if GCAL is used)"
	)
	public static final String RS_GCAL_USERNAME = "rs.gcal.username";

	@Inject(optional = true)
	@Named(RS_GCAL_USERNAME)
	private String rsGcalUsername;

	@PropConf(
			usage = "The password of the Google account to be used (only if GCAL is used)"
	)
	public static final String RS_GCAL_PASSWORD = "rs.gcal.password";

	@Inject(optional = true)
	@Named(RS_GCAL_PASSWORD)
	private String rsGcalPassword;

	@PropConf(
			usage = "The URI of the remote RS service (only if REMOTE is used)",
			typeConverter = URITypeConverter.class
	)
	public static final String RS_REMOTE_URI = "rs.remote.uri";

	@Inject
	@Named(RS_REMOTE_URI)
	private URI rsRemoteUri;

	public String getRsContextPath() {
		return rsContextPath;
	}

	public Properties getRsJPAProperties() {
		return rsJPAProperties;
	}

	public RSType getRsType() {
		return rsType;
	}

	public URI getRsRemoteUri() {
		return rsRemoteUri;
	}

	public String getRsGcalPassword() {
		return rsGcalPassword;
	}

	public String getRsGcalUsername() {
		return rsGcalUsername;
	}
}
