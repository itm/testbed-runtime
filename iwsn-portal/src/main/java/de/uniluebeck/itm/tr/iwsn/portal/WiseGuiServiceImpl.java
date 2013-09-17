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

			final Map<String, String> initParams = newHashMap();
			initParams.put(WiseGuiServiceConfig.WISEGUI_CONTEXT_PATH, wiseGuiServiceConfig.getWiseGuiContextPath());
			initParams.put(PortalServerConfig.WISEGUI_TESTBED_NAME, portalServerConfig.getWiseguiTestbedName());
			initParams.put(PortalServerConfig.REST_API_CONTEXT_PATH, portalServerConfig.getRestApiContextPath());
			initParams.put(PortalServerConfig.WEBSOCKET_CONTEXT_PATH, portalServerConfig.getWebsocketContextPath());

			webapp = servicePublisher.createServletService(
					wiseGuiServiceConfig.getWiseGuiContextPath(),
					resourceBase,
					initParams
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
