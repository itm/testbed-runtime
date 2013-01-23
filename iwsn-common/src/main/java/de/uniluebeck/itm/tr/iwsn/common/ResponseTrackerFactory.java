package de.uniluebeck.itm.tr.iwsn.common;

import de.uniluebeck.itm.tr.iwsn.messages.Request;

public interface ResponseTrackerFactory {

	ResponseTracker create(Request request);

}
