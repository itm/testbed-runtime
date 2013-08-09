package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;

public class DeviceDBRestServiceModule extends PrivateModule {

	@Override
	protected void configure() {

		requireBinding(DeviceDBService.class);
		requireBinding(DeviceDBConfig.class);
		requireBinding(ServicePublisher.class);

		bind(DeviceDBRestService.class).to(DeviceDBRestServiceImpl.class).in(Scopes.SINGLETON);

		expose(DeviceDBRestService.class);
	}
}
