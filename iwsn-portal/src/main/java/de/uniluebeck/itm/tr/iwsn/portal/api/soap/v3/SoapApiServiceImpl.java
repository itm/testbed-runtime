package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import eu.wisebed.api.v3.sm.SessionManagement;

class SoapApiServiceImpl extends AbstractService implements SoapApiService {

	private final ServicePublisher servicePublisher;

	private final SessionManagement sessionManagement;

	private Service sessionManagementService;

	@Inject
	SoapApiServiceImpl(final ServicePublisher servicePublisher, final SessionManagement sessionManagement) {
		this.servicePublisher = servicePublisher;
		this.sessionManagement = sessionManagement;
	}

	@Override
	protected void doStart() {
		try {
			sessionManagementService = servicePublisher.createJaxWsService("/v3/sm", sessionManagement);
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
