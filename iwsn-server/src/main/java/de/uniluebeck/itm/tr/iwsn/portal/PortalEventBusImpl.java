package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.Event;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeProgress;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;

import static com.google.common.base.Preconditions.checkState;

class PortalEventBusImpl extends AbstractService implements PortalEventBus {

	private final PortalConfig config;

	private final EventBus eventBus;

	@Inject
	public PortalEventBusImpl(final PortalConfig config, final EventBus eventBus) {
		this.config = config;
		this.eventBus = eventBus;
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
	public void post(final Event event) {
		assertConnectedToPortal();
		throw new RuntimeException("IMPLEMENT ME!");
	}

	@Override
	public void post(final SingleNodeProgress progress) {
		assertConnectedToPortal();
		throw new RuntimeException("IMPLEMENT ME!");
	}

	@Override
	public void post(final SingleNodeResponse response) {
		assertConnectedToPortal();
		throw new RuntimeException("IMPLEMENT ME!");
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

	private void assertConnectedToPortal() {
		checkState(isRunning(), "Not connected to portal server!");
	}
}
