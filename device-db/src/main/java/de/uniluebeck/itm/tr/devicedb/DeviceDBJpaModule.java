package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.persist.jpa.JpaPersistModule;

import java.util.Properties;

public class DeviceDBJpaModule extends AbstractModule {

	private Properties properties;

	public DeviceDBJpaModule(final Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		install(new JpaPersistModule("DeviceDB").properties(properties));
		bind(JPAInitializer.class).asEagerSingleton();
		bind(DeviceDB.class).to(DeviceDBJpa.class).in(Scopes.SINGLETON);
	}
}
