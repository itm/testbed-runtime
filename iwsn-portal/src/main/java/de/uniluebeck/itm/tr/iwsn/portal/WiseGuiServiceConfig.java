package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;
import de.uniluebeck.itm.util.propconf.converters.URITypeConverter;
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
			usage = "The base URI of the testbed REST API",
			example = "http://my.testbed.tld/rest/v1.0",
			typeConverter = URITypeConverter.class
	)
	public static final String WISEGUI_REST_API_BASE_URI = "wisegui.rest_api_base_uri";

	@Inject
	@Named(WISEGUI_REST_API_BASE_URI)
	private String wiseGuiRestApiBaseUri;

	@PropConf(
			usage = "The URI of the testbed WebSocket API",
			example = "http://my.testbed.tld/ws/v1.0"
	)
	public static final String WISEGUI_WEBSOCKET_URI = "wisegui.websocket_uri";

	@Inject
	@Named(WISEGUI_WEBSOCKET_URI)
	private String wiseGuiWebSocketUri;

	public String getWiseGuiContextPath() {
		return wiseGuiContextPath;
	}

	public File getWiseGuiSourceDir() {
		return wiseGuiSourceDir;
	}

	public String getWiseguiTestbedName() {
		return wiseguiTestbedName;
	}

	public String getWiseGuiRestApiBaseUri() {
		return wiseGuiRestApiBaseUri;
	}

	public String getWiseGuiWebSocketUri() {
		return wiseGuiWebSocketUri;
	}
}
