package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.Service;

public interface PortalEventBus extends Service {

	void register(Object object);

	void unregister(Object object);

	void post(Object event);
}
