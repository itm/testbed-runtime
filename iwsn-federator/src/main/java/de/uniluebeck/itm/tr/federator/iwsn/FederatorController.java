package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.controller.Controller;

import java.net.URI;

public interface FederatorController extends Service, Controller {

	URI getEndpointUrl();

}
