package de.uniluebeck.itm.tr.federator.utils;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.net.URI;

public interface FederatedEndpointsFactory {

	<V> FederatedEndpoints<V> create(Function<URI, V> endpointBuilderFunction,
									 Multimap<URI, NodeUrnPrefix> endpointUrlsToUrnPrefixesMap);

}
