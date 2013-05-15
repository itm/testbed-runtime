package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.sql.DriverManager;
import java.sql.SQLNonTransientConnectionException;

public class DeviceDBJpaTest extends DeviceDBTestBase {

	private EntityManager entityManager;

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setUp() throws Exception {

		DriverManager.getConnection("jdbc:derby:memory:device-db-unit-test;create=true").close();

		final Injector injector = Guice.createInjector(new DeviceDBJpaModule(JPA_PROPERTIES));
		final DeviceDB deviceDB = injector.getInstance(DeviceDB.class);

		/*entityManagerFactory = injector.getInstance(EntityManagerFactory.class);
		entityManager = injector.getInstance(EntityManager.class);*/

		super.setUp(deviceDB);
	}

	@After
	public void tearDown() throws Exception {

		/*entityManager.close();
		entityManagerFactory.close();*/

		try {
			DriverManager.getConnection("jdbc:derby:memory:device-db-unit-test;shutdown=true").close();
		} catch (SQLNonTransientConnectionException ex) {
			if (ex.getErrorCode() != 45000) {
				throw ex;
			}
			// Shutdown success
		}
	}
}
