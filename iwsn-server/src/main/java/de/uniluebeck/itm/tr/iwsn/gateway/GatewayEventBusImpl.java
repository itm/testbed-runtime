package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.tr.iwsn.messages.Event;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeProgress;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkState;

class GatewayEventBusImpl extends AbstractService implements GatewayEventBus {

	private final GatewayConfig config;

	private final EventBus eventBus;

	@Inject
	public GatewayEventBusImpl(final GatewayConfig config, final EventBus eventBus) {
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
		throw new RuntimeException("IMPLEMENT ME!");
	}

	@Override
	protected void doStop() {
		throw new RuntimeException("IMPLEMENT ME!");
	}

	private void assertConnectedToPortal() {
		checkState(isRunning(), "Not connected to portal server!");
	}
}
