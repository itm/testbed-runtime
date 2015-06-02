package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.common.util.concurrent.Service;

/**
 * A service that listens on the PortalEventBus and forwards all events to external plugins that are connected via a TCP
 * connection.
 */
public interface ExternalPluginService extends Service {

}