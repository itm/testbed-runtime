package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.SoapApiService;
import de.uniluebeck.itm.tr.util.Logging;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.iwsn.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.iwsn.common.config.ConfigHelper.setLogLevel;

public class Portal extends AbstractService {

	static {

		Logging.setLoggingDefaults();

		org.apache.log4j.Logger.getLogger("org.eclipse.jetty").setLevel(Level.WARN);
		org.apache.log4j.Logger.getLogger("org.hibernate").setLevel(Level.WARN);
		org.apache.log4j.Logger.getLogger("org.hibernate.tool.hbm2ddl").setLevel(Level.WARN);
		org.apache.log4j.Logger.getLogger("org.jboss.logging").setLevel(Level.WARN);
	}

	private static final Logger log = LoggerFactory.getLogger(Portal.class);

	private final DeviceDBService deviceDBService;

	private final PortalEventBus portalEventBus;

	private final ReservationManager reservationManager;

	private final SoapApiService soapApiService;

	private final ServicePublisher servicePublisher;

	@Inject
	public Portal(final DeviceDBService deviceDBService,
				  final PortalEventBus portalEventBus,
				  final ReservationManager reservationManager,
				  final SoapApiService soapApiService,
				  final ServicePublisher servicePublisher) {
		this.deviceDBService = checkNotNull(deviceDBService);
		this.portalEventBus = checkNotNull(portalEventBus);
		this.reservationManager = checkNotNull(reservationManager);
		this.soapApiService = checkNotNull(soapApiService);
		this.servicePublisher = checkNotNull(servicePublisher);
	}

	@Override
	protected void doStart() {
		try {

			deviceDBService.startAndWait();
			portalEventBus.startAndWait();
			reservationManager.startAndWait();
			servicePublisher.startAndWait();
			soapApiService.startAndWait();

			final String resourceBaseDir = "/de/uniluebeck/itm/tr/iwsn/portal/webapp";
			final String resourceBase = this.getClass().getResource(resourceBaseDir).toString();
			final ServicePublisherService webapp = servicePublisher.createServletService("/", resourceBase);
			webapp.startAndWait();


			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			deviceDBService.stopAndWait();
			soapApiService.stopAndWait();
			servicePublisher.stopAndWait();
			reservationManager.stopAndWait();
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
