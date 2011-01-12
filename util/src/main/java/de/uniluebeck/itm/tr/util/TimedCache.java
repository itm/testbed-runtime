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

package de.uniluebeck.itm.tr.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimedCache<K, V> implements Map<K, V> {

	private static final long DEFAULT_TIMEOUT = 30;

	private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

	private final ScheduledExecutorService scheduler;

	private final long defaultTimeout;

	private final TimeUnit defaultTimeUnit;

	private TimedCacheListener<K, V> listener;

	/**
	 * Constructs a {@link de.uniluebeck.itm.tr.util.TimedCache} instance with a default timeout of 30 minutes.
	 */
	public TimedCache() {
		this(Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("TimedCache-Thread %d").build()),
				DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT
		);
	}

	public TimedCache(int defaultTimeout, TimeUnit defaultTimeUnit) {
		this(Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("TimedCache-Thread %d").build()),
				defaultTimeout, defaultTimeUnit
		);
	}

	public TimedCache(ScheduledExecutorService scheduler) {
		this(scheduler, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
	}

	public TimedCache(ScheduledExecutorService scheduler, long defaultTimeout, TimeUnit defaultTimeUnit) {
		this.scheduler = scheduler;
		this.defaultTimeout = defaultTimeout;
		this.defaultTimeUnit = defaultTimeUnit;
	}

	private class RemoveRunnable<K2> implements Runnable {

		private K2 key;

		private RemoveRunnable(K2 key) {
			this.key = key;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void run() {

			V value = TimedCache.this.remove(key);

			if (listener != null && value != null) {
				Tuple<Long, TimeUnit> timeout = listener.timeout((K) key, value);
				if (timeout != null) {
					put( (K) key, value, timeout.getFirst(), timeout.getSecond());
				}
			}

		}

	}

	private Map<K, ScheduledFuture<Void>> cleanupMap = new HashMap<K, ScheduledFuture<Void>>();

	private Map<K, V> map = new HashMap<K, V>();

	public synchronized int size() {
		return map.size();
	}

	public synchronized boolean isEmpty() {
		return map.isEmpty();
	}

	public synchronized boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	public synchronized boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	public synchronized V get(Object key) {
		return map.get(key);
	}

	public synchronized V put(K key, V value) {
		return put(key, value, defaultTimeout, defaultTimeUnit);
	}

	@SuppressWarnings("unchecked")
	public synchronized V put(K key, V value, long timeout, TimeUnit timeUnit) {

		V v = map.put(key, value);

		if (v != null) {
			cleanupMap.remove(key).cancel(false);
		}

		cleanupMap.put(key, (ScheduledFuture<Void>) scheduler.schedule(new RemoveRunnable<K>(key), timeout, timeUnit));

		return v;

	}

	@SuppressWarnings("unchecked")
	public synchronized V remove(Object key) {

		V value = map.remove(key);

		if (value != null) {
			//noinspection SuspiciousMethodCalls
			cleanupMap.get(key).cancel(false);
		}

		cleanupMap.remove(key);

		return value;
	}

	public synchronized void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue(), defaultTimeout, defaultTimeUnit);
		}
	}

	public synchronized void clear() {
		for (ScheduledFuture<Void> future : cleanupMap.values()) {
			future.cancel(false);
		}
		cleanupMap.clear();
		map.clear();
	}

	@SuppressWarnings("unused")
	public Tuple<Long, TimeUnit> getDefaultTimeout() {
		return new Tuple<Long, TimeUnit>(defaultTimeout, defaultTimeUnit);
	}

	public synchronized Set<K> keySet() {
		return map.keySet();
	}

	public synchronized Collection<V> values() {
		return map.values();
	}

	public synchronized Set<Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		TimedCache that = (TimedCache) o;

		return map.equals(that.map);

	}

	public synchronized int hashCode() {
		return map.hashCode();
	}

	public void setListener(TimedCacheListener<K, V> listener) {
		this.listener = listener;
	}

}
