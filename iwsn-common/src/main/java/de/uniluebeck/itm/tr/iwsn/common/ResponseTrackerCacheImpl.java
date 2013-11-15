package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ResponseTrackerCacheImpl implements ResponseTrackerCache {

	private final Cache<Long, ResponseTracker> responseTrackerCache = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS)
			.build();

	@Override
	public ConcurrentMap<Long,ResponseTracker> asMap() {
		return responseTrackerCache.asMap();
	}

	@Override
	public void cleanUp() {
		responseTrackerCache.cleanUp();
	}

	@Override
	public ResponseTracker get(final Long key,
							   final Callable<? extends ResponseTracker> valueLoader) throws ExecutionException {
		return responseTrackerCache.get(key, valueLoader);
	}

	@Override
	public ImmutableMap<Long,ResponseTracker> getAllPresent(final Iterable<?> keys) {
		return responseTrackerCache.getAllPresent(keys);
	}

	@Override
	@Nullable
	public ResponseTracker getIfPresent(final Object key) {
		return responseTrackerCache.getIfPresent(key);
	}

	@Override
	public void invalidate(final Object key) {
		responseTrackerCache.invalidate(key);
	}

	@Override
	public void invalidateAll() {
		responseTrackerCache.invalidateAll();
	}

	@Override
	public void invalidateAll(final Iterable<?> keys) {
		responseTrackerCache.invalidateAll(keys);
	}

	@Override
	public void put(final Long key, final ResponseTracker value) {
		responseTrackerCache.put(key, value);
	}

	@Override
	public void putAll(
			final Map<? extends Long, ? extends ResponseTracker> m) {
		responseTrackerCache.putAll(m);
	}

	@Override
	public long size() {
		return responseTrackerCache.size();
	}

	@Override
	public CacheStats stats() {
		return responseTrackerCache.stats();
	}
}
