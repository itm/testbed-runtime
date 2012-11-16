package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.tr.iwsn.config.ConfigHelper.parseConfigOrPrintHelpAndExit;

public class Portal {

	private static final Logger log = LoggerFactory.getLogger(Portal.class);

	static {
		Logging.setLoggingDefaults();
	}

	public static void main(String[] args) {

		PortalConfig portalConfig = parseConfigOrPrintHelpAndExit(new PortalConfig(), args);

		PortalModule portalModule = new PortalModule(portalConfig);
		Injector injector = Guice.createInjector(portalModule);

		PortalEventBus portalEventBus = injector.getInstance(PortalEventBus.class);
		portalEventBus.startAndWait();

		log.info("Portal started!");
	}

}
