package de.uniluebeck.itm.tr.iwsn;

import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;

class IWSNOverlayManagerFactoryImpl implements IWSNOverlayManagerFactory {

	@Override
	public IWSNOverlayManager create(final TestbedRuntime testbedRuntime, final DOMObserver domObserver,
									 final String nodeId) {
		return new IWSNOverlayManager(testbedRuntime, domObserver, nodeId);
	}
}
