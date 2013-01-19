package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.ServicePublisherJettyMetroJerseyModule;
import de.uniluebeck.itm.tr.iwsn.gateway.rest.RestApplication;
import de.uniluebeck.itm.tr.util.Logging;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.tr.iwsn.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.iwsn.common.config.ConfigHelper.setLogLevel;

public class Gateway extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(Gateway.class);

	static {
		Logging.setLoggingDefaults();
		org.apache.log4j.Logger.getLogger("org.eclipse.jetty").setLevel(Level.WARN);
		org.apache.log4j.Logger.getLogger("de.uniluebeck.itm.wsn.drivers").setLevel(Level.WARN);
		org.apache.log4j.Logger.getLogger("de.uniluebeck.itm.wsn.deviceutils").setLevel(Level.WARN);
	}

	private final GatewayConfig gatewayConfig;

	private final GatewayEventBus gatewayEventBus;

	private final DeviceManager deviceManager;

	private final DeviceObserverWrapper deviceObserverWrapper;

	private final RestApplication restApplication;

	private ServicePublisher servicePublisher;

	@Inject
	public Gateway(final GatewayConfig gatewayConfig,
				   final GatewayEventBus gatewayEventBus,
				   final DeviceManager deviceManager,
				   final DeviceObserverWrapper deviceObserverWrapper,
				   final RestApplication restApplication) {
		this.gatewayConfig = gatewayConfig;
		this.gatewayEventBus = gatewayEventBus;
		this.deviceManager = deviceManager;
		this.deviceObserverWrapper = deviceObserverWrapper;
		this.restApplication = restApplication;
	}

	@Override
	protected void doStart() {

		log.trace("Gateway.doStart()");

		try {
			gatewayEventBus.startAndWait();
			deviceManager.startAndWait();
			deviceObserverWrapper.startAndWait();

			if (gatewayConfig.restAPI) {

				final ServicePublisherConfig config = new ServicePublisherConfig(
						gatewayConfig.restAPIPort,
						ServicePublisherJettyMetroJerseyModule.class,
						this.getClass().getResource("/").toString()
				);

				servicePublisher = ServicePublisherFactory.create(config);
				servicePublisher.createJaxRsService("/devices/*", restApplication);
				servicePublisher.startAndWait();
			}

			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		log.trace("Gateway.doStop()");

		try {

			if (gatewayConfig.restAPI && servicePublisher != null) {
				servicePublisher.stopAndWait();
			}

			deviceObserverWrapper.stopAndWait();
			deviceManager.stopAndWait();
			gatewayEventBus.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public static void main(String[] args) {

		Thread.currentThread().setName("Gateway-Main");

		final GatewayConfig config = setLogLevel(parseOrExit(new GatewayConfig(), Gateway.class, args));
		final GatewayModule gatewayModule = new GatewayModule(config);
		final Injector injector = Guice.createInjector(gatewayModule);
		final Gateway gateway = injector.getInstance(Gateway.class);

		gateway.startAndWait();

		log.info("Gateway started!");

		Runtime.getRuntime().addShutdownHook(new Thread("Gateway-Shutdown") {
			@Override
			public void run() {
				log.info("Received KILL signal. Shutting down iWSN Gateway...");
				gateway.stopAndWait();
				log.info("Over and out.");
			}
		}
		);
	}
}
