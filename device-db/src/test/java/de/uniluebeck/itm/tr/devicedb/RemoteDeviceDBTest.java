package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.util.NetworkUtils;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;
import java.util.Properties;

import static de.uniluebeck.itm.util.propconf.PropConfBuilder.buildConfig;

@RunWith(MockitoJUnitRunner.class)
public class RemoteDeviceDBTest extends DeviceDBTestBase {

	static {
		Logging.setLoggingDefaults(LogLevel.WARN);
	}

	private static int port = NetworkUtils.findFreePort();

	private static DeviceDBRestService service;

	private static DeviceDBService deviceDBService;

	@Mock
	private DeviceDBConfig remoteDeviceDBConfig;

	@BeforeClass
	public static void startRemoteServer() {

		final Properties properties = new Properties();
		properties.put(CommonConfig.URN_PREFIX, "urn:wisebed:uzl1:");
		properties.put(CommonConfig.PORT, Integer.toString(port));
		properties.put(DeviceDBConfig.DEVICEDB_TYPE, DeviceDBType.IN_MEMORY.toString());
		properties.put(DeviceDBConfig.DEVICEDB_REST_API_CONTEXT_PATH, "/rest");
		properties.put(DeviceDBConfig.DEVICEDB_JPA_PROPERTIES, "");

		final CommonConfig commonConfig = buildConfig(CommonConfig.class, properties);
		final DeviceDBConfig deviceDBConfig = buildConfig(DeviceDBConfig.class, properties);

		final DeviceDBServerModule module = new DeviceDBServerModule(commonConfig, deviceDBConfig);
		service = Guice.createInjector(module).getInstance(DeviceDBRestService.class);
		service.startAndWait();
	}

	@Before
	public void setUp() throws Exception {

		final Properties properties = new Properties();
		properties.put(DeviceDBConfig.DEVICEDB_TYPE, DeviceDBType.REMOTE.toString());
		properties.put(DeviceDBConfig.DEVICEDB_REST_API_CONTEXT_PATH, "/rest");
		properties.put(DeviceDBConfig.DEVICEDB_REMOTE_URI, URI.create("http://localhost:" + port + "/rest").toString());

		final DeviceDBConfig deviceDBConfig = buildConfig(DeviceDBConfig.class, properties);

		final DeviceDBService deviceDBService = Guice
				.createInjector(new RemoteDeviceDBModule(deviceDBConfig))
				.getInstance(DeviceDBService.class);

		super.setUp(deviceDBService);
	}

	@After
	public void tearDown() throws Exception {
		deviceDBService.removeAll();
	}

	@AfterClass
	public static void stopServer() {
		service.stopAndWait();
	}
}
