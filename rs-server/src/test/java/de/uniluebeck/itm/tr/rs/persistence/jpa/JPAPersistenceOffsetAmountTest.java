package de.uniluebeck.itm.tr.rs.persistence.jpa;

import com.google.inject.Guice;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistenceOffsetAmountTest;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

@RunWith(MockitoJUnitRunner.class)
public class JPAPersistenceOffsetAmountTest extends RSPersistenceOffsetAmountTest {

	private static final Map<String, String> properties = new HashMap<String, String>() {{

		// configure derby as embedded db for unit test
		put("hibernate.connection.url", "jdbc:derby:memory:DeviceConfigDB;create=true");
		put("hibernate.connection.driver_class", "org.apache.derby.jdbc.EmbeddedDriver");
		put("hibernate.dialect", "org.hibernate.dialect.DerbyTenSevenDialect");

		// configure hibernate ORM
		put("hibernate.ddl-generation.output-mode", "database");
		put("hibernate.hbm2ddl.auto", "create");
		put("hibernate.archive.autodetection", "class, hbm");

		// configure time zone
		put("timezone", "GMT");
	}};

	@Before
	public void setUp() throws Exception {
		final RSPersistenceJPAModule module = new RSPersistenceJPAModule(TimeZone.getDefault(), mapToProperties());
		final RSPersistence rsPersistence = Guice.createInjector(module).getInstance(RSPersistence.class);
		setUp(rsPersistence);
	}

	private Properties mapToProperties() {
		final Properties props = new Properties();
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			props.put(entry.getKey(), entry.getValue());
		}
		return props;
	}
}
