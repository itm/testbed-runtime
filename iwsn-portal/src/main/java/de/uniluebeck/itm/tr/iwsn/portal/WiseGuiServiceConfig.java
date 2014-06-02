package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;
import org.nnsoft.guice.rocoto.converters.FileConverter;

import java.io.File;

public class WiseGuiServiceConfig {

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

	public File getWiseGuiSourceDir() {
		return wiseGuiSourceDir;
	}

	public String getWiseguiTestbedName() {
		return wiseguiTestbedName;
	}
}
