package de.uniluebeck.itm.tr.iwsn;

import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;

public interface IWSNOverlayManagerFactory {

	IWSNOverlayManager create(final TestbedRuntime testbedRuntime, final DOMObserver domObserver, final String nodeId);

}
