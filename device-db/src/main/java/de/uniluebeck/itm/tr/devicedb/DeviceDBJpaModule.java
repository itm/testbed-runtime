package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.persist.jpa.JpaPersistModule;
import de.uniluebeck.itm.tr.common.jpa.JPAInitializer;

import java.util.Properties;

public class DeviceDBJpaModule extends PrivateModule {

	private final Properties jpaProperties;

	public DeviceDBJpaModule(final Properties jpaProperties) {
		this.jpaProperties = jpaProperties;
	}

	@Override
	protected void configure() {

		install(new JpaPersistModule("DeviceDB").properties(jpaProperties));
		bind(JPAInitializer.class).asEagerSingleton();
		bind(DeviceDBService.class).to(DeviceDBJpa.class).in(Scopes.SINGLETON);

		expose(DeviceDBService.class);
	}
}
