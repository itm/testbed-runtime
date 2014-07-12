package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PortalEventStoreServiceImpl extends AbstractService implements PortalEventStoreService {

	private static final Logger log = LoggerFactory.getLogger(PortalEventStoreServiceImpl.class);

	private final PortalEventBus portalEventBus;

	@Inject
	public PortalEventStoreServiceImpl(final PortalEventBus portalEventBus) {
		this.portalEventBus = portalEventBus;
	}

	@Override
	protected void doStart() {
		log.trace("PortalEventStoreServiceImpl.doStart()");
		try {
			portalEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("PortalEventStoreServiceImpl.doStop()");
		try {
			portalEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
