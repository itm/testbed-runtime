package de.uniluebeck.itm.tr.iwsn;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

class IWSNImpl implements IWSN {

	private static final Logger log = LoggerFactory.getLogger(IWSN.class);

	private final TestbedRuntime testbedRuntime;

	private final IWSNApplicationManager applicationManager;

	private final IWSNOverlayManager overlayManager;

	private final DOMObserver domObserver;

	private ScheduledExecutorService domObserverScheduler;

	private ScheduledFuture<?> domObserverSchedule;

	public IWSNImpl(final TestbedRuntime testbedRuntime,
					final IWSNApplicationManager applicationManager,
					final IWSNOverlayManager overlayManager,
					final DOMObserver domObserver) {

		this.testbedRuntime = testbedRuntime;
		this.applicationManager = applicationManager;
		this.overlayManager = overlayManager;
		this.domObserver = domObserver;
	}

	@Override
	public void start() throws Exception {

		log.info("Starting IWSN...");

		log.debug("Starting overlay services...");
		testbedRuntime.start();

		log.debug("Starting overlay manager...");
		overlayManager.start();

		log.debug("Starting application manager...");
		applicationManager.start();

		log.debug("Starting DOM observer...");
		final ThreadFactory domObserverThreadFactory = new ThreadFactoryBuilder()
				.setNameFormat("DOMObserver-Thread %d")
				.build();
		domObserverScheduler = Executors.newScheduledThreadPool(1, domObserverThreadFactory);
		domObserverSchedule = domObserverScheduler.scheduleWithFixedDelay(domObserver, 0, 3, TimeUnit.SECONDS);
	}

	@Override
	public void stop() {

		log.info("Stopping IWSN...");

		log.debug("Stopping DOM observer...");
		try {
			domObserverSchedule.cancel(true);
			ExecutorUtils.shutdown(domObserverScheduler, 1, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("Exception while stopping DOM observer: {}", e);
		}

		log.debug("Stopping application manager...");
		try {
			applicationManager.stop();
		} catch (Exception e) {
			log.error("Exception while stopping application manager: {}", e);
		}

		log.debug("Stopping overlay manager...");
		try {
			overlayManager.stop();
		} catch (Exception e) {
			log.error("Exception while stopping overlay manager: {}", e);
		}

		log.debug("Stopping overlay...");
		try {
			testbedRuntime.stop();
		} catch (Exception e) {
			log.error("Exception while stopping overlay: {}", e);
		}

		log.info("Stopped IWSN. Bye!");
	}
}