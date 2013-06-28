package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.common.config.PropertiesTypeConverter;
import de.uniluebeck.itm.tr.common.config.URITypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Properties;

public class DeviceDBConfig {

	@PropConf(
			usage = "The context path on which to run the device database REST API",
			example = "/rest/v1/devicedb",
			defaultValue = "/rest/v1/devicedb"
	)
	public static final String DEVICEDB_REST_API_CONTEXT_PATH = "devicedb.rest_api.context_path";

	@Inject
	@Named(DEVICEDB_REST_API_CONTEXT_PATH)
	private String deviceDBRestApiContextPath;

	@PropConf(
			usage = "The context path on which to run the device database web frontend",
			example = "/devicedb",
			defaultValue = "/devicedb"
	)
	public static final String DEVICEDB_WEBAPP_CONTEXT_PATH = "devicedb.webapp.context_path";

	@Inject
	@Named(DEVICEDB_WEBAPP_CONTEXT_PATH)
	private String deviceDBWebappContextPath;

	@PropConf(
			usage = ".properties file to initialize JPA backend for the device database",
			typeConverter = PropertiesTypeConverter.class
	)
	public static final String DEVICEDB_JPA_PROPERTIES = "devicedb.jpa.properties";

	@Inject
	@Named(DEVICEDB_JPA_PROPERTIES)
	private Properties deviceDBJPAProperties;

	@PropConf(
			usage = "The type of DeviceDB backend",
			example = "IN_MEMORY/JPA",
			defaultValue = "IN_MEMORY"
	)
	public static final String DEVICEDB_TYPE = "devicedb.type";

	@Inject
	@Named(DEVICEDB_TYPE)
	private DeviceDBType deviceDBType;

	@PropConf(
			usage = "The URI the DeviceDB REST service runs on",
			typeConverter = URITypeConverter.class
	)
	public static final String DEVICEDB_REMOTE_URI = "devicedb.remote.uri";

	@Inject(optional = true)
	@Named(DEVICEDB_REMOTE_URI)
	private URI deviceDBRemoteUri;

	public String getDeviceDBWebappContextPath() {
		return deviceDBWebappContextPath;
	}

	public Properties getDeviceDBJPAProperties() {
		return deviceDBJPAProperties;
	}

	public String getDeviceDBRestApiContextPath() {
		return deviceDBRestApiContextPath;
	}

	public DeviceDBType getDeviceDBType() {
		return deviceDBType;
	}

	@Nullable
	public URI getDeviceDBRemoteUri() {
		return deviceDBRemoteUri;
	}
}
