/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package eu.wisebed.motap.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;

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

	public static List<eu.wisebed.api.sm.SecretReservationKey> parseSecretReservationKeys(String str) {
		String[] pairs = str.split(";");
		List<eu.wisebed.api.sm.SecretReservationKey> keys = Lists.newArrayList();
		for (String pair : pairs) {
			String urnPrefix = pair.split(",")[0];
			String secretReservationKeys = pair.split(",")[1];
			eu.wisebed.api.sm.SecretReservationKey key = new eu.wisebed.api.sm.SecretReservationKey();
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

