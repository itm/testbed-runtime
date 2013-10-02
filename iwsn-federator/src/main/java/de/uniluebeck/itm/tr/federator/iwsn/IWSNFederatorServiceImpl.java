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
			schedulerService.startAndWait();
			portalEventBus.startAndWait();
			federatorPortalEventBusAdapter.startAndWait();
			federatedReservationManager.startAndWait();
			sessionManagementFederatorService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			if (sessionManagementFederatorService.isRunning()) {
				sessionManagementFederatorService.stopAndWait();
			}

			if (federatedReservationManager.isRunning()) {
				federatedReservationManager.stopAndWait();
			}

			if (federatorPortalEventBusAdapter.isRunning()) {
				federatorPortalEventBusAdapter.stopAndWait();
			}

			if (portalEventBus.isRunning()) {
				portalEventBus.stopAndWait();
			}

			if (schedulerService.isRunning()) {
				schedulerService.stopAndWait();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

}
