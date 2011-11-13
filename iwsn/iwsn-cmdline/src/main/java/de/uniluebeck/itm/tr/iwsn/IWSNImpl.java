package de.uniluebeck.itm.tr.iwsn;

import de.uniluebeck.itm.gtr.TestbedRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IWSNImpl implements IWSN {

	private static final Logger log = LoggerFactory.getLogger(IWSN.class);

	private final TestbedRuntime testbedRuntime;

	private final IWSNApplicationManager applicationManager;

	private final IWSNOverlayManager overlayManager;

	public IWSNImpl(final TestbedRuntime testbedRuntime,
					final IWSNApplicationManager applicationManager,
					final IWSNOverlayManager overlayManager) {

		this.testbedRuntime = testbedRuntime;
		this.applicationManager = applicationManager;
		this.overlayManager = overlayManager;
	}

	@Override
	public void start() throws Exception {

		log.info("Starting iWSN...");

		log.debug("Starting overlay services...");
		testbedRuntime.start();

		log.debug("Starting overlay manager...");
		overlayManager.start();

		log.debug("Starting application manager...");
		applicationManager.start();
	}

	@Override
	public void stop() {

		log.info("Stopping iWSN...");

		log.debug("Stopping application manager...");
		applicationManager.stop();

		log.debug("Stopping overlay manager...");
		overlayManager.stop();

		log.debug("Stopping overlay...");
		testbedRuntime.stop();

		log.info("Stopped iWSN. Bye!");
	}
}