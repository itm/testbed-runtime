package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.EndpointManager;
import eu.wisebed.api.v3.sm.SessionManagement;

import static com.google.common.base.Preconditions.checkNotNull;

class SoapApiServiceImpl extends AbstractService implements SoapApiService {

	private final ServicePublisher servicePublisher;

	private final EndpointManager endpointManager;

	private final SessionManagement sessionManagement;

	private Service sessionManagementService;

	@Inject
	SoapApiServiceImpl(final ServicePublisher servicePublisher,
					   final EndpointManager endpointManager,
					   final SessionManagement sessionManagement) {
		this.servicePublisher = checkNotNull(servicePublisher);
		this.endpointManager = checkNotNull(endpointManager);
		this.sessionManagement = checkNotNull(sessionManagement);
	}

	@Override
	protected void doStart() {
		try {
			sessionManagementService = servicePublisher.createJaxWsService(endpointManager.getSmEndpointUri().getPath(), sessionManagement);
			sessionManagementService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			if (sessionManagementService != null && sessionManagementService.isRunning()) {
				sessionManagementService.stopAndWait();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
