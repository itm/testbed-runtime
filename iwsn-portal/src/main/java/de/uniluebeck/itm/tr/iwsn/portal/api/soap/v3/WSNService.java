package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.wsn.WSN;

import java.net.URI;

public interface WSNService extends WSN, Service {
	URI getURI();
}
