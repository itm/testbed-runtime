package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;

import java.util.Properties;

public class DeviceDBModule extends AbstractModule {

	private Properties properties;

	public DeviceDBModule(final Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		/*install(new JpaPersistModule("DeviceDB").properties(properties));
		bind(JPAInitializer.class).asEagerSingleton();
		bind(DeviceDB.class).to(DeviceDBImpl.class).in(Scopes.SINGLETON);
		*/
		bind(DeviceDB.class).to(DeviceDBDummy.class);
	}

	/*@Provides
	GenericDao<DeviceConfig, String> provideDao(final EntityManager entityManager) {
		return new GenericDaoImpl<DeviceConfig, String>(entityManager, DeviceConfig.class);
	}*/
}
