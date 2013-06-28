package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public class RemoteDeviceDBModule extends PrivateModule {

	private static final String ERROR_MESSAGE =
			"URI for remote DeviceDB REST API is null. Did you forget to configure it?";

	private final URI remoteDeviceDBUri;

	public RemoteDeviceDBModule(final DeviceDBConfig deviceDBConfig) {
		this.remoteDeviceDBUri = checkNotNull(deviceDBConfig.getDeviceDBRemoteUri(), ERROR_MESSAGE);
	}

	public RemoteDeviceDBModule(final URI remoteDeviceDBUri) {
		this.remoteDeviceDBUri = checkNotNull(remoteDeviceDBUri, ERROR_MESSAGE);
	}

	@Override
	protected void configure() {

		bind(URI.class).annotatedWith(Names.named(DeviceDBConfig.DEVICEDB_REMOTE_URI)).toInstance(remoteDeviceDBUri);
		bind(DeviceDBService.class).to(RemoteDeviceDB.class).in(Scopes.SINGLETON);

		expose(DeviceDBService.class);
	}
}
