package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.wsn.WSN;

import java.net.URI;

public interface WSNFederatorService extends Service, WSN {

	WSNFederatorController getWsnFederatorController();

	URI getEndpointUri();

}
