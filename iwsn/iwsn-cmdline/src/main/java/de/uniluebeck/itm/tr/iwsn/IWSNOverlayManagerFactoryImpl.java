package de.uniluebeck.itm.tr.iwsn;

import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;

class IWSNOverlayManagerFactoryImpl implements IWSNOverlayManagerFactory {

	@Inject
	private Provider<TestbedRuntime> testbedRuntimeProvider;

	@Override
	public IWSNOverlayManager create(final DOMObserver domObserver) {
		return new IWSNOverlayManager(testbedRuntimeProvider.get(), domObserver);
	}
}
