package de.uniluebeck.itm.tr.iwsn;

import com.google.inject.Inject;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IWSNShutdownRunnable implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(IWSNShutdownRunnable.class);

	@Inject
	private TestbedRuntime testbedRuntime;

	@Inject
	private IWSNOverlayManager overlayManager;

	@Inject
	private IWSNApplicationManager applicationManager;

	@Override
	public void run() {

		log.info("Received shutdown signal!");

		log.info("Stopping applications...");
		applicationManager.stop();
		log.info("Stopped applications!");

		log.info("Stopping overlay...");
		overlayManager.stop();
		testbedRuntime.stop();
		log.info("Stopped overlay!");
	}

}
