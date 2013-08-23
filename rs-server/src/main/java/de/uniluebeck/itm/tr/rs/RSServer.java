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

package de.uniluebeck.itm.tr.rs;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.config.ConfigWithLoggingAndProperties;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.propconf.PropConfModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.printHelpAndExit;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.setLogLevel;
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.printDocumentationAndExit;

@SuppressWarnings("restriction")
public class RSServer extends AbstractService {

	static {
		Logging.setLoggingDefaults(LogLevel.WARN);
	}

	private static final Logger log = LoggerFactory.getLogger(RSServer.class);

	private final ServicePublisher servicePublisher;

	private final RSService rsService;

	@Inject
	public RSServer(final ServicePublisher servicePublisher,
					final RSService rsService) {
		this.servicePublisher = checkNotNull(servicePublisher);
		this.rsService = checkNotNull(rsService);
	}

	@Override
	protected void doStart() {
		try {
			servicePublisher.startAndWait();
			rsService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			rsService.startAndWait();
			servicePublisher.startAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public static void main(String[] args) throws Exception {

		Thread.currentThread().setName("RS-Main");

		final ConfigWithLoggingAndProperties config = setLogLevel(
				parseOrExit(new ConfigWithLoggingAndProperties(), RSServer.class, args),
				"de.uniluebeck.itm"
		);

		if (config.helpConfig) {
			printDocumentationAndExit(
					System.out,
					CommonConfig.class,
					RSServiceConfig.class,
					RSServerConfig.class
			);
		}

		if (config.config == null) {
			printHelpAndExit(config, RSServer.class);
		}

		final PropConfModule propConfModule = new PropConfModule(
				config.config,
				CommonConfig.class,
				RSServiceConfig.class,
				RSServerConfig.class
		);
		final Injector propConfInjector = Guice.createInjector(propConfModule);
		final CommonConfig commonConfig = propConfInjector.getInstance(CommonConfig.class);
		final RSServiceConfig rsServiceConfig = propConfInjector.getInstance(RSServiceConfig.class);
		final RSServerConfig rsServerConfig = propConfInjector.getInstance(RSServerConfig.class);

		final RSServerModule rsServerModule = new RSServerModule(commonConfig, rsServiceConfig, rsServerConfig);
		final RSServer rsServer = Guice.createInjector(rsServerModule).getInstance(RSServer.class);

		try {
			rsServer.start().get();
		} catch (Exception e) {
			log.error("Could not start RS: {}", e.getMessage());
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread("RS-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down RS...");
				rsServer.stopAndWait();
				log.info("Over and out.");
			}
		}
		);

		log.info(
				"RS started (SOAP API at {})",
				"http://localhost:" + commonConfig.getPort() + rsServiceConfig.getRsContextPath());
	}
}