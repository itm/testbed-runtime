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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.sm.SessionManagement;
import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.Set;


public class Federator {

	static {
		Logging.setLoggingDefaults();
	}

	private static final Logger log = LoggerFactory.getLogger(Federator.class);

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
				org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
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

		final FederatorWSNConfig config = FederatorWSNConfig.parse(properties);

		final ImmutableMap.Builder<URI, ImmutableSet<NodeUrnPrefix>> smEndpointUrlPrefixSetBuilder =
				ImmutableMap.builder();
		for (FederatorWSNTestbedConfig testbedConfig : config.getFederates()) {
			smEndpointUrlPrefixSetBuilder.put(
					testbedConfig.getSmEndpointUrl(),
					testbedConfig.getUrnPrefixes()
			);
		}

		final ImmutableMap<URI, ImmutableSet<NodeUrnPrefix>> smEndpointUrlPrefixSet =
				smEndpointUrlPrefixSetBuilder.build();

		final Function<URI, SessionManagement> uriToSessionManagementFunction = new Function<URI, SessionManagement>() {
			@Override
			public SessionManagement apply(@Nullable final URI s) {
				assert s != null;
				return WisebedServiceHelper.getSessionManagementService(s.toString());
			}
		};

		final FederationManager<SessionManagement> federationManager = new FederationManager<SessionManagement>(
				uriToSessionManagementFunction,
				smEndpointUrlPrefixSet
		);

		final SessionManagementPreconditions preconditions = new SessionManagementPreconditions();
		for (Set<NodeUrnPrefix> endpointPrefixSet : smEndpointUrlPrefixSet.values()) {
			preconditions.addServedUrnPrefixes(endpointPrefixSet);
		}

		final URI randomControllerEndpointUrl = createRandomControllerEndpointUrl(config);
		final FederatorController federatorController = new FederatorController(randomControllerEndpointUrl);

		final FederatorSessionManagement federatorSessionManagement = new FederatorSessionManagement(
				federationManager,
				preconditions,
				federatorController,
				config
		);

		log.info("Starting with config: {}", config);
		federatorSessionManagement.start();
		log.info("Started iWSN federator!");

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					federatorSessionManagement.stop();
					log.info("Stopped iWSN federator!");
				} catch (Exception e) {
					log.error("{}", e);
				}
			}
		}
		);

	}

	private static URI createRandomControllerEndpointUrl(final FederatorWSNConfig config) {
		final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();
		final URI federatorSmEndpointURL = config.getFederatorSmEndpointURL();
		return URI.create(federatorSmEndpointURL.getScheme() + "://" +
				federatorSmEndpointURL.getHost() + ":" +
				federatorSmEndpointURL.getPort() + "/" +
				secureIdGenerator.getNextId() + "/controller"
		);
	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, Federator.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}

}
