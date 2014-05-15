package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.converters.PropertiesTypeConverter;
import de.uniluebeck.itm.util.propconf.converters.URITypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Properties;

public class DeviceDBConfig {

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
			example = "IN_MEMORY/JPA/REMOTE/SMARTSANTANDER",
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

	@PropConf(
			usage = "The URI the DeviceDB ADMIN REST service runs on",
			typeConverter = URITypeConverter.class
	)
	public static final String DEVICEDB_REMOTE_ADMIN_URI = "devicedb.remote.admin_uri";

	@Inject(optional = true)
	@Named(DEVICEDB_REMOTE_ADMIN_URI)
	private URI deviceDBRemoteAdminUri;

	@PropConf(
			usage = "The username to authenticate on the DeviceDB ADMIN REST service",
			typeConverter = URITypeConverter.class
	)
	public static final String DEVICEDB_REMOTE_ADMIN_USERNAME = "devicedb.remote.admin_username";

	@Inject(optional = true)
	@Named(DEVICEDB_REMOTE_ADMIN_USERNAME)
	private String deviceDBRemoteAdminUsername;

	@PropConf(
			usage = "The password to authenticate on the DeviceDB ADMIN REST service",
			typeConverter = URITypeConverter.class
	)
	public static final String DEVICEDB_REMOTE_ADMIN_PASSWORD = "devicedb.remote.admin_password";

	@Inject(optional = true)
	@Named(DEVICEDB_REMOTE_ADMIN_PASSWORD)
	private String deviceDBRemoteAdminPassword;

	@PropConf(
			usage = "If used in the SmartSantander context: the URI of the Resource Directory (RD)"
	)
	public static final String SMARTSANTANDER_RD_URI = "devicedb.smartsantander.rd.uri";

	@Inject(optional = true)
	@Named(SMARTSANTANDER_RD_URI)
	private URI smartSantanderRDUri;

	@PropConf(
			usage = "If used in the SmartSantander context: the ID of the TR portal server in the Resource Directory (RD)"
	)
	public static final String SMARTSANTANDER_RD_PORTAL_ID = "devicedb.smartsantander.rd.portal_id";

	@Inject(optional = true)
	@Named(SMARTSANTANDER_RD_PORTAL_ID)
	private String smartSantanderRDPortalId;

	@PropConf(
			usage = "If used in the SmartSantander context: the URI of the EventBroker to connect to",
			example = "tcp://lira.tlmat.unican.es:9020",
			typeConverter = URITypeConverter.class
	)
	public static final String SMARTSANTANDER_EVENTBROKER_URI = "devicedb.smartsantander.eventbroker.uri";

	@Inject(optional = true)
	@Named(SMARTSANTANDER_EVENTBROKER_URI)
	private URI smartSantanderEventBrokerUri;

	public URI getSmartSantanderRDUri() {
		return smartSantanderRDUri;
	}

	public Properties getDeviceDBJPAProperties() {
		return deviceDBJPAProperties;
	}

	public DeviceDBType getDeviceDBType() {
		return deviceDBType;
	}

	@Nullable
	public URI getDeviceDBRemoteUri() {
		return deviceDBRemoteUri;
	}

	@Nullable
	public URI getDeviceDBRemoteAdminUri() {
		return deviceDBRemoteAdminUri;
	}

	@Nullable
	public String getSmartSantanderRDPortalId() {
		return smartSantanderRDPortalId;
	}

	@Nullable
	public URI getSmartSantanderEventBrokerUri() {
		return smartSantanderEventBrokerUri;
	}

	@Nullable
	public String getDeviceDBRemoteAdminUsername() {
		return deviceDBRemoteAdminUsername;
	}

	@Nullable
	public String getDeviceDBRemoteAdminPassword() {
		return deviceDBRemoteAdminPassword;
	}
}
