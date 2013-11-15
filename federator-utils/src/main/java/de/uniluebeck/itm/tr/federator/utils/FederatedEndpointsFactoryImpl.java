package de.uniluebeck.itm.tr.federator.utils;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.net.URI;

public class FederatedEndpointsFactoryImpl implements FederatedEndpointsFactory {

	@Override
	public <V> FederatedEndpoints<V> create(final Function<URI, V> endpointBuilderFunction,
										   final Multimap<URI, NodeUrnPrefix> endpointUrlsToUrnPrefixesMap) {
		return new FederatedEndpointsImpl<V>(endpointBuilderFunction, endpointUrlsToUrnPrefixesMap);
	}
}
