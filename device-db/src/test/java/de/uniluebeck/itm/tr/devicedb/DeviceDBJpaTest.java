package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.DriverManager;
import java.sql.SQLNonTransientConnectionException;

@RunWith(MockitoJUnitRunner.class)
public class DeviceDBJpaTest extends DeviceDBTestBase {

	@Before
	public void setUp() throws Exception {

		DriverManager.getConnection("jdbc:derby:memory:device-db-unit-test;create=true").close();

		final Injector injector = Guice.createInjector(testModule, new DeviceDBJpaModule(JPA_PROPERTIES));
		final DeviceDBService deviceDBService = injector.getInstance(DeviceDBService.class);

		super.setUp(deviceDBService);
	}

	@After
	public void tearDown() throws Exception {


		try {
			DriverManager.getConnection("jdbc:derby:memory:device-db-unit-test;shutdown=true").close();
		} catch (SQLNonTransientConnectionException ex) {
			if (ex.getErrorCode() != 45000) {
				throw ex;
			}
			// shutdown success
		}
	}
}
