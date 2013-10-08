package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerController;
import eu.wisebed.api.v3.controller.Controller;

import java.net.URI;

public interface FederatorController extends Service, Controller {

	URI getEndpointUrl();

	void addRequestIdMapping(long federatedRequestId, long federatorRequestId);

	void addController(DeliveryManagerController controller);

	void removeController(DeliveryManagerController controller);
}
