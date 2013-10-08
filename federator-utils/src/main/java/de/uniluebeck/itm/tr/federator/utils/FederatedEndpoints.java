package de.uniluebeck.itm.tr.federator.utils;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.net.URI;
import java.util.List;
import java.util.Map;

public interface FederatedEndpoints<V> {

	public static class Entry<V> {

		public final V endpoint;

		public final URI endpointUrl;

		public final ImmutableSet<NodeUrnPrefix> urnPrefixes;

		public Entry(final V endpoint, final URI endpointUrl, final ImmutableSet<NodeUrnPrefix> urnPrefixes) {
			this.endpoint = endpoint;
			this.endpointUrl = endpointUrl;
			this.urnPrefixes = urnPrefixes;
		}

	}

	ImmutableSet<Entry<V>> getEntries();

	Entry<V> getEntryByEndpointUrl(URI endpointUrl);

	Entry<V> getEntryByNodeUrn(NodeUrn nodeUrn);

	Entry<V> getEntryByUrnPrefix(NodeUrnPrefix urnPrefix);

	URI getEndpointUrlByNodeUrn(NodeUrn nodeUrn);

	URI getEndpointUrlByUrnPrefix(NodeUrnPrefix urnPrefix);

	V getEndpointByNodeUrn(NodeUrn nodeUrn);

	V getEndpointByUrnPrefix(NodeUrnPrefix urnPrefix);

	Map<V, List<NodeUrn>> getEndpointToNodeUrnMap(List<NodeUrn> nodeUrns);

	ImmutableSet<URI> getEndpointUrls();

	ImmutableSet<V> getEndpoints();

	ImmutableBiMap<V, URI> getEndpointsURIMap();

	ImmutableSet<NodeUrnPrefix> getUrnPrefixesByEndpointUrl(URI endpointUrl);

	V getEndpointByEndpointUrl(URI endpointUrl);

	ImmutableSet<NodeUrnPrefix> getUrnPrefixes();

	/**
	 * Returns all URN prefixes served by the testbed that serves the node URN {@code nodeUrn}.
	 *
	 * @param nodeUrn
	 * 		a node URN in the testbed
	 *
	 * @return all URN prefixes served by the testbed
	 */
	ImmutableSet<NodeUrnPrefix> getUrnPrefixesByNodeUrn(NodeUrn nodeUrn);

	/**
	 * Returns all URN prefixes served by the testbed that serves the URN prefix {@code urnPrefix}.
	 *
	 * @param urnPrefix
	 * 		a URN prefix served by the testbed
	 *
	 * @return all URN prefixes served by the testbed
	 */
	ImmutableSet<NodeUrnPrefix> getUrnPrefixesByUrnPrefix(NodeUrnPrefix urnPrefix);

	boolean servesUrnPrefix(NodeUrnPrefix urnPrefix);
}
