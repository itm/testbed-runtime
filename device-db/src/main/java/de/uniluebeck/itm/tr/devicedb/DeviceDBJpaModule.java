package de.uniluebeck.itm.tr.devicedb;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import de.uniluebeck.itm.tr.devicedb.entity.*;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Properties;

public class DeviceDBJpaModule extends AbstractModule {

	private Properties properties;

	public DeviceDBJpaModule(final Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		bind(DeviceDB.class).to(DeviceDBJpa.class).in(Scopes.SINGLETON);
	}

	@Provides
	@Singleton
	synchronized EntityManager provideEntityManager() {

		@SuppressWarnings("deprecation") Ejb3Configuration cfg = new Ejb3Configuration();

		@SuppressWarnings("RedundantTypeArguments") List<Class<?>> persistedClasses = Lists.<Class<?>>newArrayList(
				DeviceConfigEntity.class,
				CoordinateEntity.class,
				ChannelHandlerConfigEntity.class,
				KeyValueEntity.class,
				CapabilityEntity.class
		);

		for (Class<?> c : persistedClasses) {
			cfg.addAnnotatedClass(c);
		}

		return cfg.addProperties(properties).buildEntityManagerFactory().createEntityManager();
	}
}
