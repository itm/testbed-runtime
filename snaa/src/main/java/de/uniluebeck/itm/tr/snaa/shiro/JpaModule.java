package de.uniluebeck.itm.tr.snaa.shiro;

import com.google.inject.AbstractModule;
import com.google.inject.persist.jpa.JpaPersistModule;
import de.uniluebeck.itm.tr.common.jpa.JPAInitializer;

import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

public class JpaModule extends AbstractModule {

	private final String jpaUnit;

	private final Properties jpaProperties;

	public JpaModule(final String jpaUnit, final Properties jpaProperties) {
		this.jpaUnit = checkNotNull(jpaUnit);
		this.jpaProperties = checkNotNull(jpaProperties);
	}

	@Override
	protected void configure() {
		install(new JpaPersistModule(jpaUnit).properties(jpaProperties));
		bind(JPAInitializer.class).asEagerSingleton();
	}
}
