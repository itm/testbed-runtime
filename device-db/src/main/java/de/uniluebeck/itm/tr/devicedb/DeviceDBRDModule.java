package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import de.uniluebeck.itm.tr.common.DecoratedImpl;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceModule;

public class DeviceDBRDModule extends PrivateModule {

	@Override
	protected void configure() {

		requireBinding(DeviceDBConfig.class);

		install(new SchedulerServiceModule());

		bind(DeviceDBService.class).to(DeviceDBRD.class).in(Scopes.SINGLETON);
		bind(DeviceDBService.class).annotatedWith(DecoratedImpl.class).to(DeviceDBInMemory.class);

		expose(DeviceDBService.class);
	}

	@Provides
	SchedulerService provideSchedulerService(SchedulerServiceFactory factory) {
		return factory.create(-1, "DeviceDBRD");
	}
}
