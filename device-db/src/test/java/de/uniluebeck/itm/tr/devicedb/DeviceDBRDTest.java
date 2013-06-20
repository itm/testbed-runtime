package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import org.junit.Before;

public class DeviceDBRDTest extends DeviceDBTestBase {
	@Before
	public void setUp() throws Exception {
		super.setUp(Guice.createInjector(new DeviceDBRDModule()).getInstance(DeviceDB.class));

	}
}
