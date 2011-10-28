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

package de.uniluebeck.itm.tr.federatorutils;


import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.uniluebeck.itm.tr.util.TimedCache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;

public class FederationManager<V> {

	public static class Entry<V> {

		public final V endpoint;

		public final String endpointUrl;

		public final ImmutableSet<String> urnPrefixes;

		public Entry(final V endpoint, final String endpointUrl, final ImmutableSet<String> urnPrefixes) {
			this.endpoint = endpoint;
			this.endpointUrl = endpointUrl;
			this.urnPrefixes = urnPrefixes;
		}
	}

	/**
	 * The function that builds the endpoint from an endpoint URL
	 */
	private final Function<String, V> endpointBuilderFunction;

	/**
	 * Federated endpoint URL -> Set<URN Prefixes>
	 */
	private final ImmutableMap<String, ImmutableSet<String>> endpointUrlsToUrnPrefixesMap;

	/**
	 * Caches a mapping from endpoint URL to endpoint.
	 */
	private final TimedCache<String, V> endpointUrlToEndpointCache = new TimedCache<String, V>(10, TimeUnit.MINUTES);

	private final Lock endpointUrlToEndpointCacheLock = new ReentrantLock();

	public FederationManager(final Function<String, V> endpointBuilderFunction,
							 final ImmutableMap<String, ImmutableSet<String>> endpointUrlsToUrnPrefixesMap) {

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

		for (Map.Entry<String, ImmutableSet<String>> entry : endpointUrlsToUrnPrefixesMap.entrySet()) {

			final String endpointUrl = entry.getKey();
			final ImmutableSet<String> urnPrefixes = entry.getValue();

			setBuilder.add(new Entry(getEndpointByEndpointUrl(endpointUrl), endpointUrl, urnPrefixes));
		}

		return setBuilder.build();
	}

	/**
	 * Returns an {@link Entry} instance for the given endpoint URL {@code endpointUrl}.
	 *
	 * @param endpointUrl the endpoint URL to look for
	 *
	 * @return an {@link Entry} instance
	 *
	 * @throws IllegalArgumentException if {@code endpointUrl} is {@code null} or if the endpoint URL {@code endpointUrl}
	 *                                  is unknown
	 */
	@SuppressWarnings("unused")
	public Entry getEntryByEndpointUrl(final String endpointUrl) {

		final ImmutableSet<String> urnPrefixes = endpointUrlsToUrnPrefixesMap.get(endpointUrl);

		if (endpointUrl == null) {
			throw new IllegalArgumentException("Unknown endpoint URL \"" + endpointUrl + "\"");
		}

		return new Entry(
				getEndpointByEndpointUrl(endpointUrl),
				endpointUrl,
				urnPrefixes
		);
	}

	/**
	 * Returns an {@link Entry} instance for the given node URN {@code nodeUrn}.
	 *
	 * @param nodeUrn a nodes URN
	 *
	 * @return an {@link Entry} instance
	 *
	 * @throws IllegalArgumentException if {@code nodeUrn} is {@code null} or if the node URN {@code nodeUrn} is unknown
	 */
	@SuppressWarnings("unused")
	public Entry getEntryByNodeUrn(final String nodeUrn) {
		return getEntryByPrefixesMapEntry(getPrefixesMapEntryByNodeUrn(nodeUrn));
	}

	/**
	 * Returns an {@link Entry} instance for the given URN prefix {@code urnPrefix}.
	 *
	 * @param urnPrefix the URN prefix
	 *
	 * @return an {@link Entry} instance
	 *
	 * @throws IllegalArgumentException if {@code urnPrefix} is {@code null} or if the URN prefix {@code urnPrefix} is
	 *                                  unknown
	 */
	@SuppressWarnings("unused")
	public Entry getEntryByUrnPrefix(final String urnPrefix) {
		return getEntryByPrefixesMapEntry(getPrefixesMapEntryByUrnPrefix(urnPrefix));
	}

	/**
	 * Returns the endpoint URL for the given node URN {@code nodeUrn}.
	 *
	 * @param nodeUrn the node URN
	 *
	 * @return the endpoint URL
	 *
	 * @throws IllegalArgumentException if {@code nodeUrn} is {@code null} or if there's no known endpoint URL for {@code
	 *                                  nodeUrn}s URN prefix
	 */
	public String getEndpointUrlByNodeUrn(String nodeUrn) {
		return getPrefixesMapEntryByNodeUrn(nodeUrn).getKey();
	}

	/**
	 * Returns the endpoint URL for the given URN prefix {@code urnPrefix}.
	 *
	 * @param urnPrefix the URN prefix
	 *
	 * @return the endpoint URL
	 *
	 * @throws IllegalArgumentException if {@code urnPrefix} is {@code null} or if there's no known endpoint URL for {@code
	 *                                  nodeUrn}s URN prefix
	 */
	public String getEndpointUrlByUrnPrefix(String urnPrefix) {
		return getPrefixesMapEntryByUrnPrefix(urnPrefix).getKey();
	}

	/**
	 * Returns the endpoint for the given node URN {@code nodeUrn}.
	 *
	 * @param nodeUrn the node URN
	 *
	 * @return the endpoint
	 *
	 * @throws IllegalArgumentException if {@code nodeUrn} is {@code null} or if there's no known endpoint URL for {@code
	 *                                  nodeUrn}s URN prefix
	 */
	public V getEndpointByNodeUrn(String nodeUrn) {
		return getEndpointFromCache(getEndpointUrlByNodeUrn(nodeUrn));
	}

	/**
	 * Returns the endpoint for the given URN prefix {@code urnPrefix}.
	 *
	 * @param urnPrefix the URN prefix
	 *
	 * @return the endpoint
	 *
	 * @throws IllegalArgumentException if {@code urnPrefix} is {@code null} or if there's no known endpoint URL for {@code
	 *                                  nodeUrn}s URN prefix
	 */
	public V getEndpointByUrnPrefix(String urnPrefix) {

		String endpointUrl = getEndpointUrlByUrnPrefix(urnPrefix);
		return getEndpointFromCache(endpointUrl);
	}

	/**
	 * For a given list of node IDs calculate a mapping between federated endpoint URLs and the corresponding list of node
	 * IDs.
	 *
	 * @param nodeUrns list of node IDs
	 *
	 * @return see above
	 */
	public Map<V, List<String>> getEndpointToServedUrnPrefixesMap(List<String> nodeUrns) {

		Map<V, List<String>> mapping = Maps.newHashMap();

		for (String nodeUrn : nodeUrns) {

			V endpoint = getEndpointByNodeUrn(nodeUrn);

			List<String> nodeIdList = mapping.get(endpoint);
			if (nodeIdList == null) {
				nodeIdList = Lists.newArrayList();
				mapping.put(endpoint, nodeIdList);
			}
			nodeIdList.add(nodeUrn);

		}

		return mapping;
	}

	/**
	 * Returns all federated endpoint URLs.
	 *
	 * @return all federated endpoint URLs
	 */
	public ImmutableSet<String> getEndpointUrls() {
		return endpointUrlsToUrnPrefixesMap.keySet();
	}

	/**
	 * Returns all federated endpoints.
	 *
	 * @return all federated endpoints
	 */
	public ImmutableSet<V> getEndpoints() {
		ImmutableSet.Builder<V> endpointsBuilder = ImmutableSet.builder();
		for (String endpointUrl : getEndpointUrls()) {
			endpointsBuilder.add(getEndpointFromCache(endpointUrl));
		}
		return endpointsBuilder.build();
	}

	/**
	 * Returns the URN prefixes served by the endpoint URL {@code endpointUrl}.
	 *
	 * @param endpointUrl the endpoint URL
	 *
	 * @return the set of URN prefixes served by {@code endpointUrl}
	 *
	 * @throws IllegalArgumentException if {@code endpointUrl} is {@code null} or if there's no known endpoint for {@code
	 *                                  endpointUrl}
	 */
	@SuppressWarnings("unused")
	public ImmutableSet<String> getUrnPrefixesByEndpointUrl(String endpointUrl) {
		checkNotNull(endpointUrl);
		final ImmutableSet<String> urnPrefixes = endpointUrlsToUrnPrefixesMap.get(endpointUrl);
		if (urnPrefixes == null) {
			throw new IllegalArgumentException("Unknown endpoint URL \"" + endpointUrl + "\"");
		}
		return urnPrefixes;
	}

	/**
	 * Returns the endpoint for the given endpoint URL {@code endpointUrl}.
	 *
	 * @param endpointUrl the endpoint URL
	 *
	 * @return the endpoint
	 */
	public V getEndpointByEndpointUrl(final String endpointUrl) {
		checkNotNull(endpointUrl);
		return getEndpointFromCache(endpointUrl);
	}

	/**
	 * Returns all federated URN prefixes.
	 *
	 * @return all federated URN prefixes
	 */
	public ImmutableSet<String> getUrnPrefixes() {
		ImmutableSet.Builder<String> urnPrefixesBuilder = ImmutableSet.builder();
		for (ImmutableSet<String> federatedEndpointUrnPrefixes : endpointUrlsToUrnPrefixesMap.values()) {
			urnPrefixesBuilder.addAll(federatedEndpointUrnPrefixes);
		}
		return urnPrefixesBuilder.build();
	}

	/**
	 * Returns all URN prefixes served by the testbed that serves the node URN {@code nodeUrn}.
	 *
	 * @param nodeUrn a node URN in the testbed
	 *
	 * @return all URN prefixes served by the testbed
	 */
	@SuppressWarnings("unused")
	public ImmutableSet<String> getUrnPrefixesByNodeUrn(final String nodeUrn) {
		return getPrefixesMapEntryByUrnPrefix(nodeUrn).getValue();
	}

	/**
	 * Returns all URN prefixes served by the testbed that serves the URN prefix {@code urnPrefix}.
	 *
	 * @param urnPrefix a URN prefix served by the testbed
	 *
	 * @return all URN prefixes served by the testbed
	 */
	@SuppressWarnings("unused")
	public ImmutableSet<String> getUrnPrefixesByUrnPrefix(final String urnPrefix) {
		return getPrefixesMapEntryByUrnPrefix(urnPrefix).getValue();
	}

	private Map.Entry<String, ImmutableSet<String>> getPrefixesMapEntryByNodeUrn(final String nodeUrn) {
		checkNotNull(nodeUrn);
		for (Map.Entry<String, ImmutableSet<String>> entry : endpointUrlsToUrnPrefixesMap.entrySet()) {
			final ImmutableSet<String> servedUrnPrefixes = entry.getValue();
			for (String servedUrnPrefix : servedUrnPrefixes) {
				if (nodeUrn.startsWith(servedUrnPrefix)) {
					return entry;
				}
			}
		}
		throw new IllegalArgumentException("Unknown node URN \"" + nodeUrn + "\"");
	}

	private Map.Entry<String, ImmutableSet<String>> getPrefixesMapEntryByUrnPrefix(final String urnPrefix) {
		checkNotNull(urnPrefix);
		for (Map.Entry<String, ImmutableSet<String>> entry : endpointUrlsToUrnPrefixesMap.entrySet()) {
			final ImmutableSet<String> servedUrnPrefixes = entry.getValue();
			if (servedUrnPrefixes.contains(urnPrefix)) {
				return entry;
			}
		}
		throw new IllegalArgumentException("Unknown URN prefix \"" + urnPrefix + "\"");
	}

	private Entry getEntryByPrefixesMapEntry(final Map.Entry<String, ImmutableSet<String>> mapEntry) {
		return new Entry(
				getEndpointByEndpointUrl(mapEntry.getKey()),
				mapEntry.getKey(),
				mapEntry.getValue()
		);
	}

	/**
	 * Retrieves the endpoint for a given endpoint URL from the cache or creates and caches it if it's not currently in the
	 * cache.
	 *
	 * @param endpointUrl the endpoint URL
	 *
	 * @return the endpoint
	 */
	private V getEndpointFromCache(String endpointUrl) {

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
