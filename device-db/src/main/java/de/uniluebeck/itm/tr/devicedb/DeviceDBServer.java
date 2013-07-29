package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.WisemlProviderConfig;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.config.ConfigWithLoggingAndProperties;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.propconf.PropConfModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.Guice.createInjector;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.setLogLevel;
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
			servicePublisher.startAndWait();
			deviceDBService.startAndWait();
			deviceDBRestService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			if (deviceDBRestService.isRunning()) {
				deviceDBRestService.stopAndWait();
			}

			if (deviceDBService.isRunning()) {
				deviceDBService.stopAndWait();
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

		Thread.currentThread().setName("DeviceDB-Main");

		final ConfigWithLoggingAndProperties config = setLogLevel(
				parseOrExit(new ConfigWithLoggingAndProperties(), DeviceDBServer.class, args),
				"de.uniluebeck.itm"
		);

		if (config.helpConfig) {
			printDocumentationAndExit(System.out, CommonConfig.class, DeviceDBConfig.class);
		}

		final Injector confInjector = createInjector(
				new PropConfModule(config.config, CommonConfig.class, DeviceDBConfig.class, WisemlProviderConfig.class)
		);
		final CommonConfig commonConfig = confInjector.getInstance(CommonConfig.class);
		final DeviceDBConfig deviceDBConfig = confInjector.getInstance(DeviceDBConfig.class);
		final WisemlProviderConfig wisemlProviderConfig = confInjector.getInstance(WisemlProviderConfig.class);

		final DeviceDBServerModule module = new DeviceDBServerModule(commonConfig, deviceDBConfig, wisemlProviderConfig);
		final DeviceDBServer deviceDBServer = createInjector(module).getInstance(DeviceDBServer.class);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (deviceDBServer.isRunning()) {
					deviceDBServer.stopAndWait();
				}
			}
		}
		);

		try {
			deviceDBServer.start().get();
		} catch (Exception e) {
			log.error("Could not start DeviceDB: {}", e.getMessage());
			System.exit(1);
		}

		log.info(
				"DeviceDB started at {} (REST API: {})",
				"http://localhost:" + commonConfig.getPort() + deviceDBConfig.getDeviceDBWebappContextPath(),
				"http://localhost:" + commonConfig.getPort() + deviceDBConfig.getDeviceDBRestApiContextPath()
		);
	}

}
