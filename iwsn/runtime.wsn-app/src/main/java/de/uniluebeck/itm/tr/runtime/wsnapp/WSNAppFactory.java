package de.uniluebeck.itm.tr.runtime.wsnapp;


import de.uniluebeck.itm.gtr.TestbedRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WSNAppFactory {

	public static WSNApp create(final TestbedRuntime testbedRuntime, final String[] reservedNodes) {
		return new WSNAppImpl(testbedRuntime, reservedNodes);
	}

}
