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

	public String getWiseGuiContextPath() {
		return wiseGuiContextPath;
	}

	public File getWiseGuiSourceDir() {
		return wiseGuiSourceDir;
	}
}
