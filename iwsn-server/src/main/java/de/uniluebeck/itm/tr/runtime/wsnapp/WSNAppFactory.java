package de.uniluebeck.itm.tr.runtime.wsnapp;


import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;

public class WSNAppFactory {

	public static WSNApp create(final TestbedRuntime testbedRuntime, final ImmutableSet<String> reservedNodes) {
		return new WSNAppImpl(testbedRuntime, reservedNodes);
	}

}
