package de.uniluebeck.itm.tr.common;

import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.util.Collection;
import java.util.Set;

public interface CommonPreconditions {

	void checkNodeUrnsPrefixesServed(NodeUrn... nodeUrns);

	void checkNodeUrnsPrefixesServed(Collection<NodeUrn> nodeUrns);

	void checkUrnPrefixesServed(Set<NodeUrnPrefix> urnPrefixes);

	void checkNodesKnown(NodeUrn... nodeUrns);

	void checkNodesKnown(Collection<NodeUrn> nodeUrns);
}
