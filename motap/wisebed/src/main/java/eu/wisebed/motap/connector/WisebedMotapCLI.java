package eu.wisebed.motap.connector;

import com.coalesenses.otap.core.MotapController;
import com.coalesenses.otap.core.OtapPlugin;
import com.coalesenses.otap.core.cli.AbstractOtapCLI;
import com.coalesenses.otap.core.cli.OtapConfig;
import com.coalesenses.otap.core.connector.DeviceConnector;
import com.coalesenses.otap.core.connector.DeviceConnectorListener;
import com.coalesenses.otap.core.seraerial.SerAerialPacket;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.net.httpserver.HttpServer;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v211.SecretReservationKey;
import eu.wisebed.testbed.api.wsn.v211.SessionManagement;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Executors;

public class WisebedMotapCLI extends AbstractOtapCLI {

	private static final Logger log = LoggerFactory.getLogger(WisebedMotapCLI.class);

	private HttpServer server;

	private Config config;

	private DeviceConnector device;

	private static OtapPlugin otapPlugin;

	private static class Config extends OtapConfig {

		String sessionManagementEndpointURL;

		List<SecretReservationKey> secretReservationKeys;

		Integer port;

		String nodeURN;

	}

	public static void main(String[] args) throws UnknownHostException {

		Logging.setLoggingDefaults();

		WisebedMotapCLI wisebedMotapCLI = new WisebedMotapCLI();
		wisebedMotapCLI.parseCmdLine(args);

		Runtime.getRuntime().addShutdownHook(wisebedMotapCLI.shutdownHook);

		wisebedMotapCLI.startHttpServer();
		wisebedMotapCLI.setUpDeviceConnector();
		wisebedMotapCLI.startProgramming();

		System.exit(0);

	}

	private void startProgramming() {

		otapPlugin = new OtapPlugin(device);
		otapPlugin.setChannel(config.channel);
		otapPlugin.setMultihopSupportState(config.multihop);
		otapPlugin.setOtapKey(null, false);
		otapPlugin.setProgramFilename(config.program);

		device.addListener(new DeviceConnectorListener() {
			@Override
			public void handleDevicePacket(SerAerialPacket p) {
				otapPlugin.handleDevicePacket(p);
			}
		}
		);

		MotapController motapController = new MotapController();
		motapController.executeProgramming(otapPlugin, config);

	}

	private void setUpDeviceConnector() {

		SessionManagement sessionManagement =
				WSNServiceHelper.getSessionManagementService(config.sessionManagementEndpointURL);

		try {

			device = WisebedMotapConnectorFactory
					.create(sessionManagement, server, config.secretReservationKeys, config.nodeURN);

		} catch (Exception e) {
			log.error("" + e, e);
			System.exit(1);
		}

	}

	private Thread shutdownHook = new Thread(new Runnable() {
		@Override
		public void run() {
			if (device != null) {
				device.shutdown();
			}
			if (server != null) {
				server.stop(1);
			}
		}
	}
	);

	private void startHttpServer() {

		if (config.port == null) {

			config.port = UrlUtils.getRandomUnprivilegedPort();

			while (!startHttpServer(config.port)) {
				if (log.isDebugEnabled()) {
					log.debug(
							"Could not start controller endpoint at {}. Retrying with another random unprivileged port..."
					);
				}
				config.port = UrlUtils.getRandomUnprivilegedPort();
			}

		} else {

			if (!startHttpServer(config.port)) {
				throw new IllegalArgumentException(
						"Controller endpoint could not be started. Please see the log for details!"
				);
			}

		}

		log.debug("Successfully started HTTP server for local controller endpoint at port {}", config.port);
	}

	private boolean startHttpServer(int port) {

		try {
			server = HttpServer.create(new InetSocketAddress(port), 5);
			server.setExecutor(Executors.newCachedThreadPool(
					new ThreadFactoryBuilder().setNameFormat("Controller-Thread %d").build()
			)
			);
			server.start();
		} catch (IOException e) {
			log.warn("Exception while starting HTTP server for local controller endpoint on port {}. Reason: {}", port,
					e
			);
			return false;
		}

		return true;
	}

	public static List<eu.wisebed.testbed.api.wsn.v211.SecretReservationKey> parseSecretReservationKeys(String str) {
		String[] pairs = str.split(";");
		List<eu.wisebed.testbed.api.wsn.v211.SecretReservationKey> keys = Lists.newArrayList();
		for (String pair : pairs) {
			String urnPrefix = pair.split(",")[0];
			String secretReservationKeys = pair.split(",")[1];
			eu.wisebed.testbed.api.wsn.v211.SecretReservationKey key =
					new eu.wisebed.testbed.api.wsn.v211.SecretReservationKey();
			key.setUrnPrefix(urnPrefix);
			key.setSecretReservationKey(secretReservationKeys);
			keys.add(key);
		}
		return keys;
	}

	private Config parseCmdLine(String[] args) {

		config = new Config();
		Options options = createOptions();

		try {

			CommandLineParser parser = new PosixParser();
			CommandLine commandLine = parser.parse(options, args);

			super.parseCmdLine(commandLine, options, config);

			config.sessionManagementEndpointURL = commandLine.getOptionValue('s');
			config.secretReservationKeys = parseSecretReservationKeys(commandLine.getOptionValue('r'));
			config.nodeURN = commandLine.getOptionValue('n');

			if (config.secretReservationKeys.size() == 0) {
				throw new RuntimeException("You must at least supply one secret reservation key.");
			}

			if (commandLine.hasOption('p')) {
				try {
					config.port = Integer.parseInt(commandLine.getOptionValue('p'));
					if (config.port < 0 || config.port > (int) Math.pow(2, 16)) {
						throw new NumberFormatException();
					}
				} catch (NumberFormatException e) {
					log.error("Unable to parse port number. Please supply a valid port number between 0 and 65535.");
					System.exit(1);
				}
			}

		} catch (Exception e) {
			log.error("Invalid command line: " + e, e);
			usage(options);
			System.exit(1);
		}

		assert config.sessionManagementEndpointURL != null;
		assert config.secretReservationKeys != null || config.secretReservationKeys.size() > 0;

		return config;

	}

	protected Options createOptions() {

		Options options = super.createOptions();

		// session management endpoint url
		Option sessionManaqementOption = new Option("s", "sessionmanagement", true,
				"The endpoint URL of the session management service of the testbed."
		);
		sessionManaqementOption.setRequired(true);
		options.addOption(sessionManaqementOption);

		// secret reservation keys
		Option reservationKeysOptions =
				new Option("r", "reservationkeys", true,
						"The (secret) reservation keys that were returned by the reservation system."
				);
		reservationKeysOptions.setRequired(true);
		options.addOption(reservationKeysOptions);

		// controllerendpointurl
		options.addOption("p", "port", true,
				"The port of the local controller that is started as a feedback channel for testbed outputs."
		);

		// nodeURN
		Option nodeURNOption = new Option("n", "nodeurn", true, "The URN of the node to attach to.");
		nodeURNOption.setRequired(true);
		options.addOption(nodeURNOption);

		return options;
	}

}

