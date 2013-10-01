package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.EventBusFactory;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;

import static com.google.common.base.Preconditions.checkState;

public class FederatorPortalEventBus extends AbstractService implements PortalEventBus {

	private final EventBus eventBus;

	@Inject
	public FederatorPortalEventBus(final EventBusFactory eventBusFactory) {
		this.eventBus = eventBusFactory.create("FederatorPortalEventBus");
	}

	@Override
	protected void doStart() {
		try {
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public void register(final Object object) {
		eventBus.register(object);
	}

	@Override
	public void unregister(final Object object) {
		eventBus.unregister(object);
	}

	@Override
	public void post(final Object event) {
		checkState(isRunning(), "FederatorPortalEventBus is not running");
		eventBus.post(event);
	}
}
