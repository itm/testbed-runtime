package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.messages.Event;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeProgress;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;

public interface PortalEventBus extends Service {

	void register(Object object);

	void unregister(Object object);

	void post(Event event);

	void post(SingleNodeProgress progress);

	void post(SingleNodeResponse response);

}
