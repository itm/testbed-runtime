package de.uniluebeck.itm.tr.iwsn;

import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;

class IWSNOverlayManagerFactoryImpl implements IWSNOverlayManagerFactory {

	@Override
	public IWSNOverlayManager create(final TestbedRuntime testbedRuntime, final DOMObserver domObserver) {
		return new IWSNOverlayManager(testbedRuntime, domObserver);
	}
}
