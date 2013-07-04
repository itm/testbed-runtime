package de.uniluebeck.itm.tr.federator.snaa;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.config.ConfigWithLoggingAndProperties;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.propconf.PropConfBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.setLogLevel;
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.buildConfig;

public class SNAAFederator extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(SNAAFederator.class);

	static {
		Logging.setLoggingDefaults(LogLevel.WARN);
	}

	private final ServicePublisher servicePublisher;

	private final SNAAFederatorService snaaFederatorService;

	@Inject
	public SNAAFederator(final ServicePublisher servicePublisher, final SNAAFederatorService snaaFederatorService) {
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

		Thread.currentThread().setName("SNAAFederator-Main");

		final ConfigWithLoggingAndProperties config = setLogLevel(
				parseOrExit(new ConfigWithLoggingAndProperties(), SNAAFederator.class, args),
				"de.uniluebeck.itm"
		);

		if (config.helpConfig) {
			PropConfBuilder.printDocumentation(System.out, SNAAFederatorConfig.class);
			System.exit(1);
		}

		final SNAAFederatorConfig snaaFederatorConfig = buildConfig(SNAAFederatorConfig.class, config.config);
		final SNAAFederatorModule snaaFederatorModule = new SNAAFederatorModule(snaaFederatorConfig);
		final SNAAFederator snaaFederator = Guice.createInjector(snaaFederatorModule).getInstance(SNAAFederator.class);

		try {
			snaaFederator.start().get();
		} catch (Exception e) {
			log.error("Could not start SNAA federator: {}", e.getMessage());
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread("SNAAFederator-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down SNAA federator...");
				snaaFederator.stopAndWait();
				log.info("Over and out.");
			}
		}
		);

		log.info("SNAA federator started!");
	}
}
