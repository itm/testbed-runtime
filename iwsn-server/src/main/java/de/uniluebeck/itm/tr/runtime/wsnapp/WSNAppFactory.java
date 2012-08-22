package de.uniluebeck.itm.tr.runtime.wsnapp;


import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import eu.wisebed.api.v3.common.NodeUrn;

public interface WSNAppFactory {

	WSNApp create(TestbedRuntime testbedRuntime, ImmutableSet<NodeUrn> reservedNodes);

}
