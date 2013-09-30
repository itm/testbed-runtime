package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
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

		if (event instanceof Request) {

			final Request request = (Request) event;
			final String reservationId = request.getReservationId();
			final long requestId = request.getRequestId();

			switch (request.getType()) {
				case ARE_NODES_ALIVE:
					break;
				case ARE_NODES_CONNECTED:
					break;
				case DISABLE_NODES:
					break;
				case DISABLE_VIRTUAL_LINKS:
					break;
				case DISABLE_PHYSICAL_LINKS:
					break;
				case ENABLE_NODES:
					break;
				case ENABLE_PHYSICAL_LINKS:
					break;
				case ENABLE_VIRTUAL_LINKS:
					break;
				case FLASH_IMAGES:
					break;
				case GET_CHANNEL_PIPELINES:
					break;
				case RESET_NODES:
					break;
				case SEND_DOWNSTREAM_MESSAGES:
					break;
				case SET_CHANNEL_PIPELINES:
					break;
				default:
					throw new RuntimeException("Unknown request type: " + request.getType());
			}
		}
		throw new RuntimeException("There shouldn't be other events than requests");
	}
}
