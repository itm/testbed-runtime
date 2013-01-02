package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.tr.iwsn.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.iwsn.common.config.ConfigHelper.setLogLevel;

public class Gateway extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(Gateway.class);

	static {
		Logging.setLoggingDefaults();
	}

	private final GatewayEventBus gatewayEventBus;

	@Inject
	public Gateway(final GatewayEventBus gatewayEventBus) {
		this.gatewayEventBus = gatewayEventBus;
	}

	@Override
	protected void doStart() {
		try {
			gatewayEventBus.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
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