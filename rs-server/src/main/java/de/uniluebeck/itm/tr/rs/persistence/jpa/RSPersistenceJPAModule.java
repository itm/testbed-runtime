package de.uniluebeck.itm.tr.rs.persistence.jpa;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.jpa.entity.*;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

public class RSPersistenceJPAModule extends AbstractModule {

	private final Map<String, String> properties;

	private final TimeZone localTimeZone;

	private EntityManagerFactory factory;

	@Inject
	public RSPersistenceJPAModule(final TimeZone localTimeZone, final Map<String, String> properties) {
		this.localTimeZone = localTimeZone;
		this.properties = properties;
	}

	@Override
	protected void configure() {
		bind(TimeZone.class).toInstance(localTimeZone);
		bind(RSPersistence.class).to(RSPersistenceJPA.class);
	}

	@Provides
	synchronized EntityManager provideEntityManager() {

		// create singleton if it doesn't exist
		if (factory == null) {
			factory = createEntityManagerFactory();
		}

		// create a new entity manager using the factory
		return factory.createEntityManager();
	}

	private EntityManagerFactory createEntityManagerFactory() {

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

		Properties props = new Properties();
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			props.put(entry.getKey(), entry.getValue());
		}

		return cfg.addProperties(props).buildEntityManagerFactory();
	}
}
