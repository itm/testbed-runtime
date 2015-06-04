package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import de.uniluebeck.itm.tr.common.*;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactory;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactoryImpl;
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
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class RemoteDeviceDBTest extends DeviceDBTestBase {

	private static int port = NetworkUtils.findFreePort();
	private static int portWithoutService = NetworkUtils.findFreePort();
	private static DeviceDBServer deviceDBServer;
	private static DeviceDBService deviceDBService;

	static {
		Logging.setLoggingDefaults(LogLevel.ERROR);
	}

	@Mock
	private DeviceDBConfig remoteDeviceDBConfig;

	private DeviceDBService remoteDeviceDBService;

	@BeforeClass
	public static void startRemoteServer() {

		final Properties properties = new Properties();
		properties.put(CommonConfig.URN_PREFIX, "urn:wisebed:uzl1:");
		properties.put(CommonConfig.PORT, Integer.toString(port));
		properties.put(DeviceDBConfig.DEVICEDB_TYPE, DeviceDBType.IN_MEMORY.toString());
		properties.put(DeviceDBConfig.DEVICEDB_JPA_PROPERTIES, "");

		final CommonConfig commonConfig = buildConfig(CommonConfig.class, properties);
		final DeviceDBConfig deviceDBConfig = buildConfig(DeviceDBConfig.class, properties);
		final WisemlProviderConfig wisemlProviderConfig = buildConfig(WisemlProviderConfig.class, properties);
		final DeviceDBServerModule module = new DeviceDBServerModule(commonConfig, deviceDBConfig, wisemlProviderConfig);
		final AbstractModule mockEventBusModule = new AbstractModule() {
			@Override
			protected void configure() {
				bind(EventBusService.class).toInstance(mock(EventBusService.class));
				bind(IdProvider.class).to(IncrementalIdProvider.class).in(Scopes.SINGLETON);
				bind(TimestampProvider.class).to(UnixTimestampProvider.class).in(Scopes.SINGLETON);
				bind(MessageFactory.class).to(MessageFactoryImpl.class).in(Scopes.SINGLETON);
			}
		};
		final Injector injector = Guice.createInjector(mockEventBusModule, module);

		deviceDBServer = injector.getInstance(DeviceDBServer.class);
		deviceDBServer.startAsync().awaitRunning();
		deviceDBService = injector.getInstance(DeviceDBService.class);
	}

	@AfterClass
	public static void stopServer() {
		deviceDBServer.stopAsync().awaitTerminated();
	}

	@Before
	public void setUp() throws Exception {
		remoteDeviceDBService = Guice.createInjector(testModule, new RemoteDeviceDBModule(
				URI.create("http://localhost:" + port + Constants.DEVICE_DB.DEVICEDB_REST_API_CONTEXT_PATH_VALUE),
				URI.create("http://localhost:" + port + Constants.DEVICE_DB.DEVICEDB_REST_API_CONTEXT_PATH_VALUE),
				"admin",
				"secret"
		)).getInstance(DeviceDBService.class);
		super.setUp(remoteDeviceDBService);
	}

	@After
	public void tearDown() throws Exception {
		deviceDBService.removeAll();
	}

	@Test(expected = Exception.class)
	public void testIfGetByMacAddressThrowsExceptionIfRemoteUriDoesNotRunAService() {
		DeviceDBService db = Guice.createInjector(new RemoteDeviceDBModule(
				URI.create("http://localhost:" + portWithoutService + "/rest/wrong/uri"),
				URI.create("http://localhost:" + portWithoutService + "/rest/wrong/uri"),
				"admin",
				"secret"
		)).getInstance(DeviceDBService.class);
		db.startAsync().awaitRunning();
		assertNull(db.getConfigByMacAddress(0x1234L));
	}
}
