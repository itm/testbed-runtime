package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.EventBusFactory;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

public class FederatorPortalEventBus extends AbstractService implements PortalEventBus {

	private static final Logger log = LoggerFactory.getLogger(FederatorPortalEventBus.class);

	private final EventBus eventBus;

	@Inject
	public FederatorPortalEventBus(final EventBusFactory eventBusFactory) {
		this.eventBus = eventBusFactory.create("FederatorPortalEventBus");
	}

	@Override
	protected void doStart() {
		log.trace("FederatorPortalEventBus.doStart()");
		try {
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("FederatorPortalEventBus.doStop()");
		try {
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public void register(final Object object) {
		log.trace("FederatorPortalEventBus.register({})", object);
		eventBus.register(object);
	}

	@Override
	public void unregister(final Object object) {
		log.trace("FederatorPortalEventBus.unregister({})", object);
		eventBus.unregister(object);
	}

	@Override
	public void post(final Object event) {
		log.trace("FederatorPortalEventBus.post({})", event);
		checkState(isRunning(), "FederatorPortalEventBus is not running");
		eventBus.post(event);
	}
}
