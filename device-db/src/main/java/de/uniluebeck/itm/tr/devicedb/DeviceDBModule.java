package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.persist.jpa.JpaPersistModule;

import javax.persistence.EntityManager;
import java.util.Properties;

public class DeviceDBModule extends AbstractModule {

	private Properties properties;

	public DeviceDBModule(final Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		install(new JpaPersistModule("DeviceDB").properties(properties));
		bind(JPAInitializer.class).asEagerSingleton();
		bind(DeviceDB.class).to(DeviceDBImpl.class).in(Scopes.SINGLETON);
	}

	@Provides
	GenericDao<DeviceConfig, String> provideDao(final EntityManager entityManager) {
		return new GenericDaoImpl<DeviceConfig, String>(entityManager, DeviceConfig.class);
	}
}
