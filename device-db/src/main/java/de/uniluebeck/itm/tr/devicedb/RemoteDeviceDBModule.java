package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

import java.net.URI;

public class RemoteDeviceDBModule extends PrivateModule {

	private final URI remoteDeviceDBUri;

	public RemoteDeviceDBModule(final DeviceDBConfig deviceDBConfig) {
		this.remoteDeviceDBUri = deviceDBConfig.getDeviceDBRemoteUri();
	}

	public RemoteDeviceDBModule(final URI remoteDeviceDBUri) {
		this.remoteDeviceDBUri = remoteDeviceDBUri;
	}

	@Override
	protected void configure() {

		bind(URI.class).annotatedWith(Names.named(DeviceDBConfig.DEVICEDB_REMOTE_URI)).toInstance(remoteDeviceDBUri);
		bind(DeviceDBService.class).to(RemoteDeviceDB.class).in(Scopes.SINGLETON);

		expose(DeviceDBService.class);
	}
}
