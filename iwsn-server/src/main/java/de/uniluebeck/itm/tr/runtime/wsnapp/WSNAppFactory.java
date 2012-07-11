package de.uniluebeck.itm.tr.runtime.wsnapp;


import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;

public interface WSNAppFactory {

	WSNApp create(TestbedRuntime testbedRuntime, ImmutableSet<String> reservedNodes);

}
