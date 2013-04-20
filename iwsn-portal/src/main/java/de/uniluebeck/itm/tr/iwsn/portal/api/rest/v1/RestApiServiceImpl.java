package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;

public class RestApiServiceImpl extends AbstractService implements RestApiService {

	private final ServicePublisher servicePublisher;

	private final RestApiApplication application;

	private ServicePublisherService service;

	@Inject
	public RestApiServiceImpl(final ServicePublisher servicePublisher, final RestApiApplication application) {
		this.servicePublisher = servicePublisher;
		this.application = application;
	}

	@Override
	protected void doStart() {
		try {
			service = servicePublisher.createJaxRsService("/rest/v1.0", application);
			service.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			if (service != null) {
				service.stopAndWait();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
