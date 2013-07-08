package de.uniluebeck.itm.tr.common;

import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.util.Set;

public interface PreconditionsFactory {

	CommonPreconditions createCommonPreconditions(Set<NodeUrnPrefix> servedNodeUrnPrefixes,
												  Set<NodeUrn> servedNodeUrns);

	WSNPreconditions createWsnPreconditions(Set<NodeUrnPrefix> servedNodeUrnPrefixes,
											Set<NodeUrn> servedNodeUrns);

	SessionManagementPreconditions createSessionManagementPreconditions(Set<NodeUrnPrefix> servedNodeUrnPrefixes,
																		Set<NodeUrn> servedNodeUrns);

}
