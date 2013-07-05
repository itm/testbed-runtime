package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.config.ConfigWithLoggingAndProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.inject.Guice.createInjector;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.setLogLevel;
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.buildConfig;
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.printDocumentationAndExit;

public class IWSNFederatorServer extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(IWSNFederatorServer.class);

	private final ServicePublisher servicePublisher;

	private final IWSNFederatorService iwsnFederatorService;

	@Inject
	public IWSNFederatorServer(final ServicePublisher servicePublisher,
							   final IWSNFederatorService iwsnFederatorService) {
		this.servicePublisher = servicePublisher;
		this.iwsnFederatorService = iwsnFederatorService;
	}

	@Override
	protected void doStart() {
		try {
			servicePublisher.startAndWait();
			iwsnFederatorService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			if (servicePublisher.isRunning()) {
				servicePublisher.stopAndWait();
			}
			if (iwsnFederatorService.isRunning()) {
				iwsnFederatorService.startAndWait();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public static void main(String[] args) {

		Thread.currentThread().setName("IWSNFederatorServer-Main");

		final ConfigWithLoggingAndProperties config = setLogLevel(
				parseOrExit(new ConfigWithLoggingAndProperties(), IWSNFederatorServer.class, args),
				"de.uniluebeck.itm"
		);

		if (config.helpConfig) {
			printDocumentationAndExit(System.out, IWSNFederatorServerConfig.class, IWSNFederatorServiceConfig.class);
		}

		final IWSNFederatorServerConfig serverConfig = buildConfig(IWSNFederatorServerConfig.class, config.config);
		final IWSNFederatorServiceConfig serviceConfig = buildConfig(IWSNFederatorServiceConfig.class, config.config);
		final IWSNFederatorServerModule federatorModule = new IWSNFederatorServerModule(serverConfig, serviceConfig);
		final IWSNFederatorServer federator = createInjector(federatorModule).getInstance(IWSNFederatorServer.class);

		try {
			federator.start().get();
		} catch (Exception e) {
			log.error("Could not start iWSN federator: {}", e.getMessage());
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread("IWSNFederatorServer-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down IWSN federator...");
				federator.stopAndWait();
				log.info("Over and out.");
			}
		}
		);

		log.info("IWSN federator started!");
	}
}
