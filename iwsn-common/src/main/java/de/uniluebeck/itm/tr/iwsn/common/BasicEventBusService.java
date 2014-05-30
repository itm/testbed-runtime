package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.tr.common.EventBusService;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class BasicEventBusService extends AbstractService implements EventBusService {

	private final EventBus eventBus;

	public BasicEventBusService() {
		this.eventBus = new EventBus();
	}

	public BasicEventBusService(final EventBus eventBus) {
		this.eventBus = checkNotNull(eventBus);
	}

	@Override
	public void post(final Object event) {
		checkState(isRunning());
		eventBus.post(event);
	}

	@Override
	public void register(final Object object) {
		checkState(isRunning());
		eventBus.register(object);
	}

	@Override
	public void unregister(final Object object) {
		checkState(isRunning());
		eventBus.unregister(object);
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
}
