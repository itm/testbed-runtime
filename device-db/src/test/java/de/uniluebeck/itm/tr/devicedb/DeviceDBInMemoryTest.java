package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeviceDBInMemoryTest extends DeviceDBTestBase {

	@Before
	public void setUp() throws Exception {
		final DeviceDBService deviceDBService = Guice
				.createInjector(testModule, new DeviceDBInMemoryModule())
				.getInstance(DeviceDBService.class);
		super.setUp(deviceDBService);
	}
}
