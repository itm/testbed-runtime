package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.NetworkUtils;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;

public class RemoteDeviceDBTest extends DeviceDBTestBase {

	static {
		Logging.setLoggingDefaults(Level.WARN);
	}

	private static int port = NetworkUtils.findFreePort();

	private DeviceDBService service;

	@Before
	public void setUp() throws Exception {

		final File file = File.createTempFile("lkjsdfl", "lkjdsfl");
		final FileWriter fileWriter = new FileWriter(file);
		JPA_PROPERTIES.store(fileWriter, "");
		fileWriter.close();

		service = Guice
				.createInjector(new DeviceDBMainModule(new DeviceDBMainConfig(port, file)))
				.getInstance(DeviceDBServiceFactory.class)
				.create("/rest", "/");

		service.startAndWait();

		final URI remoteDeviceDBUri = URI.create("http://localhost:" + port + "/rest");
		final RemoteDeviceDBConfig remoteDeviceDBConfig = new RemoteDeviceDBConfig(remoteDeviceDBUri);
		super.setUp(Guice.createInjector(new RemoteDeviceDBModule(remoteDeviceDBConfig)).getInstance(DeviceDB.class));
	}

	@After
	public void tearDown() throws Exception {
		service.stopAndWait();
	}
}
