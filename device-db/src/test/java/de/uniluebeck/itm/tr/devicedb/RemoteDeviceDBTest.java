package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;

public class RemoteDeviceDBTest extends DeviceDBTestBase {

	private DeviceDBService service;

	@Before
	public void setUp() throws Exception {

		final File file = File.createTempFile("lkjsdfl", "lkjdsfl");
		final FileWriter fileWriter = new FileWriter(file);
		JPA_PROPERTIES.store(fileWriter, "");
		fileWriter.close();

		service = Guice
				.createInjector(new DeviceDBServiceModule(new DeviceDBServiceConfig(7654, file)))
				.getInstance(DeviceDBService.class);

		service.startAndWait();

		final URI remoteDeviceDBUri = URI.create("http://localhost:8888/rest");
		final RemoteDeviceDBConfig remoteDeviceDBConfig = new RemoteDeviceDBConfig(remoteDeviceDBUri);
		super.setUp(Guice.createInjector(new RemoteDeviceDBModule(remoteDeviceDBConfig)).getInstance(DeviceDB.class));
	}

	@After
	public void tearDown() throws Exception {
		service.stopAndWait();
	}
}
