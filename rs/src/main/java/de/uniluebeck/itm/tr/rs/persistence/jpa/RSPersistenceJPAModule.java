package de.uniluebeck.itm.tr.rs.persistence.jpa;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.persist.jpa.JpaPersistModule;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.jpa.JPAInitializer;
import de.uniluebeck.itm.tr.rs.RSServiceConfig;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;

import java.util.Properties;
import java.util.TimeZone;

public class RSPersistenceJPAModule extends AbstractModule {

	private final TimeZone timeZone;

	private final Properties jpaProperties;

	public RSPersistenceJPAModule(final TimeZone timeZone, final Properties jpaProperties) {
		this.timeZone = timeZone;
		this.jpaProperties = jpaProperties;
	}

	public RSPersistenceJPAModule(final CommonConfig commonConfig, final RSServiceConfig rsServiceConfig) {
		this(commonConfig.getTimeZone(), rsServiceConfig.getRsJPAProperties());
	}

	@Override
	protected void configure() {
		bind(TimeZone.class).toInstance(timeZone);
		install(new JpaPersistModule("RS").properties(jpaProperties));
		bind(JPAInitializer.class).asEagerSingleton();
		bind(RSPersistence.class).to(RSPersistenceJPA.class).in(Singleton.class);
	}
}
