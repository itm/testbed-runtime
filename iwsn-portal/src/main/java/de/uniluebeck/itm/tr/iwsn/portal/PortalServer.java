package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.config.ConfigWithLoggingAndProperties;
import de.uniluebeck.itm.tr.devicedb.DeviceDBConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBRestService;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.RestApiService;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.SoapApiService;
import de.uniluebeck.itm.tr.rs.RSService;
import de.uniluebeck.itm.tr.rs.RSServiceConfig;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.propconf.PropConfModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.setLogLevel;

public class PortalServer extends AbstractService {

	static {
		Logging.setLoggingDefaults(LogLevel.WARN);
	}

	private static final Logger log = LoggerFactory.getLogger(PortalServer.class);

	private final DeviceDBRestService deviceDBRestService;

	private final PortalEventBus portalEventBus;

	private final ReservationManager reservationManager;

	private final SoapApiService soapApiService;

	private final ServicePublisher servicePublisher;

	private final RestApiService restApiService;

	private final RSService rsService;

	@Inject
	public PortalServer(final DeviceDBRestService deviceDBRestService,
						final PortalEventBus portalEventBus,
						final ReservationManager reservationManager,
						final SoapApiService soapApiService,
						final RestApiService restApiService,
						final ServicePublisher servicePublisher,
						final RSService rsService) {
		this.deviceDBRestService = checkNotNull(deviceDBRestService);
		this.portalEventBus = checkNotNull(portalEventBus);
		this.reservationManager = checkNotNull(reservationManager);
		this.soapApiService = checkNotNull(soapApiService);
		this.restApiService = checkNotNull(restApiService);
		this.servicePublisher = checkNotNull(servicePublisher);
		this.rsService = checkNotNull(rsService);
	}

	@Override
	protected void doStart() {
		try {

			servicePublisher.startAndWait();

			deviceDBRestService.startAndWait();
			rsService.startAndWait();

			portalEventBus.startAndWait();
			reservationManager.startAndWait();
			soapApiService.startAndWait();
			restApiService.startAndWait();

			{
				final String resourceBaseDir = "/de/uniluebeck/itm/tr/iwsn/portal/wisegui";
				final String resourceBase = this.getClass().getResource(resourceBaseDir).toString();
				final ServicePublisherService webapp = servicePublisher.createServletService("/", resourceBase);
				webapp.startAndWait();
			}

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			restApiService.stopAndWait();
			soapApiService.stopAndWait();
			reservationManager.stopAndWait();
			portalEventBus.stopAndWait();

			rsService.stopAndWait();
			deviceDBRestService.stopAndWait();

			servicePublisher.stopAndWait();

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public static void main(String[] args) {

		Thread.currentThread().setName("Portal-Main");

		final ConfigWithLoggingAndProperties config = setLogLevel(
				parseOrExit(new ConfigWithLoggingAndProperties(), PortalServer.class, args),
				"de.uniluebeck.itm"
		);

		final Injector confInjector = Guice.createInjector(
				new PropConfModule(config.config, CommonConfig.class, RSServiceConfig.class, DeviceDBConfig.class,
						PortalConfig.class, SNAAConfig.class
				)
		);

		final CommonConfig commonConfig = confInjector.getInstance(CommonConfig.class);
		final RSServiceConfig rsServiceConfig = confInjector.getInstance(RSServiceConfig.class);
		final DeviceDBConfig deviceDBConfig = confInjector.getInstance(DeviceDBConfig.class);
		final PortalConfig portalConfig = confInjector.getInstance(PortalConfig.class);
		final SNAAConfig snaaConfig = confInjector.getInstance(SNAAConfig.class);

		final PortalModule portalModule = new PortalModule(
				commonConfig,
				deviceDBConfig,
				portalConfig,
				rsServiceConfig,
				snaaConfig
		);
		final PortalServer portalServer = Guice.createInjector(portalModule).getInstance(PortalServer.class);

		try {
			portalServer.start().get();
		} catch (Exception e) {
			log.error("Could not start iWSN portal: {}", e.getMessage());
			System.exit(1);
		}

		log.info("iWSN Portal started!");

		Runtime.getRuntime().addShutdownHook(new Thread("Portal-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down iWSN Portal...");
				portalServer.stopAndWait();
				log.info("Over and out.");
			}
		}
		);
	}
}
