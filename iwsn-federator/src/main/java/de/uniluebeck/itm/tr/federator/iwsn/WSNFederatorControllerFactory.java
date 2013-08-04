package de.uniluebeck.itm.tr.federator.iwsn;

import de.uniluebeck.itm.tr.federator.utils.FederationManager;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.wsn.WSN;

import java.util.Set;

public interface WSNFederatorControllerFactory {

	WSNFederatorController create(final FederationManager<WSN> wsnFederationManager,
								  Set<NodeUrnPrefix> servedNodeUrnPrefixes, Set<NodeUrn> reservedNodeUrns);

}
