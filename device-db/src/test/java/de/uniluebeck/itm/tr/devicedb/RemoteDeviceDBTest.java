package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.common.WisemlProviderConfig;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.util.NetworkUtils;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;
import java.util.Properties;

import static de.uniluebeck.itm.util.propconf.PropConfBuilder.buildConfig;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class RemoteDeviceDBTest extends DeviceDBTestBase {

	static {
		Logging.setLoggingDefaults(LogLevel.WARN);
	}

	private static int port = NetworkUtils.findFreePort();

	private static int portWithoutService = NetworkUtils.findFreePort();

	private static DeviceDBServer deviceDBServer;

	private static DeviceDBService deviceDBService;

	@Mock
	private DeviceDBConfig remoteDeviceDBConfig;

	private DeviceDBService remoteDeviceDBService;

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
		final WisemlProviderConfig wisemlProviderConfig = buildConfig(WisemlProviderConfig.class, properties);

		final DeviceDBServerModule module = new DeviceDBServerModule(commonConfig, deviceDBConfig, wisemlProviderConfig);
		final Injector injector = Guice.createInjector(module);
		deviceDBServer = injector.getInstance(DeviceDBServer.class);
		deviceDBServer.startAndWait();
		deviceDBService = injector.getInstance(DeviceDBService.class);
	}

	@Before
	public void setUp() throws Exception {

		remoteDeviceDBService = Guice
				.createInjector(new RemoteDeviceDBModule(URI.create("http://localhost:" + port + "/rest")))
				.getInstance(DeviceDBService.class);

		super.setUp(remoteDeviceDBService);
	}

	@After
	public void tearDown() throws Exception {
		deviceDBService.removeAll();
	}

	@AfterClass
	public static void stopServer() {
		deviceDBServer.stopAndWait();
	}

	@Test(expected = Exception.class)
	public void testIfGetByMacAddressThrowsExceptionIfRemoteUriDoesNotRunAService() {

		DeviceDBService db = Guice.createInjector(
				new RemoteDeviceDBModule(URI.create("http://localhost:" + portWithoutService + "/rest/wrong/uri"))
		).getInstance(DeviceDBService.class);
		db.startAndWait();

		assertNull(db.getConfigByMacAddress(0x1234L));
	}
}
