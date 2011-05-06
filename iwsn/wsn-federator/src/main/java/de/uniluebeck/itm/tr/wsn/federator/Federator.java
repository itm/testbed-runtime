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

package de.uniluebeck.itm.tr.wsn.federator;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;


public class Federator {

	private static class FederatedTestbedConfig {

		public String sessionManagementEndpointUrl;

		public Set<String> urnPrefixes = new HashSet<String>();

		@Override
		public String toString() {
			return "FederatedTestbedConfig{" +
					"sessionManagementEndpointUrl='" + sessionManagementEndpointUrl + '\'' +
					", urnPrefixes=" + urnPrefixes +
					"}";
		}
	}

	private static class Config {

		public static Config parseConfig(Properties properties) {

			Config config = new Config();

			config.port = Integer.parseInt((String) properties.get("port"));
			config.path = (String) properties.get("path");
			checkNotNull(config.path);
			config.sessionmanagementEndpointURL = (String) properties.get("sessionmanagement_endpoint_url");
			config.reservationEndpointUrl = (String) properties.get("reservation_endpoint_url");
			config.snaaEndpointUrl = (String) properties.get("snaa_endpoint_url");

			String[] federates = ((String) properties.get("federates")).split(",");

			for (String federate : federates) {
				FederatedTestbedConfig federatedTestbedConfig = new FederatedTestbedConfig();
				federatedTestbedConfig.sessionManagementEndpointUrl =
						(String) properties.get(federate.trim() + ".sessionmanagement_endpoint_url");
				String[] urnPrefixes =
						((String) properties.get(federate.trim() + ".urnprefixes")).split(",");
				for (String urnPrefix : urnPrefixes) {
					federatedTestbedConfig.urnPrefixes.add(urnPrefix.trim());
				}
				config.federates.add(federatedTestbedConfig);
			}

			return config;

		}

		private String snaaEndpointUrl;

		public int port;

		public String path;

		public String sessionmanagementEndpointURL;

		public String reservationEndpointUrl;

		public List<FederatedTestbedConfig> federates = new ArrayList<FederatedTestbedConfig>();

		@Override
		public String toString() {
			return "Config{" +
					"port=" + port +
					", path='" + path + '\'' +
					", sessionmanagementEndpointURL='" + sessionmanagementEndpointURL + '\'' +
					", reservationEndpointUrl='" + reservationEndpointUrl + '\'' +
					", snaaEndpointUrl='" + snaaEndpointUrl + '\'' +
					", federates=" + federates +
					"}";
		}
	}

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Federator.class);

	public static void main(String[] args) throws Exception {

		Properties properties = null;

		// create the command line parser
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("f", "file", true, "The properties file");
		options.addOption("v", "verbose", false, "Verbose logging output");
		options.addOption("h", "help", false, "Help output");

		try {

			CommandLine line = parser.parse(options, args);

			if (line.hasOption('v')) {
				Logger.getRootLogger().setLevel(Level.DEBUG);
			}

			if (line.hasOption('h')) {
				usage(options);
			}

			if (line.hasOption('f')) {

				File file = new File(line.getOptionValue('f'));
				properties = new Properties();
				try {
					properties.load(new FileInputStream(file));
				} catch (IOException e) {
					log.error("Error while reading properties file: " + e, e);
					System.exit(1);
				}

			} else {
				throw new Exception("Please supply -f");
			}

		} catch (Exception e) {
			log.error("Invalid command line: " + e, e);
			usage(options);
		}

		Config config = Config.parseConfig(properties);

		log.info("Starting with config: {}", config);

		BiMap<String, Set<String>> sessionManagementEndpointUrlPrefixSet = HashBiMap.create();
		for (FederatedTestbedConfig testbedConfig : config.federates) {
			sessionManagementEndpointUrlPrefixSet
					.put(testbedConfig.sessionManagementEndpointUrl, testbedConfig.urnPrefixes);
		}

		String localhost = config.sessionmanagementEndpointURL;

		if (localhost == null)
			localhost = InetAddress.getLocalHost().getHostName();

		String endpointUrlBase = "http://" + localhost + ":" + config.port + "/";
		String path = config.path.startsWith("/") ? config.path : "/" + config.path;
		String reservationEndpointUrl =
				config.reservationEndpointUrl == null || "".equals(config.reservationEndpointUrl) ? null :
						config.reservationEndpointUrl;
		String snaaEndpointUrl =
				config.snaaEndpointUrl == null || "".equals(config.snaaEndpointUrl) ? null :
						config.snaaEndpointUrl;

		final FederatorSessionManagement federatorSessionManagement =
				new FederatorSessionManagement(sessionManagementEndpointUrlPrefixSet, endpointUrlBase, path,
						reservationEndpointUrl, snaaEndpointUrl
				);

		federatorSessionManagement.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					federatorSessionManagement.stop();
				} catch (Exception e) {
					log.error("{}", e);
				}
			}
		});

	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, Federator.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}

}
