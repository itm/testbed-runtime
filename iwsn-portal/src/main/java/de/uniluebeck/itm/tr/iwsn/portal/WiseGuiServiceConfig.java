package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;

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

	public String getWiseGuiContextPath() {
		return wiseGuiContextPath;
	}
}
