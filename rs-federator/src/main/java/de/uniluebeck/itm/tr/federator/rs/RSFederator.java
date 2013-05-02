package de.uniluebeck.itm.tr.federator.rs;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.util.Logging;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.setLogLevel;

public class RSFederator extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(RSFederator.class);

	static {
		Logging.setLoggingDefaults(Level.WARN);
	}

	private final ServicePublisher servicePublisher;

	private final RSFederatorService rsFederatorService;

	@Inject
	public RSFederator(final ServicePublisher servicePublisher,
					   final RSFederatorService rsFederatorService) {
		this.servicePublisher = checkNotNull(servicePublisher);
		this.rsFederatorService = checkNotNull(rsFederatorService);
	}

	public static void main(String[] args) {

		Thread.currentThread().setName("RSFederator-Main");

		final RSFederatorConfig config = setLogLevel(
				parseOrExit(new RSFederatorConfig(), RSFederator.class, args),
				"de.uniluebeck.itm"
		);

		final Injector injector = Guice.createInjector(new RSFederatorModule(config));
		final RSFederator rsFederator = injector.getInstance(RSFederator.class);

		try {
			rsFederator.start().get();
		} catch (Exception e) {
			log.error("Could not start RS federator: {}", e.getMessage());
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread("RSFederator-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down RS federator...");
				rsFederator.stopAndWait();
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
