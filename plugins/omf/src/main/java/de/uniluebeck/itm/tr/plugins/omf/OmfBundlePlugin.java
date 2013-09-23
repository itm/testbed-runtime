package de.uniluebeck.itm.tr.plugins.omf;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OmfBundlePlugin extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(OmfBundlePlugin.class);

	private final PortalEventBus portalEventBus;

	public OmfBundlePlugin(final PortalEventBus portalEventBus) {
		this.portalEventBus = portalEventBus;
	}

	@Override
	protected void doStart() {
		try {
			portalEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			portalEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onReservationStartedEvent(final ReservationStartedEvent event) {
		log.debug("{}", event);
	}

	@Subscribe
	public void onReservationEndedEvent(final ReservationEndedEvent event) {
		log.debug("{}", event);
	}

	@Subscribe
	public void onRequest(final Request request) {
		log.debug("{}", request);
	}
}
