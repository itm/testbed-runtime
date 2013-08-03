/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.federator.utils;


import com.google.common.base.Function;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.util.TimedCache;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

class FederationManagerImpl<V> implements FederationManager<V> {

	/**
	 * The function that builds the endpoint from an endpoint URL
	 */
	private final Function<URI, V> endpointBuilderFunction;

	/**
	 * Federated endpoint URL -> Set<URN Prefixes>
	 */
	private final Multimap<URI, NodeUrnPrefix> endpointUrlsToUrnPrefixesMap;

	/**
	 * Caches a mapping from endpoint URL to endpoint.
	 */
	private final TimedCache<URI, V> endpointUrlToEndpointCache = new TimedCache<URI, V>(10, TimeUnit.MINUTES);

	private final Lock endpointUrlToEndpointCacheLock = new ReentrantLock();

	@Inject
	public FederationManagerImpl(@Assisted final Function<URI, V> endpointBuilderFunction,
								 @Assisted final Multimap<URI, NodeUrnPrefix> endpointUrlsToUrnPrefixesMap) {

		this.endpointBuilderFunction = endpointBuilderFunction;
		this.endpointUrlsToUrnPrefixesMap = endpointUrlsToUrnPrefixesMap;
	}

	/**
	 * Returns all {@link Entry} instances.
	 *
	 * @return all {@link Entry} instances
	 */
	@Override
	public ImmutableSet<Entry<V>> getEntries() {

		final ImmutableSet.Builder<Entry<V>> setBuilder = ImmutableSet.builder();

		for (URI uri : endpointUrlsToUrnPrefixesMap.keys()) {

			final V endpointByEndpointUrl = getEndpointByEndpointUrl(uri);
			final ImmutableSet<NodeUrnPrefix> urnPrefixes = ImmutableSet.copyOf(endpointUrlsToUrnPrefixesMap.get(uri));

			setBuilder.add(new Entry<V>(endpointByEndpointUrl, uri, urnPrefixes));
		}

		return setBuilder.build();
	}

	/**
	 * Returns an {@link Entry} instance for the given endpoint URL {@code endpointUrl}.
	 *
	 * @param endpointUrl
	 * 		the endpoint URL to look for
	 *
	 * @return an {@link Entry} instance
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code endpointUrl} is {@code null} or if the endpoint URL {@code endpointUrl}
	 * 		is unknown
	 */
	@Override
	@SuppressWarnings("unused")
	public Entry<V> getEntryByEndpointUrl(final URI endpointUrl) {

		final ImmutableSet<NodeUrnPrefix> urnPrefixes =
				ImmutableSet.copyOf(endpointUrlsToUrnPrefixesMap.get(endpointUrl));

		if (endpointUrl == null) {
			throw new IllegalArgumentException("Unknown endpoint URL \"" + endpointUrl + "\"");
		}

		return new Entry<V>(getEndpointByEndpointUrl(endpointUrl), endpointUrl, urnPrefixes);
	}

	/**
	 * Returns an {@link Entry} instance for the given node URN {@code nodeUrn}.
	 *
	 * @param nodeUrn
	 * 		a nodes URN
	 *
	 * @return an {@link Entry} instance
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code nodeUrn} is {@code null} or if the node URN {@code nodeUrn} is unknown
	 */
	@Override
	@SuppressWarnings("unused")
	public Entry<V> getEntryByNodeUrn(final NodeUrn nodeUrn) {
		for (URI uri : endpointUrlsToUrnPrefixesMap.keys()) {
			final Collection<NodeUrnPrefix> nodeUrnPrefixes = endpointUrlsToUrnPrefixesMap.get(uri);
			for (NodeUrnPrefix nodeUrnPrefix : nodeUrnPrefixes) {
				if (nodeUrn.belongsTo(nodeUrnPrefix)) {
					return new Entry<V>(endpointBuilderFunction.apply(uri), uri, ImmutableSet.copyOf(nodeUrnPrefixes));
				}
			}
		}
		throw new IllegalArgumentException("Node URN " + nodeUrn + "is unknown!");
	}

	/**
	 * Returns an {@link Entry} instance for the given URN prefix {@code urnPrefix}.
	 *
	 * @param urnPrefix
	 * 		the URN prefix
	 *
	 * @return an {@link Entry} instance
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code urnPrefix} is {@code null} or if the URN prefix {@code urnPrefix} is
	 * 		unknown
	 */
	@Override
	@SuppressWarnings("unused")
	public Entry<V> getEntryByUrnPrefix(final NodeUrnPrefix urnPrefix) {
		for (URI uri : endpointUrlsToUrnPrefixesMap.keys()) {
			final Collection<NodeUrnPrefix> nodeUrnPrefixes = endpointUrlsToUrnPrefixesMap.get(uri);
			if (nodeUrnPrefixes.contains(urnPrefix)) {
				return new Entry<V>(endpointBuilderFunction.apply(uri), uri, ImmutableSet.copyOf(nodeUrnPrefixes));
			}
		}
		throw new IllegalArgumentException("NodeUrnPrefix " + urnPrefix + "is unknown!");
	}

	/**
	 * Returns the endpoint URL for the given node URN {@code nodeUrn}.
	 *
	 * @param nodeUrn
	 * 		the node URN
	 *
	 * @return the endpoint URL
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code nodeUrn} is {@code null} or if there's no known endpoint URL for {@code
	 * 		nodeUrn}s URN prefix
	 */
	@Override
	public URI getEndpointUrlByNodeUrn(NodeUrn nodeUrn) {
		return getEntryByNodeUrn(nodeUrn).endpointUrl;
	}

	/**
	 * Returns the endpoint URL for the given URN prefix {@code urnPrefix}.
	 *
	 * @param urnPrefix
	 * 		the URN prefix
	 *
	 * @return the endpoint URL
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code urnPrefix} is {@code null} or if there's no known endpoint URL for {@code
	 * 		nodeUrn}s URN prefix
	 */
	@Override
	public URI getEndpointUrlByUrnPrefix(NodeUrnPrefix urnPrefix) {
		return getEntryByUrnPrefix(urnPrefix).endpointUrl;
	}

	/**
	 * Returns the endpoint for the given node URN {@code nodeUrn}.
	 *
	 * @param nodeUrn
	 * 		the node URN
	 *
	 * @return the endpoint
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code nodeUrn} is {@code null} or if there's no known endpoint URL for {@code
	 * 		nodeUrn}s URN prefix
	 */
	@Override
	public V getEndpointByNodeUrn(NodeUrn nodeUrn) {
		return getEndpointFromCache(getEndpointUrlByNodeUrn(nodeUrn));
	}

	/**
	 * Returns the endpoint for the given URN prefix {@code urnPrefix}.
	 *
	 * @param urnPrefix
	 * 		the URN prefix
	 *
	 * @return the endpoint
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code urnPrefix} is {@code null} or if there's no known endpoint URL for {@code
	 * 		nodeUrn}s URN prefix
	 */
	@Override
	public V getEndpointByUrnPrefix(NodeUrnPrefix urnPrefix) {
		return getEndpointFromCache(getEndpointUrlByUrnPrefix(urnPrefix));
	}

	/**
	 * For a given list of node IDs calculate a mapping between federated endpoint URLs and the corresponding list of node
	 * IDs.
	 *
	 * @param nodeUrns
	 * 		list of node IDs
	 *
	 * @return see above
	 */
	@Override
	public Map<V, List<NodeUrn>> getEndpointToNodeUrnMap(List<NodeUrn> nodeUrns) {

		Map<V, List<NodeUrn>> mapping = Maps.newHashMap();

		for (NodeUrn nodeUrn : nodeUrns) {

			V endpoint = getEndpointByNodeUrn(nodeUrn);

			List<NodeUrn> nodeUrnList = mapping.get(endpoint);
			if (nodeUrnList == null) {
				nodeUrnList = newArrayList();
				mapping.put(endpoint, nodeUrnList);
			}

			nodeUrnList.add(nodeUrn);
		}

		return mapping;
	}

	/**
	 * Returns all federated endpoint URLs.
	 *
	 * @return all federated endpoint URLs
	 */
	@Override
	public ImmutableSet<URI> getEndpointUrls() {
		return ImmutableSet.copyOf(endpointUrlsToUrnPrefixesMap.keySet());
	}

	/**
	 * Returns all federated endpoints.
	 *
	 * @return all federated endpoints
	 */
	@Override
	public ImmutableSet<V> getEndpoints() {
		ImmutableSet.Builder<V> endpointsBuilder = ImmutableSet.builder();
		for (URI endpointUrl : getEndpointUrls()) {
			endpointsBuilder.add(getEndpointFromCache(endpointUrl));
		}
		return endpointsBuilder.build();
	}

	@Override
	public ImmutableBiMap<V, URI> getEndpointsURIMap() {
		final ImmutableBiMap.Builder<V, URI> builder = ImmutableBiMap.builder();
		for (URI endpointUrl : getEndpointUrls()) {
			builder.put(getEndpointFromCache(endpointUrl), endpointUrl);
		}
		return builder.build();
	}

	/**
	 * Returns the URN prefixes served by the endpoint URL {@code endpointUrl}.
	 *
	 * @param endpointUrl
	 * 		the endpoint URL
	 *
	 * @return the set of URN prefixes served by {@code endpointUrl}
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code endpointUrl} is {@code null} or if there's no known endpoint for {@code
	 * 		endpointUrl}
	 */
	@Override
	@SuppressWarnings("unused")
	public ImmutableSet<NodeUrnPrefix> getUrnPrefixesByEndpointUrl(URI endpointUrl) {
		checkNotNull(endpointUrl);
		final ImmutableSet<NodeUrnPrefix> urnPrefixes = ImmutableSet.copyOf(
				endpointUrlsToUrnPrefixesMap.get(endpointUrl)
		);
		if (urnPrefixes == null) {
			throw new IllegalArgumentException("Unknown endpoint URL \"" + endpointUrl + "\"");
		}
		return urnPrefixes;
	}

	/**
	 * Returns the endpoint for the given endpoint URL {@code endpointUrl}.
	 *
	 * @param endpointUrl
	 * 		the endpoint URL
	 *
	 * @return the endpoint
	 */
	@Override
	public V getEndpointByEndpointUrl(final URI endpointUrl) {
		checkNotNull(endpointUrl);
		return getEndpointFromCache(endpointUrl);
	}

	/**
	 * Returns all federated URN prefixes.
	 *
	 * @return all federated URN prefixes
	 */
	@Override
	public ImmutableSet<NodeUrnPrefix> getUrnPrefixes() {
		ImmutableSet.Builder<NodeUrnPrefix> urnPrefixesBuilder = ImmutableSet.builder();
		for (NodeUrnPrefix nodeUrnPrefix : endpointUrlsToUrnPrefixesMap.values()) {
			urnPrefixesBuilder.add(nodeUrnPrefix);
		}
		return urnPrefixesBuilder.build();
	}

	@Override
	public ImmutableSet<NodeUrnPrefix> getUrnPrefixesByNodeUrn(final NodeUrn nodeUrn) {
		for (URI uri : endpointUrlsToUrnPrefixesMap.keys()) {
			final Collection<NodeUrnPrefix> nodeUrnPrefixes = endpointUrlsToUrnPrefixesMap.get(uri);
			for (NodeUrnPrefix nodeUrnPrefix : nodeUrnPrefixes) {
				if (nodeUrnPrefix.belongsTo(nodeUrn)) {
					return ImmutableSet.copyOf(nodeUrnPrefixes);
				}
			}
		}
		return null;
	}

	@Override
	public ImmutableSet<NodeUrnPrefix> getUrnPrefixesByUrnPrefix(final NodeUrnPrefix urnPrefix) {
		for (URI uri : endpointUrlToEndpointCache.keySet()) {
			final Collection<NodeUrnPrefix> nodeUrnPrefixes = endpointUrlsToUrnPrefixesMap.get(uri);
			if (nodeUrnPrefixes.contains(urnPrefix)) {
				return ImmutableSet.copyOf(nodeUrnPrefixes);
			}
		}
		return null;
	}

	@Override
	public boolean servesUrnPrefix(final NodeUrnPrefix urnPrefix) {
		return endpointUrlsToUrnPrefixesMap.containsValue(urnPrefix);
	}

	/**
	 * Retrieves the endpoint for a given endpoint URL from the cache or creates and caches it if it's not currently in
	 * the
	 * cache.
	 *
	 * @param endpointUrl
	 * 		the endpoint URL
	 *
	 * @return the endpoint
	 */
	private V getEndpointFromCache(URI endpointUrl) {

		endpointUrlToEndpointCacheLock.lock();

		try {

			V endpoint = endpointUrlToEndpointCache.get(endpointUrl);

			// create on if it does not exist
			if (endpoint == null) {
				endpoint = endpointBuilderFunction.apply(endpointUrl);
				endpointUrlToEndpointCache.put(endpointUrl, endpoint);
			}

			return endpoint;

		} finally {
			endpointUrlToEndpointCacheLock.unlock();
		}
	}
}
