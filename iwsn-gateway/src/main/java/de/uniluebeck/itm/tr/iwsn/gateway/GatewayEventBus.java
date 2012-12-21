package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.util.concurrent.Service;

public interface GatewayEventBus extends Service {

	void register(Object object);

	void unregister(Object object);

	void post(Object event);
}
