package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import de.uniluebeck.itm.gtr.common.Service;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import eu.wisebed.testbed.api.wsn.v211.WSN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;


@Singleton
public class WSNServiceHandle implements Service {

	private static final Logger log = LoggerFactory.getLogger(WSNServiceHandle.class);

	private WSNService wsnService;

	private WSNApp wsnApp;

	private URL wsnInstanceEndpointUrl;

	@Inject
	WSNServiceHandle(@Named(WSNServiceModule.WSN_SERVICE_ENDPOINT_URL) URL wsnInstanceEndpointUrl,
					 WSNService wsnService,
					 WSNApp wsnApp) {

		this.wsnService = wsnService;
		this.wsnApp = wsnApp;
		this.wsnInstanceEndpointUrl = wsnInstanceEndpointUrl;
	}

	@Override
	public void start() throws Exception {
		wsnApp.start();
		wsnService.start();
	}

	@Override
	public void stop() {
		try {
			wsnService.stop();
		} catch (Exception e) {
			log.warn("" + e, e);
		}
		try {
			wsnApp.stop();
		} catch (Exception e) {
			log.warn("" + e, e);
		}
	}

	public WSN getWSNService() {
		return wsnService;
	}

	public WSNApp getWsnApp() {
		return wsnApp;
	}

	public URL getWsnInstanceEndpointUrl() {
		return wsnInstanceEndpointUrl;
	}
	
}
