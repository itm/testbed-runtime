package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;

import static com.google.common.base.Preconditions.checkNotNull;

public class IWSNFederatorServiceImpl extends AbstractService implements IWSNFederatorService {

	private final SessionManagementFederatorService sessionManagementFederatorService;

	private final PortalEventBus portalEventBus;

	private final FederatedReservationManager federatedReservationManager;

	@Inject
	public IWSNFederatorServiceImpl(final PortalEventBus portalEventBus,
									final SessionManagementFederatorService sessionManagementFederatorService,
									final FederatedReservationManager federatedReservationManager) {
		this.portalEventBus = portalEventBus;
		this.federatedReservationManager = federatedReservationManager;
		this.sessionManagementFederatorService = checkNotNull(sessionManagementFederatorService);
	}

	@Override
	protected void doStart() {
		try {
			portalEventBus.startAndWait();
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

			if (portalEventBus.isRunning()) {
				portalEventBus.stopAndWait();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

}
