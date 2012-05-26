package de.uniluebeck.itm.tr.iwsn;

import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;

public interface IWSNApplicationManagerFactory {

	IWSNApplicationManager create(final TestbedRuntime testbedRuntime,
								  final DOMObserver domObserver,
								  final String configurationNodeId);

}
