package de.uniluebeck.itm.tr.iwsn.devicedb;

import com.google.inject.AbstractModule;

import java.util.Properties;

public class DeviceConfigDBModule extends AbstractModule {

	private Properties properties;

	public DeviceConfigDBModule(final Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		/*install(new JpaPersistModule("DeviceConfigDB").properties(properties));
		bind(JPAInitializer.class).asEagerSingleton();
		bind(DeviceConfigDB.class).to(DeviceConfigDBImpl.class).in(Scopes.SINGLETON);
		*/
		bind(DeviceConfigDB.class).to(DeviceConfigDBDummy.class);
	}

	/*@Provides
	GenericDao<DeviceConfig, String> provideDao(final EntityManager entityManager) {
		return new GenericDaoImpl<DeviceConfig, String>(entityManager, DeviceConfig.class);
	}*/
}
