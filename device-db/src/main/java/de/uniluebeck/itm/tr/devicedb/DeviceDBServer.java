package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.Constants;
import de.uniluebeck.itm.tr.common.WisemlProviderConfig;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.config.ConfigWithLoggingAndProperties;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.propconf.PropConfModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.Guice.createInjector;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.*;
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.printDocumentationAndExit;

public class DeviceDBServer extends AbstractService {

	static {
		Logging.setLoggingDefaults();
	}

	private static final Logger log = LoggerFactory.getLogger(DeviceDBServer.class);

	private final DeviceDBService deviceDBService;

	private final DeviceDBRestService deviceDBRestService;

	private final ServicePublisher servicePublisher;

	@Inject
	public DeviceDBServer(final ServicePublisher servicePublisher,
						  final DeviceDBRestService deviceDBRestService,
						  final DeviceDBService deviceDBService) {
		this.servicePublisher = checkNotNull(servicePublisher);
		this.deviceDBRestService = checkNotNull(deviceDBRestService);
		this.deviceDBService = checkNotNull(deviceDBService);
	}

	@Override
	protected void doStart() {
		try {
			servicePublisher.startAsync().awaitRunning();
			deviceDBService.startAsync().awaitRunning();
			deviceDBRestService.startAsync().awaitRunning();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			if (deviceDBRestService.isRunning()) {
				deviceDBRestService.stopAsync().awaitTerminated();
			}

			if (deviceDBService.isRunning()) {
				deviceDBService.stopAsync().awaitTerminated();
			}

			if (servicePublisher.isRunning()) {
				servicePublisher.stopAsync().awaitTerminated();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public static void main(String[] args) {

		Thread.currentThread().setName("DeviceDB-Main");

		final ConfigWithLoggingAndProperties config = setLogLevel(
				parseOrExit(new ConfigWithLoggingAndProperties(), DeviceDBServer.class, args),
				"de.uniluebeck.itm"
		);

		if (config.helpConfig) {
			printDocumentationAndExit(System.out, CommonConfig.class, DeviceDBConfig.class);
		}

		if (config.config == null) {
			printHelpAndExit(config, DeviceDBServer.class);
		}

		final Injector confInjector = createInjector(
				new PropConfModule(config.config, CommonConfig.class, DeviceDBConfig.class, WisemlProviderConfig.class)
		);
		final CommonConfig commonConfig = confInjector.getInstance(CommonConfig.class);
		final DeviceDBConfig deviceDBConfig = confInjector.getInstance(DeviceDBConfig.class);
		final WisemlProviderConfig wisemlProviderConfig = confInjector.getInstance(WisemlProviderConfig.class);

		final DeviceDBServerModule module =
				new DeviceDBServerModule(commonConfig, deviceDBConfig, wisemlProviderConfig);
		final Injector deviceDBInjector = createInjector(module);
		final DeviceDBServer deviceDBServer = deviceDBInjector.getInstance(DeviceDBServer.class);

		Runtime.getRuntime().addShutdownHook(new Thread() {
												 @Override
												 public void run() {
													 if (deviceDBServer.isRunning()) {
														 deviceDBServer.stopAsync().awaitTerminated();
													 }
												 }
											 }
		);

		try {
			deviceDBServer.startAsync().awaitRunning();
		} catch (Exception e) {
			log.error("Could not start DeviceDB: {}", e);
			System.exit(1);
		}

		final String baseUri = "http://" + commonConfig.getHostname() + ":" + commonConfig
				.getPort();
		log.info(
				"DeviceDB started at {} (REST API: {})",
				baseUri + Constants.DEVICE_DB.DEVICEDB_WEBAPP_CONTEXT_PATH_VALUE,
				baseUri + Constants.DEVICE_DB.DEVICEDB_REST_API_CONTEXT_PATH_VALUE
		);
	}
}
