package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.config.ConfigWithLoggingAndProperties;
import de.uniluebeck.itm.util.propconf.PropConfBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.setLogLevel;
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.buildConfig;

public class IWSNFederatorServer extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(IWSNFederatorServer.class);

	private final SessionManagementFederatorService sessionManagementFederatorService;

	private final ServicePublisher servicePublisher;

	@Inject
	public IWSNFederatorServer(final ServicePublisher servicePublisher,
							   final SessionManagementFederatorService sessionManagementFederatorService) {
		this.servicePublisher = servicePublisher;
		this.sessionManagementFederatorService = checkNotNull(sessionManagementFederatorService);
	}

	@Override
	protected void doStart() {
		try {
			servicePublisher.startAndWait();
			sessionManagementFederatorService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			if (sessionManagementFederatorService.isRunning()) {
				sessionManagementFederatorService.stopAndWait();
			}

			if (servicePublisher.isRunning()) {
				servicePublisher.stopAndWait();
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
			PropConfBuilder.printDocumentation(System.out, IWSNFederatorServerConfig.class);
			System.exit(1);
		}

		final IWSNFederatorServerConfig federatorConfig = buildConfig(IWSNFederatorServerConfig.class, config.config);
		final IWSNFederatorServerModule federatorModule = new IWSNFederatorServerModule(federatorConfig);
		final IWSNFederatorServer federator =
				Guice.createInjector(federatorModule).getInstance(IWSNFederatorServer.class);

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
