package de.uniluebeck.itm.tr.common.jpa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;

import static com.google.common.base.Throwables.propagate;

@Singleton
public class JPAInitializer {

	@Inject
	public JPAInitializer(final PersistService service) {
		try {
			service.start();
		} catch (Exception e) {
			throw propagate(e);
		}
	}
}