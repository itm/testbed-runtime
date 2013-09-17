package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.tr.common.DecoratedImpl;

public class DeviceDBRDModule extends PrivateModule {

	@Override
	protected void configure() {

		requireBinding(DeviceDBConfig.class);

		bind(DeviceDBService.class).to(DeviceDBRD.class).in(Scopes.SINGLETON);
		bind(DeviceDBService.class).annotatedWith(DecoratedImpl.class).to(DeviceDBInMemory.class);

		expose(DeviceDBService.class);
	}
}
