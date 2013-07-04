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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import de.uniluebeck.itm.util.TimedCache;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

public class FederationManager<V> {

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

	/**
	 * The function that builds the endpoint from an endpoint URL
	 */
	private final Function<URI, V> endpointBuilderFunction;

	/**
	 * Federated endpoint URL -> Set<URN Prefixes>
	 */
	private final ImmutableMap<URI, ImmutableSet<NodeUrnPrefix>> endpointUrlsToUrnPrefixesMap;

	/**
	 * Caches a mapping from endpoint URL to endpoint.
	 */
	private final TimedCache<URI, V> endpointUrlToEndpointCache = new TimedCache<URI, V>(10, TimeUnit.MINUTES);

	private final Lock endpointUrlToEndpointCacheLock = new ReentrantLock();

	public FederationManager(final Function<URI, V> endpointBuilderFunction,
							 final ImmutableMap<URI, ImmutableSet<NodeUrnPrefix>> endpointUrlsToUrnPrefixesMap) {

		this.endpointBuilderFunction = endpointBuilderFunction;
		this.endpointUrlsToUrnPrefixesMap = endpointUrlsToUrnPrefixesMap;
	}

	/**
	 * Returns all {@link Entry} instances.
	 *
	 * @return all {@link Entry} instances
	 */
	public ImmutableSet<Entry<V>> getEntries() {

		final ImmutableSet.Builder<Entry<V>> setBuilder = ImmutableSet.builder();

		for (Map.Entry<URI, ImmutableSet<NodeUrnPrefix>> entry : endpointUrlsToUrnPrefixesMap.entrySet()) {

			final URI endpointUrl = entry.getKey();
			final V endpointByEndpointUrl = getEndpointByEndpointUrl(endpointUrl);
			final ImmutableSet<NodeUrnPrefix> urnPrefixes = entry.getValue();

			setBuilder.add(new Entry<V>(endpointByEndpointUrl, endpointUrl, urnPrefixes));
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
	@SuppressWarnings("unused")
	public Entry<V> getEntryByEndpointUrl(final URI endpointUrl) {

		final ImmutableSet<NodeUrnPrefix> urnPrefixes = endpointUrlsToUrnPrefixesMap.get(endpointUrl);

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
	@SuppressWarnings("unused")
	public Entry<V> getEntryByNodeUrn(final NodeUrn nodeUrn) {
		return getEntryByPrefixesMapEntry(getPrefixesMapEntryByNodeUrn(nodeUrn));
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
	@SuppressWarnings("unused")
	public Entry<V> getEntryByUrnPrefix(final NodeUrnPrefix urnPrefix) {
		return getEntryByPrefixesMapEntry(getPrefixesMapEntryByUrnPrefix(urnPrefix));
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
	public URI getEndpointUrlByNodeUrn(NodeUrn nodeUrn) {
		return getPrefixesMapEntryByNodeUrn(nodeUrn).getKey();
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
	public URI getEndpointUrlByUrnPrefix(NodeUrnPrefix urnPrefix) {
		return getPrefixesMapEntryByUrnPrefix(urnPrefix).getKey();
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
	public ImmutableSet<URI> getEndpointUrls() {
		return endpointUrlsToUrnPrefixesMap.keySet();
	}

	/**
	 * Returns all federated endpoints.
	 *
	 * @return all federated endpoints
	 */
	public ImmutableSet<V> getEndpoints() {
		ImmutableSet.Builder<V> endpointsBuilder = ImmutableSet.builder();
		for (URI endpointUrl : getEndpointUrls()) {
			endpointsBuilder.add(getEndpointFromCache(endpointUrl));
		}
		return endpointsBuilder.build();
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
	@SuppressWarnings("unused")
	public ImmutableSet<NodeUrnPrefix> getUrnPrefixesByEndpointUrl(URI endpointUrl) {
		checkNotNull(endpointUrl);
		final ImmutableSet<NodeUrnPrefix> urnPrefixes = endpointUrlsToUrnPrefixesMap.get(endpointUrl);
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
	public V getEndpointByEndpointUrl(final URI endpointUrl) {
		checkNotNull(endpointUrl);
		return getEndpointFromCache(endpointUrl);
	}

	/**
	 * Returns all federated URN prefixes.
	 *
	 * @return all federated URN prefixes
	 */
	public ImmutableSet<NodeUrnPrefix> getUrnPrefixes() {
		ImmutableSet.Builder<NodeUrnPrefix> urnPrefixesBuilder = ImmutableSet.builder();
		for (ImmutableSet<NodeUrnPrefix> federatedEndpointUrnPrefixes : endpointUrlsToUrnPrefixesMap.values()) {
			urnPrefixesBuilder.addAll(federatedEndpointUrnPrefixes);
		}
		return urnPrefixesBuilder.build();
	}

	/**
	 * Returns all URN prefixes served by the testbed that serves the node URN {@code nodeUrn}.
	 *
	 * @param nodeUrn
	 * 		a node URN in the testbed
	 *
	 * @return all URN prefixes served by the testbed
	 */
	@SuppressWarnings("unused")
	public ImmutableSet<NodeUrnPrefix> getUrnPrefixesByNodeUrn(final NodeUrn nodeUrn) {
		return getPrefixesMapEntryByUrnPrefix(nodeUrn.getPrefix()).getValue();
	}

	/**
	 * Returns all URN prefixes served by the testbed that serves the URN prefix {@code urnPrefix}.
	 *
	 * @param urnPrefix
	 * 		a URN prefix served by the testbed
	 *
	 * @return all URN prefixes served by the testbed
	 */
	@SuppressWarnings("unused")
	public ImmutableSet<NodeUrnPrefix> getUrnPrefixesByUrnPrefix(final NodeUrnPrefix urnPrefix) {
		return getPrefixesMapEntryByUrnPrefix(urnPrefix).getValue();
	}

	public boolean servesUrnPrefix(final NodeUrnPrefix urnPrefix) {
		for (ImmutableSet<NodeUrnPrefix> prefixes : endpointUrlsToUrnPrefixesMap.values()) {
			if (prefixes.contains(urnPrefix)) {
				return true;
			}
		}
		return false;
	}

	private Map.Entry<URI, ImmutableSet<NodeUrnPrefix>> getPrefixesMapEntryByNodeUrn(final NodeUrn nodeUrn) {
		checkNotNull(nodeUrn);
		for (Map.Entry<URI, ImmutableSet<NodeUrnPrefix>> entry : endpointUrlsToUrnPrefixesMap.entrySet()) {
			final ImmutableSet<NodeUrnPrefix> servedUrnPrefixes = entry.getValue();
			for (NodeUrnPrefix servedUrnPrefix : servedUrnPrefixes) {
				if (nodeUrn.belongsTo(servedUrnPrefix)) {
					return entry;
				}
			}
		}
		throw new IllegalArgumentException("Unknown node URN \"" + nodeUrn + "\"");
	}

	private Map.Entry<URI, ImmutableSet<NodeUrnPrefix>> getPrefixesMapEntryByUrnPrefix(
			final NodeUrnPrefix urnPrefix) {
		checkNotNull(urnPrefix);
		for (Map.Entry<URI, ImmutableSet<NodeUrnPrefix>> entry : endpointUrlsToUrnPrefixesMap.entrySet()) {
			final ImmutableSet<NodeUrnPrefix> servedUrnPrefixes = entry.getValue();
			if (servedUrnPrefixes.contains(urnPrefix)) {
				return entry;
			}
		}
		throw new IllegalArgumentException("Unknown URN prefix \"" + urnPrefix + "\"");
	}

	private Entry<V> getEntryByPrefixesMapEntry(final Map.Entry<URI, ImmutableSet<NodeUrnPrefix>> mapEntry) {

		final URI endpointUrl = mapEntry.getKey();
		final V endpoint = getEndpointByEndpointUrl(endpointUrl);
		final ImmutableSet<NodeUrnPrefix> urnPrefixes = mapEntry.getValue();

		return new Entry<V>(endpoint, endpointUrl, urnPrefixes);
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
