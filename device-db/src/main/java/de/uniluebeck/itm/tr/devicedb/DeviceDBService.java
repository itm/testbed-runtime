package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.tr.util.Logging;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.tr.iwsn.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.iwsn.common.config.ConfigHelper.setLogLevel;

public class DeviceDBService extends AbstractService {

	static {
		Logging.setLoggingDefaults();
		org.apache.log4j.Logger.getLogger("org.eclipse.jetty").setLevel(Level.WARN);
		//org.apache.log4j.Logger.getLogger("org.hibernate").setLevel(Level.WARN);
	}

	private static final Logger log = LoggerFactory.getLogger(DeviceDBService.class);

	private final ServicePublisherConfig servicePublisherConfig;

	private final ServicePublisherFactory servicePublisherFactory;

	private final DeviceDBRestApplication restApplication;

	private ServicePublisher servicePublisher;

	@Inject
	public DeviceDBService(final ServicePublisherConfig servicePublisherConfig,
						   final ServicePublisherFactory servicePublisherFactory,
						   final DeviceDBRestApplication restApplication) {
		this.servicePublisherConfig = servicePublisherConfig;
		this.servicePublisherFactory = servicePublisherFactory;
		this.restApplication = restApplication;
	}

	@Override
	protected void doStart() {

		log.trace("DeviceDBService.doStart()");

		try {

			servicePublisher = servicePublisherFactory.create(servicePublisherConfig);
			servicePublisher.createJaxRsService("/rest/*", restApplication);
			servicePublisher.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	@Override
	protected void doStop() {

		log.trace("DeviceDBService.doStop()");

		try {

			if (servicePublisher != null && servicePublisher.isRunning()) {
				servicePublisher.stopAndWait();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public static void main(String[] args) {

		final DeviceDBServiceConfig config = setLogLevel(parseOrExit(
				new DeviceDBServiceConfig(),
				DeviceDBService.class,
				args
		)
		);

		final DeviceDBService deviceDBService = Guice
				.createInjector(new DeviceDBServiceModule(config))
				.getInstance(DeviceDBService.class);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (deviceDBService.isRunning()) {
					deviceDBService.stopAndWait();
				}
			}
		});

		deviceDBService.startAndWait();
	}
}
