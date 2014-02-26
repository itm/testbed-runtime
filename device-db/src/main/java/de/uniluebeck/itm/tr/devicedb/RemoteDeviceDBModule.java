package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.devicedb.DeviceDBConfig.*;

public class RemoteDeviceDBModule extends PrivateModule {

	private static final String ERROR_MESSAGE =
			"URI for remote DeviceDB REST API is null. Did you forget to configure it?";

	private final URI uri;

	private final URI adminUri;

	private final String adminUsername;

	private final String adminPassword;

	public RemoteDeviceDBModule(final DeviceDBConfig deviceDBConfig) {
		this.uri = checkNotNull(deviceDBConfig.getDeviceDBRemoteUri(), ERROR_MESSAGE);
		this.adminUri = deviceDBConfig.getDeviceDBRemoteAdminUri();
		this.adminUsername = deviceDBConfig.getDeviceDBRemoteAdminUsername();
		this.adminPassword = deviceDBConfig.getDeviceDBRemoteAdminPassword();
	}

	public RemoteDeviceDBModule(final URI uri) {
		this.uri = checkNotNull(uri, ERROR_MESSAGE);
		this.adminUri = null;
		this.adminUsername = null;
		this.adminPassword = null;
	}

	@Override
	protected void configure() {

		bind(URI.class).annotatedWith(Names.named(DEVICEDB_REMOTE_URI)).toInstance(uri);
		if (adminUri != null) {
			bind(URI.class).annotatedWith(Names.named(DEVICEDB_REMOTE_ADMIN_URI)).toInstance(adminUri);
		} else {
			bind(URI.class).annotatedWith(Names.named(DEVICEDB_REMOTE_ADMIN_URI)).toProvider(Providers.<URI>of(null));
		}
		if (adminUsername != null) {
			bind(String.class).annotatedWith(Names.named(DEVICEDB_REMOTE_ADMIN_USERNAME)).toInstance(adminUsername);
		} else {
			bind(String.class)
					.annotatedWith(Names.named(DEVICEDB_REMOTE_ADMIN_USERNAME))
					.toProvider(Providers.<String>of(null));
		}
		if (adminPassword != null) {
			bind(String.class).annotatedWith(Names.named(DEVICEDB_REMOTE_ADMIN_PASSWORD)).toInstance(adminPassword);
		} else {
			bind(String.class)
					.annotatedWith(Names.named(DEVICEDB_REMOTE_ADMIN_PASSWORD))
					.toProvider(Providers.<String>of(null));
		}

		bind(DeviceDBService.class).to(RemoteDeviceDB.class).in(Scopes.SINGLETON);

		expose(DeviceDBService.class);
	}
}
