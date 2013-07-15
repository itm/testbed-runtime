package de.uniluebeck.itm.tr.common;

import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.util.Set;

class PreconditionsFactoryImpl implements PreconditionsFactory {

	@Override
	public CommonPreconditions createCommonPreconditions(final Set<NodeUrnPrefix> servedNodeUrnPrefixes,
														 final Set<NodeUrn> servedNodeUrns) {
		return new CommonPreconditionsImpl(
				ServedNodeUrnsProviders.of(servedNodeUrns),
				ServedNodeUrnPrefixesProviders.of(servedNodeUrnPrefixes)
		);
	}

	@Override
	public WSNPreconditions createWsnPreconditions(final Set<NodeUrnPrefix> servedNodeUrnPrefixes,
												   final Set<NodeUrn> servedNodeUrns) {
		return new WSNPreconditionsImpl(createCommonPreconditions(servedNodeUrnPrefixes, servedNodeUrns));
	}

	@Override
	public SessionManagementPreconditions createSessionManagementPreconditions(
			final Set<NodeUrnPrefix> servedNodeUrnPrefixes,
			final Set<NodeUrn> servedNodeUrns) {
		return new SessionManagementPreconditionsImpl(createCommonPreconditions(servedNodeUrnPrefixes, servedNodeUrns));
	}
}
