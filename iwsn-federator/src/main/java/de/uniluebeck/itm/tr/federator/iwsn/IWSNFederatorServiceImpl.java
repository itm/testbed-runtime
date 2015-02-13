package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.util.scheduler.SchedulerService;

import static com.google.common.base.Preconditions.checkNotNull;

public class IWSNFederatorServiceImpl extends AbstractService implements IWSNFederatorService {

	private final SessionManagementFederatorService sessionManagementFederatorService;

	private final SchedulerService schedulerService;

	private final PortalEventBus portalEventBus;

	private final FederatorPortalEventBusAdapter federatorPortalEventBusAdapter;

	private final FederatedReservationManager federatedReservationManager;

	@Inject
	public IWSNFederatorServiceImpl(final SchedulerService schedulerService,
									final PortalEventBus portalEventBus,
									final FederatorPortalEventBusAdapter federatorPortalEventBusAdapter,
									final SessionManagementFederatorService sessionManagementFederatorService,
									final FederatedReservationManager federatedReservationManager) {
		this.schedulerService = schedulerService;
		this.portalEventBus = portalEventBus;
		this.federatorPortalEventBusAdapter = federatorPortalEventBusAdapter;
		this.federatedReservationManager = federatedReservationManager;
		this.sessionManagementFederatorService = checkNotNull(sessionManagementFederatorService);
	}

	@Override
	protected void doStart() {
		try {
            schedulerService.startAsync().awaitRunning();
            portalEventBus.startAsync().awaitRunning();
            federatorPortalEventBusAdapter.startAsync().awaitRunning();
            federatedReservationManager.startAsync().awaitRunning();
            sessionManagementFederatorService.startAsync().awaitRunning();
            notifyStarted();
        } catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			if (sessionManagementFederatorService.isRunning()) {
				sessionManagementFederatorService.stopAsync().awaitTerminated();
			}

			if (federatedReservationManager.isRunning()) {
				federatedReservationManager.stopAsync().awaitTerminated();
			}

			if (federatorPortalEventBusAdapter.isRunning()) {
				federatorPortalEventBusAdapter.stopAsync().awaitTerminated();
			}

			if (portalEventBus.isRunning()) {
				portalEventBus.stopAsync().awaitTerminated();
			}

			if (schedulerService.isRunning()) {
				schedulerService.stopAsync().awaitTerminated();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

}
