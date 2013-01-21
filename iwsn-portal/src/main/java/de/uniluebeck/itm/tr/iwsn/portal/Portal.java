package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.tr.iwsn.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.iwsn.common.config.ConfigHelper.setLogLevel;

public class Portal extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(Portal.class);

	static {
		Logging.setLoggingDefaults();
	}

	private final PortalEventBus portalEventBus;

	@Inject
	public Portal(final PortalEventBus portalEventBus) {
		this.portalEventBus = portalEventBus;
	}

	@Override
	protected void doStart() {
		try {
			portalEventBus.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			portalEventBus.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public static void main(String[] args) {

		Thread.currentThread().setName("Portal-Main");

		final PortalConfig portalConfig = setLogLevel(parseOrExit(new PortalConfig(), Portal.class, args));
		final PortalModule portalModule = new PortalModule(portalConfig);
		final Injector injector = Guice.createInjector(portalModule);
		final Portal portal = injector.getInstance(Portal.class);

		try {
			portal.start().get();
		} catch (Exception e) {
			log.error("Could not start iWSN portal: {}", e.getMessage());
			System.exit(1);
		}

		log.info("iWSN Portal started!");

		Runtime.getRuntime().addShutdownHook(new Thread("Portal-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down iWSN Portal...");
				portal.stopAndWait();
				log.info("Over and out.");
			}
		}
		);
	}
}
