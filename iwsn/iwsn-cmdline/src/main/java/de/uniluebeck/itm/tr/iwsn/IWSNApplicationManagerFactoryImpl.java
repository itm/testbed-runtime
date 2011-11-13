package de.uniluebeck.itm.tr.iwsn;

import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;

public class IWSNApplicationManagerFactoryImpl implements IWSNApplicationManagerFactory {

	@Override
	public IWSNApplicationManager create(final TestbedRuntime testbedRuntime,
										 final DOMObserver domObserver,
										 final String configurationNodeId) {

		return new IWSNApplicationManager(testbedRuntime, domObserver, configurationNodeId);
	}
}
