package de.uniluebeck.itm.tr.federator.rs;

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

public class RSFederatorServer extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(RSFederatorServer.class);

	static {
		Logging.setLoggingDefaults(LogLevel.WARN);
	}

	private final ServicePublisher servicePublisher;

	private final RSFederatorService rsFederatorService;

	@Inject
	public RSFederatorServer(final ServicePublisher servicePublisher,
							 final RSFederatorService rsFederatorService) {
		this.servicePublisher = checkNotNull(servicePublisher);
		this.rsFederatorService = checkNotNull(rsFederatorService);
	}

	public static void main(String[] args) {

		Thread.currentThread().setName("RSFederatorServer-Main");

		final ConfigWithLoggingAndProperties config = setLogLevel(
				parseOrExit(new ConfigWithLoggingAndProperties(), RSFederatorServer.class, args),
				"de.uniluebeck.itm"
		);

		if (config.helpConfig) {
			printDocumentationAndExit(System.out, RSFederatorServerConfig.class, RSFederatorServiceConfig.class);
		}

		final RSFederatorServerConfig serverConfig = buildConfig(RSFederatorServerConfig.class, config.config);
		final RSFederatorServiceConfig serviceConfig = buildConfig(RSFederatorServiceConfig.class, config.config);
		final RSFederatorServerModule serverModule = new RSFederatorServerModule(serverConfig, serviceConfig);
		final RSFederatorServer server = createInjector(serverModule).getInstance(RSFederatorServer.class);

		try {
			server.start().get();
		} catch (Exception e) {
			log.error("Could not start RS federator: {}", e.getMessage());
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread("RSFederatorServer-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down RS federator...");
				server.stopAndWait();
				log.info("Over and out.");
			}
		}
		);

		log.info("RS federator started!");
	}

	@Override
	protected void doStart() {
		try {
			servicePublisher.startAndWait();
			rsFederatorService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			rsFederatorService.stopAndWait();
			servicePublisher.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
