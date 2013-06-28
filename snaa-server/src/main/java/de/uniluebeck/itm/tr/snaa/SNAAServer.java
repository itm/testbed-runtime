package de.uniluebeck.itm.tr.snaa;

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

import static de.uniluebeck.itm.tr.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.setLogLevel;

public class SNAAServer extends AbstractService {

	static {
		Logging.setLoggingDefaults(LogLevel.WARN);
	}

	private static final Logger log = LoggerFactory.getLogger(SNAAServer.class);

	private final ServicePublisher servicePublisher;

	private final SNAAService snaaService;

	@Inject
	public SNAAServer(final ServicePublisher servicePublisher, final SNAAService snaaService) {
		this.servicePublisher = servicePublisher;
		this.snaaService = snaaService;
	}

	@Override
	protected void doStart() {
		try {
			servicePublisher.startAndWait();
			snaaService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			snaaService.stopAndWait();
			servicePublisher.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public static void main(String[] args) throws Exception {

		Thread.currentThread().setName("SNAA-Main");

		final ConfigWithLoggingAndProperties config = setLogLevel(
				parseOrExit(new ConfigWithLoggingAndProperties(), SNAAServer.class, args),
				"de.uniluebeck.itm"
		);

		final PropConfModule propConfModule = new PropConfModule(config.config, CommonConfig.class, SNAAConfig.class);
		final Injector propConfInjector = Guice.createInjector(propConfModule);

		final CommonConfig commonConfig = propConfInjector.getInstance(CommonConfig.class);
		final SNAAConfig snaaConfig = propConfInjector.getInstance(SNAAConfig.class);

		final SNAAServerModule module = new SNAAServerModule(commonConfig, snaaConfig);
		final SNAAServer snaa = Guice.createInjector(module).getInstance(SNAAServer.class);

		try {
			snaa.start().get();
		} catch (Exception e) {
			log.error("Could not start SNAA: {}", e.getMessage());
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread("SNAA-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down SNAA...");
				snaa.stopAndWait();
				log.info("Over and out.");
			}
		}
		);

		log.info(
				"SNAA started (SOAP API at {})",
				"http://localhost:" + commonConfig.getPort() + snaaConfig.getSnaaContextPath()
		);
	}
}
