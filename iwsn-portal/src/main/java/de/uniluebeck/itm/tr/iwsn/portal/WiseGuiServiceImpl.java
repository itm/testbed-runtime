package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;

import java.io.File;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class WiseGuiServiceImpl extends AbstractService implements WiseGuiService {

	private final ServicePublisher servicePublisher;

	private final WiseGuiServiceConfig wiseGuiServiceConfig;

	private final PortalServerConfig portalServerConfig;

	private ServicePublisherService webapp;

	@Inject
	public WiseGuiServiceImpl(final ServicePublisher servicePublisher,
							  final WiseGuiServiceConfig wiseGuiServiceConfig,
							  final PortalServerConfig portalServerConfig) {
		this.servicePublisher = servicePublisher;
		this.wiseGuiServiceConfig = wiseGuiServiceConfig;
		this.portalServerConfig = portalServerConfig;
	}

	@Override
	protected void doStart() {
		try {

			final String resourceBase;
			final File wiseGuiSourceDir = wiseGuiServiceConfig.getWiseGuiSourceDir();
			if (wiseGuiSourceDir != null && !"".equals(wiseGuiSourceDir.toString())) {

				if (!wiseGuiSourceDir.exists()) {
					throw new IllegalArgumentException(
							"WiseGui source directory \"" + wiseGuiSourceDir + "\" does not exist"
					);
				}

				if (!wiseGuiSourceDir.isDirectory()) {
					throw new IllegalArgumentException(
							"WiseGui source directory \"" + wiseGuiSourceDir + "\" is not a directory"
					);
				}

				if (!wiseGuiSourceDir.canRead()) {
					throw new IllegalArgumentException(
							"WiseGui source directory \"" + wiseGuiSourceDir + "\" can't be read"
					);
				}

				resourceBase = wiseGuiSourceDir.toString();

			} else {

				resourceBase = this.getClass().getResource("/de/uniluebeck/itm/tr/iwsn/portal/wisegui").toString();
			}

			final Map<String, String> params = newHashMap();
			params.put(WiseGuiServiceConfig.WISEGUI_CONTEXT_PATH, wiseGuiServiceConfig.getWiseGuiContextPath());
			params.put(WiseGuiServiceConfig.WISEGUI_TESTBED_NAME, wiseGuiServiceConfig.getWiseguiTestbedName());
			params.put(WiseGuiServiceConfig.WISEGUI_REST_API_BASE_URI, wiseGuiServiceConfig.getWiseGuiRestApiBaseUri());
			params.put(WiseGuiServiceConfig.WISEGUI_WEBSOCKET_URI, wiseGuiServiceConfig.getWiseGuiWebSocketUri());

			webapp = servicePublisher.createServletService(
					wiseGuiServiceConfig.getWiseGuiContextPath(),
					resourceBase,
					params
			);

			webapp.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			if (webapp != null && webapp.isRunning()) {
				webapp.stopAndWait();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
