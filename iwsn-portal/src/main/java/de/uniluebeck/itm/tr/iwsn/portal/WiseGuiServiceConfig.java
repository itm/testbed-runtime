package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;
import org.nnsoft.guice.rocoto.converters.FileConverter;

import java.io.File;

public class WiseGuiServiceConfig {

	@PropConf(
			usage = "The context path under which the WiseGui shall be run",
			example = "/",
			defaultValue = "/"
	)
	public static final String WISEGUI_CONTEXT_PATH = "wisegui.context_path";

	@Inject
	@Named(WISEGUI_CONTEXT_PATH)
	private String wiseGuiContextPath;

	@PropConf(
			usage = "The directory under which to find the WiseGui. If not given WiseGui will be served from a packaged "
					+ "internal version.",
			typeConverter = FileConverter.class
	)
	public static final String WISEGUI_SOURCE_DIR = "wisegui.source_dir";

	@Inject
	@Named(WISEGUI_SOURCE_DIR)
	private File wiseGuiSourceDir;

	@PropConf(
			usage = "Name of the testbed to be displayed in WiseGui frontend",
			defaultValue = "local testbed"
	)
	public static final String WISEGUI_TESTBED_NAME = "wisegui.testbed_name";

	@Inject
	@Named(WISEGUI_TESTBED_NAME)
	private String wiseguiTestbedName;

	@PropConf(
			usage = "JSON Description of the federated testbeds to be displayed in WiseGui",
			example = "{\"urn:wisebed:uzl:staging1:\":\"Testbed 1\", \"urn:wisebed:uzl:staging2:\":\"Testbed 2\"}"
	)
	public static final String WISEGUI_FEDERATES = "wisegui.federates";

	@Inject
	@Named(WISEGUI_FEDERATES)
	private String wiseguiFederates;

	@PropConf(
			usage = "The context path under which the REST API runs",
			example = "/rest/v1.0",
			defaultValue = "/rest/v1.0"
	)
	public static final String REST_API_CONTEXT_PATH = "rest_api.context_path";

	@Inject
	@Named(REST_API_CONTEXT_PATH)
	private String restApiContextPath;

	@PropConf(
			usage = "The context path under which the WebSocket API runs",
			example = "/ws/v1.0",
			defaultValue = "/ws/v1.0"
	)
	public static final String WEBSOCKET_CONTEXT_PATH = "websocket.context_path";

	@Inject
	@Named(WEBSOCKET_CONTEXT_PATH)
	private String websocketContextPath;

	public String getWiseGuiContextPath() {
		return wiseGuiContextPath;
	}

	public File getWiseGuiSourceDir() {
		return wiseGuiSourceDir;
	}

	public String getWiseguiTestbedName() {
		return wiseguiTestbedName;
	}

	public String getWiseguiFederates() {
		return wiseguiFederates;
	}

	public String getRestApiContextPath() {
		return restApiContextPath;
	}

	public String getWebsocketContextPath() {
		return websocketContextPath;
	}
}
