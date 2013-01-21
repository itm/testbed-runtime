package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.util.concurrent.Service;

public interface EventBusService extends Service {

	void register(Object object);

	void unregister(Object object);

	void post(Object event);

}
