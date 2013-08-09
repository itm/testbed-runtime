package de.uniluebeck.itm.tr.federator.snaa;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.config.ConfigWithLoggingAndProperties;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.Guice.createInjector;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.setLogLevel;
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.buildConfig;
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.printDocumentationAndExit;

public class SNAAFederatorServer extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(SNAAFederatorServer.class);

	static {
		Logging.setLoggingDefaults(LogLevel.WARN);
	}

	private final ServicePublisher servicePublisher;

	private final SNAAFederatorService snaaFederatorService;

	@Inject
	public SNAAFederatorServer(final ServicePublisher servicePublisher, final SNAAFederatorService snaaFederatorService) {
		this.servicePublisher = checkNotNull(servicePublisher);
		this.snaaFederatorService = checkNotNull(snaaFederatorService);
	}

	@Override
	protected void doStart() {
		try {
			servicePublisher.startAndWait();
			snaaFederatorService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			snaaFederatorService.stopAndWait();
			servicePublisher.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public static void main(String[] args) {

		Thread.currentThread().setName("SNAAFederatorServer-Main");

		final ConfigWithLoggingAndProperties config = setLogLevel(
				parseOrExit(new ConfigWithLoggingAndProperties(), SNAAFederatorServer.class, args),
				"de.uniluebeck.itm"
		);

		if (config.helpConfig) {
			printDocumentationAndExit(System.out, SNAAFederatorServerConfig.class, SNAAFederatorServiceConfig.class);
		}

		final SNAAFederatorServerConfig serverConfig = buildConfig(SNAAFederatorServerConfig.class, config.config);
		final SNAAFederatorServiceConfig serviceConfig = buildConfig(SNAAFederatorServiceConfig.class, config.config);
		final SNAAFederatorServerModule serverModule = new SNAAFederatorServerModule(serverConfig, serviceConfig);
		final SNAAFederatorServer federatorServer = createInjector(serverModule).getInstance(SNAAFederatorServer.class);

		try {
			federatorServer.start().get();
		} catch (Exception e) {
			log.error("Could not start SNAA federator: {}", e.getMessage());
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread("SNAAFederatorServer-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down SNAA federator...");
				federatorServer.stopAndWait();
				log.info("Over and out.");
			}
		}
		);

		log.info("SNAA federator started!");
	}
}
