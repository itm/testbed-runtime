package de.uniluebeck.itm.tr.federator;

import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;

public class FederatorPortalEventBus extends AbstractService implements PortalEventBus {

	@Override
	protected void doStart() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	protected void doStop() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public void register(final Object object) {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public void unregister(final Object object) {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public void post(final Object event) {
		throw new RuntimeException("Implement me!");
	}
}
