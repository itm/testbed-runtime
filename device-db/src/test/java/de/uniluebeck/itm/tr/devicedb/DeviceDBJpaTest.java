package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import org.junit.Before;

public class DeviceDBJpaTest extends DeviceDBTestBase {

	@Before
	public void setUp() throws Exception {
		super.setUp(Guice.createInjector(new DeviceDBJpaModule(JPA_PROPERTIES)).getInstance(DeviceDB.class));
	}
}
