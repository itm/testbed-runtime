package de.uniluebeck.itm.tr.federator;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.config.ConfigWithLoggingAndProperties;
import de.uniluebeck.itm.tr.federator.iwsn.IWSNFederatorServiceConfig;
import de.uniluebeck.itm.tr.federator.rs.RSFederatorServiceConfig;
import de.uniluebeck.itm.tr.federator.snaa.SNAAFederatorServiceConfig;
import de.uniluebeck.itm.tr.iwsn.portal.WiseGuiService;
import de.uniluebeck.itm.tr.iwsn.portal.WiseGuiServiceConfig;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.RestApiService;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.propconf.PropConfModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.inject.Guice.createInjector;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.*;
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.printDocumentationAndExit;

public class FederatorServer extends AbstractService {

	static {
		Logging.setLoggingDefaults(LogLevel.ERROR);
	}

	public static final Logger log = LoggerFactory.getLogger(FederatorServer.class);

	private final FederatorService federatorService;

	private final ServicePublisher servicePublisher;

	private final RestApiService restApiService;

	private final WiseGuiService wiseGuiService;

	@Inject
	public FederatorServer(final FederatorService federatorService,
						   final ServicePublisher servicePublisher,
						   final RestApiService restApiService,
						   final WiseGuiService wiseGuiService) {
		this.federatorService = federatorService;
		this.servicePublisher = servicePublisher;
		this.restApiService = restApiService;
		this.wiseGuiService = wiseGuiService;
	}

	@Override
	protected void doStart() {
		try {
			servicePublisher.startAndWait();
			federatorService.startAndWait();
			restApiService.startAndWait();
			wiseGuiService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			if (wiseGuiService.isRunning()) {
				wiseGuiService.stopAndWait();
			}
			if (restApiService.isRunning()) {
				restApiService.stopAndWait();
			}
			if (federatorService.isRunning()) {
				federatorService.stopAndWait();
			}
			if (servicePublisher.isRunning()) {
				servicePublisher.stopAndWait();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public static void main(String[] args) throws Exception {

		Thread.currentThread().setName("Federator-Main");

		final ConfigWithLoggingAndProperties config = setLogLevel(
				parseOrExit(new ConfigWithLoggingAndProperties(), FederatorServer.class, args),
				"de.uniluebeck.itm"
		);

		if (config.helpConfig) {
			printDocumentationAndExit(
					System.out,
					FederatorServerConfig.class,
					IWSNFederatorServiceConfig.class,
					RSFederatorServiceConfig.class,
					SNAAFederatorServiceConfig.class
			);
		}

		if (config.config == null) {
			printHelpAndExit(config, FederatorServer.class);
		}

		final PropConfModule propConfModule = new PropConfModule(
				config.config,
				FederatorServerConfig.class,
				IWSNFederatorServiceConfig.class,
				RSFederatorServiceConfig.class,
				SNAAFederatorServiceConfig.class,
				WiseGuiServiceConfig.class
		);

		final Injector conf = createInjector(propConfModule);
		final FederatorServerConfig federatorServerConfig = conf.getInstance(FederatorServerConfig.class);
		final IWSNFederatorServiceConfig iwsnFederatorServiceConfig = conf.getInstance(IWSNFederatorServiceConfig.class);
		final RSFederatorServiceConfig rsFederatorServiceConfig = conf.getInstance(RSFederatorServiceConfig.class);
		final SNAAFederatorServiceConfig snaaFederatorServiceConfig = conf.getInstance(SNAAFederatorServiceConfig.class);
		final WiseGuiServiceConfig wiseGuiServiceConfig = conf.getInstance(WiseGuiServiceConfig.class);

		final FederatorServerModule serverModule = new FederatorServerModule(
				federatorServerConfig,
				iwsnFederatorServiceConfig,
				rsFederatorServiceConfig,
				snaaFederatorServiceConfig,
				wiseGuiServiceConfig
		);
		final FederatorServer federatorServer = createInjector(serverModule).getInstance(FederatorServer.class);

		try {
			federatorServer.start().get();
		} catch (Exception e) {
			log.error("Could not start Federator: {}", e.getMessage());
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread("Federator-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down Federator...");
				federatorServer.stopAndWait();
				log.info("Over and out.");
			}
		}
		);

		log.info("Federator started");
	}
}
