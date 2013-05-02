package de.uniluebeck.itm.tr.rs.persistence.jpa;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.*;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Properties;

public class RSPersistenceJPAModule extends AbstractModule {

	private final Properties properties;

	@Inject
	public RSPersistenceJPAModule(final Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {
		bind(RSPersistence.class).to(RSPersistenceJPA.class);
	}

	@Provides
	@Singleton
	synchronized EntityManager provideEntityManager() {

		Ejb3Configuration cfg = new Ejb3Configuration();

		@SuppressWarnings("unchecked")
		List<Class<?>> persistedClasses = Lists.<Class<?>>newArrayList(
				ConfidentialReservationDataInternal.class,
				DataInternal.class,
				PublicReservationDataInternal.class,
				ReservationDataInternal.class,
				SecretReservationKeyInternal.class
		);

		for (Class<?> c : persistedClasses) {
			cfg.addAnnotatedClass(c);
		}

		return cfg.addProperties(properties).buildEntityManagerFactory().createEntityManager();
	}
}
