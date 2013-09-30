package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.portal.EventBusFactory;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;

import static com.google.common.base.Preconditions.checkState;

public class FederatedReservationEventBus extends AbstractService implements ReservationEventBus {

	private final EventBus eventBus;

	private final WSNFederatorService wsnFederatorService;

	private final WSNFederatorController wsnFederatorController;

	@Inject
	public FederatedReservationEventBus(final EventBusFactory eventBusFactory,
										@Assisted final WSNFederatorService wsnFederatorService,
										@Assisted final WSNFederatorController wsnFederatorController) {
		this.wsnFederatorService = wsnFederatorService;
		this.wsnFederatorController = wsnFederatorController;
		this.eventBus = eventBusFactory.create("FederatedReservationEventBus");
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
	public void enableVirtualization() {
		checkRunning();
		// TODO implement
	}

	@Override
	public void disableVirtualization() {
		checkRunning();
		// TODO implement
	}

	@Override
	public boolean isVirtualizationEnabled() {
		checkRunning();
		return false;  // TODO implement
	}

	@Override
	public void register(final Object object) {
		checkRunning();
		// TODO implement
	}

	private void checkRunning() {
		checkState(isRunning(), "FederatedReservationEventBus is not running");
	}

	@Override
	public void unregister(final Object object) {
		checkRunning();
		// TODO implement
	}

	@Override
	public void post(final Object event) {
		checkRunning();
	}
}
