package de.uniluebeck.itm.wisebed.cmdlineclient.scripts;

import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wisebed.cmdlineclient.WisebedBeanShellLauncher;
import de.uniluebeck.itm.wisebed.cmdlineclient.WisebedClient;
import de.uniluebeck.itm.wisebed.cmdlineclient.WisebedClientBase;
import de.uniluebeck.itm.wisebed.cmdlineclient.WisebedProtobufClient;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static de.uniluebeck.itm.wisebed.cmdlineclient.BeanShellHelper.parseSecretReservationKeys;

public class GetSupportedChannelHandlers {

	private static final Logger log = LoggerFactory.getLogger(GetSupportedChannelHandlers.class);

	public static void main(String[] args) throws Exception {

		String propertiesFile = args[0];
		WisebedBeanShellLauncher.parseAndSetProperties(propertiesFile);

		Logging.setLoggingDefaults();

		final String useProtobufStr = System.getProperty("testbed.useprotobuf");
		final boolean useProtobuf = useProtobufStr != null ? Boolean.parseBoolean(useProtobufStr) : false;
		final String secretReservationKeys = System.getProperty("testbed.secretreservationkeys");
		final String sessionManagementEndpointUrl = System.getProperty("testbed.sm.endpointurl");

		final WisebedClientBase client;
		if (useProtobuf) {

			final String protobufHost = System.getProperty("testbed.protobuf.hostname");
			final Integer protobufPort = Integer.parseInt(System.getProperty("testbed.protobuf.port"));
			client = new WisebedProtobufClient(sessionManagementEndpointUrl, protobufHost, protobufPort);

		} else {
			client = new WisebedClient(sessionManagementEndpointUrl);
		}

		final WSNAsyncWrapper wsn = client.connectToExperiment(parseSecretReservationKeys(secretReservationKeys)).get();
		List<ChannelHandlerDescription> channelHandlerDescriptions = wsn.getSupportedChannelHandlers().get();

		synchronized (System.out) {
			for (ChannelHandlerDescription chd : channelHandlerDescriptions) {

				System.out.println("ChannelHandler {");
				System.out.println("\tname=\"" + chd.getName() + "\"");
				System.out.println("\tdescription=\"" + chd.getDescription() + "\"");
				System.out.print("\tconfigurationOptions={");
				if (chd.getConfigurationOptions().size() > 0) {
					System.out.println();
					for (KeyValuePair keyValuePair : chd.getConfigurationOptions()) {
						System.out.println("\t\tkey=\"" +
								keyValuePair.getKey() +
								"\", description=\"" +
								keyValuePair.getValue() + "\""
						);
					}
					System.out.println("\t}");
				} else {
					System.out.println("}");
				}
				System.out.println("}");
			}
		}

		System.exit(0);
	}
}
