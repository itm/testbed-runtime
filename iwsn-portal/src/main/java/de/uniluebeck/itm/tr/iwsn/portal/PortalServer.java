package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.WisemlProviderConfig;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.config.ConfigWithLoggingAndProperties;
import de.uniluebeck.itm.tr.devicedb.DeviceDBConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBRestService;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.RestApiService;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.SoapApiService;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.PortalEventStoreService;
import de.uniluebeck.itm.tr.iwsn.portal.externalplugins.ExternalPluginServiceConfig;
import de.uniluebeck.itm.tr.iwsn.portal.nodestatustracker.NodeStatusTracker;
import de.uniluebeck.itm.tr.iwsn.portal.plugins.PortalPluginService;
import de.uniluebeck.itm.tr.rs.RSService;
import de.uniluebeck.itm.tr.rs.RSServiceConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.propconf.PropConfModule;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.Guice.createInjector;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.*;
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.printDocumentationAndExit;

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

	private final SNAAService snaaService;

	private final DeviceDBService deviceDBService;

	private final WiseGuiService wiseGuiService;

	private final PortalPluginService portalPluginService;

	private final SchedulerService schedulerService;

	private final PortalEventStoreService portalEventStoreService;

	private final UserRegistrationWebAppService userRegistrationWebAppService;

	private final NodeStatusTracker nodeStatusTracker;

	@Inject
	public PortalServer(final SchedulerService schedulerService,
						final ServicePublisher servicePublisher,
						final DeviceDBService deviceDBService,
						final RSService rsService,
						final SNAAService snaaService,
						final PortalEventBus portalEventBus,
						final ReservationManager reservationManager,
						final DeviceDBRestService deviceDBRestService,
						final SoapApiService soapApiService,
						final RestApiService restApiService,
						final WiseGuiService wiseGuiService,
						final PortalPluginService portalPluginService,
						final PortalEventStoreService portalEventStoreService,
						final UserRegistrationWebAppService userRegistrationWebAppService,
						final NodeStatusTracker nodeStatusTracker) {

		this.schedulerService = checkNotNull(schedulerService);
		this.servicePublisher = checkNotNull(servicePublisher);

		this.rsService = checkNotNull(rsService);
		this.snaaService = checkNotNull(snaaService);
		this.deviceDBService = checkNotNull(deviceDBService);

		this.portalEventBus = checkNotNull(portalEventBus);
		this.portalEventStoreService = checkNotNull(portalEventStoreService);
		this.reservationManager = checkNotNull(reservationManager);

		this.deviceDBRestService = checkNotNull(deviceDBRestService);
		this.soapApiService = checkNotNull(soapApiService);
		this.restApiService = checkNotNull(restApiService);
		this.wiseGuiService = checkNotNull(wiseGuiService);
		this.userRegistrationWebAppService = checkNotNull(userRegistrationWebAppService);
		this.nodeStatusTracker = checkNotNull(nodeStatusTracker);

		this.portalPluginService = checkNotNull(portalPluginService);
	}

	@Override
	protected void doStart() {
		try {

			schedulerService.startAndWait();

			// the web server
			servicePublisher.startAndWait();

			// services that the portal depends on (either embedded or remote, depends on binding)
			rsService.startAndWait();
			snaaService.startAndWait();
			deviceDBService.startAndWait();

			// internal components of the portal server
			portalEventBus.startAndWait();
			portalEventStoreService.startAndWait();
			reservationManager.startAndWait();
			nodeStatusTracker.startAndWait();

			// services that the portal exposes to clients
			deviceDBRestService.startAndWait();
			soapApiService.startAndWait();
			restApiService.startAndWait();
			wiseGuiService.startAndWait();
			userRegistrationWebAppService.startAndWait();

			portalPluginService.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			portalPluginService.stopAndWait();

			// services that the portal server exposes to clients
			userRegistrationWebAppService.stopAndWait();
			wiseGuiService.stopAndWait();
			restApiService.stopAndWait();
			soapApiService.stopAndWait();
			deviceDBRestService.stopAndWait();

			// internal components
			nodeStatusTracker.stopAndWait();
			reservationManager.stopAndWait();
			portalEventStoreService.stopAndWait();
			portalEventBus.stopAndWait();

			// services that the portal depends on (either embedded or remote, depends on binding)
			deviceDBService.stopAndWait();
			rsService.stopAndWait();
			snaaService.stopAndWait();

			// the web server
			servicePublisher.stopAndWait();

			schedulerService.stopAndWait();

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

		if (config.helpConfig) {
			printDocumentationAndExit(
					System.out,
					CommonConfig.class,
					RSServiceConfig.class,
					DeviceDBConfig.class,
					PortalServerConfig.class,
					SNAAServiceConfig.class,
					WiseGuiServiceConfig.class
			);
		}

		if (config.config == null) {
			printHelpAndExit(config, PortalServer.class);
		}

		final Injector confInjector = createInjector(
				new PropConfModule(
						config.config,
						CommonConfig.class,
						RSServiceConfig.class,
						DeviceDBConfig.class,
						PortalServerConfig.class,
						SNAAServiceConfig.class,
						WiseGuiServiceConfig.class,
						WisemlProviderConfig.class,
						ExternalPluginServiceConfig.class
				)
		);

		final CommonConfig commonConfig = confInjector.getInstance(CommonConfig.class);
		final RSServiceConfig rsServiceConfig = confInjector.getInstance(RSServiceConfig.class);
		final DeviceDBConfig deviceDBConfig = confInjector.getInstance(DeviceDBConfig.class);
		final PortalServerConfig portalServerConfig = confInjector.getInstance(PortalServerConfig.class);
		final SNAAServiceConfig snaaServiceConfig = confInjector.getInstance(SNAAServiceConfig.class);
		final WiseGuiServiceConfig wiseGuiServiceConfig = confInjector.getInstance(WiseGuiServiceConfig.class);
		final WisemlProviderConfig wisemlProviderConfig = confInjector.getInstance(WisemlProviderConfig.class);
		final ExternalPluginServiceConfig externalPluginServiceConfig =
				confInjector.getInstance(ExternalPluginServiceConfig.class);

		final PortalModule portalModule = new PortalModule(
				commonConfig,
				deviceDBConfig,
				portalServerConfig,
				rsServiceConfig,
				snaaServiceConfig,
				wiseGuiServiceConfig,
				wisemlProviderConfig,
				externalPluginServiceConfig
		);

		final Injector portalInjector = createInjector(portalModule);
		final PortalServer portalServer = portalInjector.getInstance(PortalServer.class);

		try {
			portalServer.start().get();
		} catch (Exception e) {
			log.error("Could not start iWSN portal: {}", e.getMessage(), e);
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread("Portal-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down iWSN Portal...");
				portalServer.stopAndWait();
				log.info("Over and out.");
			}
		}
		);

		log.info("iWSN Portal started!");
	}
}
