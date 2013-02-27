package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import eu.wisebed.api.v3.common.NodeUrn;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) {

		final RemoteDeviceDBConfig remoteDeviceDBConfig = new RemoteDeviceDBConfig(URI.create("http://localhost:7654/rest"));
		final RemoteDeviceDBModule remoteDeviceDBModule = new RemoteDeviceDBModule(remoteDeviceDBConfig);

		final DeviceDB deviceDB = Guice.createInjector(remoteDeviceDBModule).getInstance(DeviceDB.class);

		deviceDB.add(new DeviceConfig(
				new NodeUrn("urn:wisebed:uzl1:0x1234"),
				"isense48",
				false,
				null,
				"lkajsdfl",
				null,
				null,
				TimeUnit.SECONDS.toMillis(1),
				TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5),
				TimeUnit.SECONDS.toMillis(2),
				null
		));

		System.out.println(deviceDB.getAll());
	}
}
