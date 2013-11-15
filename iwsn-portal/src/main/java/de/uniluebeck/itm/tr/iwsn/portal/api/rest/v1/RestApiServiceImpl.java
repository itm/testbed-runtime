package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws.WebSocketServlet;

public class RestApiServiceImpl extends AbstractService implements RestApiService {

	private final ServicePublisher servicePublisher;

	private final RestApiApplication application;

	private final WebSocketServlet webSocketServlet;

	private ServicePublisherService restApi;

	private ServicePublisherService wsnWebSocketService;

	@Inject
	public RestApiServiceImpl(final ServicePublisher servicePublisher,
							  final RestApiApplication application,
							  final WebSocketServlet webSocketServlet) {
		this.servicePublisher = servicePublisher;
		this.application = application;
		this.webSocketServlet = webSocketServlet;
	}

	@Override
	protected void doStart() {
		try {

			restApi = servicePublisher.createJaxRsService("/rest/v1.0", application);
			restApi.startAndWait();

			wsnWebSocketService = servicePublisher.createWebSocketService("/ws/v1.0", webSocketServlet);
			wsnWebSocketService.startAndWait();

			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			if (wsnWebSocketService != null) {
				wsnWebSocketService.stopAndWait();
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
