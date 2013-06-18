package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.*;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.util.NetworkUtils;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.net.URI;

public class RemoteDeviceDBTest extends DeviceDBTestBase {

	static {
		Logging.setLoggingDefaults(LogLevel.WARN);
	}

	private static int port = NetworkUtils.findFreePort();

	private static DeviceDBService service;

	private static DeviceDB deviceDB;

	@BeforeClass
	public static void startServer() {

		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				install(new ServicePublisherCxfModule());
				install(new DeviceDBInMemoryModule());
				install(new DeviceDBServiceModule());
			}

			@Provides
			@Singleton
			ServicePublisher provideServicePublisher(final ServicePublisherFactory factory) {
				return factory.create(new ServicePublisherConfig(port));
			}
		}
		);

		deviceDB = injector.getInstance(DeviceDB.class);
		service = injector.getInstance(DeviceDBServiceFactory.class).create("/rest", "/");
		service.startAndWait();
	}

	@Before
	public void setUp() throws Exception {
		final URI remoteDeviceDBUri = URI.create("http://localhost:" + port + "/rest");
		final RemoteDeviceDBConfig remoteDeviceDBConfig = new RemoteDeviceDBConfig(remoteDeviceDBUri);
		super.setUp(Guice.createInjector(new RemoteDeviceDBModule(remoteDeviceDBConfig)).getInstance(DeviceDB.class));
	}

	@After
	public void tearDown() throws Exception {
		deviceDB.removeAll();
	}

	@AfterClass
	public static void stopServer() {
		service.stopAndWait();
	}
}
