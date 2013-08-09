package de.uniluebeck.itm.tr.tools.wisemltodevicedb;

import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfig;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.devicedb.RemoteDeviceDBModule;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Setup;
import eu.wisebed.wiseml.WiseMLHelper;
import eu.wisebed.wiseml.Wiseml;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

import static com.google.common.collect.Sets.newHashSet;

public class WiseMLToDeviceDBConverter {

	public static void main(String[] args) throws IOException {

		if (args.length < 2) {
			System.out.println("Usage: WiseMLToDeviceDBConverter wiseml-file device-db-rest-uri");
			System.exit(1);
		}

		final File wiseMLFile = new File(args[0]);
		final String wiseMLString = Files.toString(wiseMLFile, Charset.defaultCharset());
		final Wiseml wiseml = WiseMLHelper.deserialize(wiseMLString);

		final Injector injector = Guice.createInjector(new RemoteDeviceDBModule(URI.create(args[1])));
		final DeviceDBService deviceDBService = injector.getInstance(DeviceDBService.class);

		deviceDBService.startAndWait();

		for (Setup.Node node : wiseml.getSetup().getNode()) {
			final DeviceConfig deviceConfig = new DeviceConfig(
					new NodeUrn(node.getId()),
					node.getNodeType(),
					node.isGateway(),
					null,
					node.getDescription(),
					null,
					null,
					"isense".equals(node.getNodeType()) ?
							new ChannelHandlerConfigList(new ChannelHandlerConfig("dlestxetx-framing")) :
							null,
					null,
					null,
					null,
					null,
					node.getPosition(),
					newHashSet(node.getCapability())
			);
			deviceDBService.add(deviceConfig);
		}

		deviceDBService.stopAndWait();
	}

}
