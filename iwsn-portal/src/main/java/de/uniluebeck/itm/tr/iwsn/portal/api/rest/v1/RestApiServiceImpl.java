package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.devicedb.CachedDeviceDBService;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws.WebSocketServlet;
import org.jetbrains.annotations.NotNull;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

public class RestApiServiceImpl extends AbstractService implements RestApiService {

	private final ServicePublisher servicePublisher;

	private final RestApiApplication application;

	private final CachedDeviceDBService cachedDeviceDBService;

	private final WebSocketServlet webSocketServlet;

	private ServiceManager serviceManager;

	@Inject
	public RestApiServiceImpl(final ServicePublisher servicePublisher,
							  final RestApiApplication application,
							  final CachedDeviceDBService cachedDeviceDBService,
							  final WebSocketServlet webSocketServlet) {
		this.servicePublisher = servicePublisher;
		this.application = application;
		this.cachedDeviceDBService = cachedDeviceDBService;
		this.webSocketServlet = webSocketServlet;
	}

	@Override
	protected void doStart() {

		final ServicePublisherService restApi = servicePublisher.createJaxRsService("/rest/v1.0", application, null);
		final ServicePublisherService wsnWebSocketService =
				servicePublisher.createWebSocketService("/ws/v1.0", webSocketServlet);

		serviceManager = new ServiceManager(newHashSet(cachedDeviceDBService, restApi, wsnWebSocketService));
		final ServiceManager.Listener listener = new ServiceManager.Listener() {
			@Override
			public void healthy() {
				notifyStarted();
			}

			@Override
			public void stopped() {
				notifyStopped();
			}

			@Override
			public void failure(@NotNull final Service service) {
				notifyFailed(service.failureCause());
			}
		};
		serviceManager.addListener(listener, sameThreadExecutor());
		serviceManager.startAsync();
	}

	@Override
	protected void doStop() {
		serviceManager.stopAsync();
	}
}
