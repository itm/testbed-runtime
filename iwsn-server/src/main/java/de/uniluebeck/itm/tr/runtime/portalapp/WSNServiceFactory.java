package de.uniluebeck.itm.tr.runtime.portalapp;

import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;

public interface WSNServiceFactory {
	
	WSNService create(final WSNServiceConfig config, final DeliveryManager deliveryManager,
						  final WSNPreconditions preconditions, final WSNApp wsnApp);
}
