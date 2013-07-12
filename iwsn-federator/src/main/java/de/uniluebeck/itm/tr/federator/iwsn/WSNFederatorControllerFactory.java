package de.uniluebeck.itm.tr.federator.iwsn;

import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.util.Set;

public interface WSNFederatorControllerFactory {

	WSNFederatorController create(Set<NodeUrnPrefix> servedNodeUrnPrefixes, Set<NodeUrn> reservedNodeUrns);

}
