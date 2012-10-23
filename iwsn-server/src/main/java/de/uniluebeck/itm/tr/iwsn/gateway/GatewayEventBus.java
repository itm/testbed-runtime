package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.messages.Event;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeProgress;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;

public interface GatewayEventBus extends Service {

	void register(Object object);

	void unregister(Object object);

	void post(Event event);

	void post(SingleNodeProgress progress);

	void post(SingleNodeResponse response);

}
