package de.uniluebeck.itm.tr.iwsn;

import com.google.inject.Inject;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;

public class IWSNApplicationManagerFactoryImpl implements IWSNApplicationManagerFactory {

	@Inject
	private TestbedRuntime testbedRuntime;

	@Override
	public IWSNApplicationManager create(final DOMObserver domObserver, final String configurationNodeId) {
		return new IWSNApplicationManager(testbedRuntime, domObserver, configurationNodeId);
	}
}
