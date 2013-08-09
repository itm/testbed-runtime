package de.uniluebeck.itm.tr.common;

import com.google.inject.Provider;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Wiseml;

public interface WisemlProvider extends Provider<Wiseml> {

	Wiseml get(Iterable<NodeUrn> nodeUrns);

}
