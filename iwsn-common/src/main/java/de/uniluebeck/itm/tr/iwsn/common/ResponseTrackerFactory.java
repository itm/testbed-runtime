package de.uniluebeck.itm.tr.iwsn.common;

import de.uniluebeck.itm.tr.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.messages.RequestResponseHeader;

public interface ResponseTrackerFactory {

	ResponseTracker create(RequestResponseHeader requestHeader, EventBusService eventBus);

}
