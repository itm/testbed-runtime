package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws.WsnWebSocketServlet;

public class RestApiServiceImpl extends AbstractService implements RestApiService {

	private final ServicePublisher servicePublisher;

	private final RestApiApplication application;

	private final WsnWebSocketServlet wsnWebSocketServlet;

	private ServicePublisherService restApi;

	private ServicePublisherService webSocketService;

	@Inject
	public RestApiServiceImpl(final ServicePublisher servicePublisher, final RestApiApplication application,
							  final WsnWebSocketServlet wsnWebSocketServlet) {
		this.servicePublisher = servicePublisher;
		this.application = application;
		this.wsnWebSocketServlet = wsnWebSocketServlet;
	}

	@Override
	protected void doStart() {
		try {

			restApi = servicePublisher.createJaxRsService("/rest/v1.0", application);
			restApi.startAndWait();

			webSocketService = servicePublisher.createWebSocketService("/ws/v1.0", wsnWebSocketServlet);
			webSocketService.startAndWait();

			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			if (webSocketService != null) {
				webSocketService.stopAndWait();
			}

			if (restApi != null) {
				restApi.stopAndWait();
			}

			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
