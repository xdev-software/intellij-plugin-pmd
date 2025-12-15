/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package software.xdev.pmd.external.org.apache.shiro.lang.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A <code><em>Soft</em>HashMap</code> is a memory-constrained map that stores its <em>values</em> in
 * {@link SoftReference SoftReference}s.  (Contrast this with the JDK's {@link java.util.WeakHashMap WeakHashMap}, which
 * uses weak references for its <em>keys</em>, which is of little value if you want the cache to auto-resize itself
 * based on memory constraints).
 * <p/>
 * Having the values wrapped by soft references allows the cache to automatically reduce its size based on memory
 * limitations and garbage collection.  This ensures that the cache will not cause memory leaks by holding strong
 * references to all of its values.
 * <p/>
 * This class is a generics-enabled Map based on initial ideas from Heinz Kabutz's and Sydney Redelinghuys's
 * <a href="http://www.javaspecialists.eu/archive/Issue015.html">publicly posted version (with their approval)</a>,
 * with continued modifications.
 * <p/>
 * This implementation is thread-safe and usable in concurrent environments.
 *
 * @implNote AB 2025-10-22: Removed retention size as this is not needed
 */
@SuppressWarnings("all")
public class SoftHashMap<K, V> implements Map<K, V>
{
	/**
	 * The internal HashMap that will hold the SoftReference.
	 */
	private final Map<K, SoftValue<V, K>> map;
	
	/**
	 * Reference queue for cleared SoftReference objects.
	 */
	private final ReferenceQueue<? super V> queue;
	
	public SoftHashMap()
	{
		super();
		this.queue = new ReferenceQueue<>();
		this.map = new ConcurrentHashMap<>();
	}
	
	/**
	 * Creates a {@code SoftHashMap} backed by the specified {@code source}.
	 *
	 * @param source the backing map to populate this {@code SoftHashMap}
	 * @see #SoftHashMap(java.util.Map, int)
	 */
	public SoftHashMap(final Map<K, V> source)
	{
		this();
		this.putAll(source);
	}
	
	@Override
	public V get(final Object key)
	{
		this.processQueue();
		
		V result = null;
		final SoftValue<V, K> value = this.map.get(key);
		
		if(value != null)
		{
			// unwrap the 'real' value from the SoftReference
			result = value.get();
			if(result == null)
			{
				// The wrapped value was garbage collected, so remove this entry from the backing map:
				// noinspection SuspiciousMethodCalls
				this.map.remove(key);
			}
		}
		return result;
	}
	
	/**
	 * Traverses the ReferenceQueue and removes garbage-collected SoftValue objects from the backing map by looking
	 * them
	 * up using the SoftValue.key data member.
	 */
	@SuppressWarnings({"unchecked", "SuspiciousMethodCalls"})
	private void processQueue()
	{
		SoftValue sv;
		while((sv = (SoftValue)this.queue.poll()) != null)
		{
			// we can access private data!
			this.map.remove(sv.key);
		}
	}
	
	@Override
	public boolean isEmpty()
	{
		this.processQueue();
		return this.map.isEmpty();
	}
	
	@Override
	public boolean containsKey(final Object key)
	{
		this.processQueue();
		return this.map.containsKey(key);
	}
	
	@Override
	public boolean containsValue(final Object value)
	{
		this.processQueue();
		final Collection values = this.values();
		return values != null && values.contains(value);
	}
	
	@Override
	public void putAll(final Map<? extends K, ? extends V> m)
	{
		if(m == null || m.isEmpty())
		{
			this.processQueue();
			return;
		}
		for(final Map.Entry<? extends K, ? extends V> entry : m.entrySet())
		{
			this.put(entry.getKey(), entry.getValue());
		}
	}
	
	@Override
	public Set<K> keySet()
	{
		this.processQueue();
		return this.map.keySet();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Collection<V> values()
	{
		this.processQueue();
		final Collection<K> keys = this.map.keySet();
		if(keys.isEmpty())
		{
			return Collections.EMPTY_SET;
		}
		final Collection<V> values = new ArrayList<>(keys.size());
		for(final K key : keys)
		{
			final V v = this.get(key);
			if(v != null)
			{
				values.add(v);
			}
		}
		return values;
	}
	
	/**
	 * Creates a new entry, but wraps the value in a SoftValue instance to enable auto garbage collection.
	 */
	@Override
	public V put(final K key, final V value)
	{
		// throw out garbage collected values first
		this.processQueue();
		final SoftValue<V, K> sv = new SoftValue<>(value, key, this.queue);
		final SoftValue<V, K> previous = this.map.put(key, sv);
		return previous != null ? previous.get() : null;
	}
	
	@Override
	public V remove(final Object key)
	{
		// throw out garbage collected values first
		this.processQueue();
		final SoftValue<V, K> raw = this.map.remove(key);
		return raw != null ? raw.get() : null;
	}
	
	@Override
	public void clear()
	{
		// throw out garbage collected values
		this.processQueue();
		this.map.clear();
	}
	
	@Override
	public int size()
	{
		// throw out garbage collected values first
		this.processQueue();
		return this.map.size();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Set<Map.Entry<K, V>> entrySet()
	{
		// throw out garbage collected values first
		this.processQueue();
		final Collection<K> keys = this.map.keySet();
		if(keys.isEmpty())
		{
			// noinspection unchecked
			return Collections.EMPTY_SET;
		}
		
		final Map<K, V> kvPairs = new HashMap<>(keys.size());
		for(final K key : keys)
		{
			final V v = this.get(key);
			if(v != null)
			{
				kvPairs.put(key, v);
			}
		}
		return kvPairs.entrySet();
	}
	
	/**
	 * We define our own subclass of SoftReference which contains not only the value but also the key to make it easier
	 * to find the entry in the HashMap after it's been garbage collected.
	 */
	private static final class SoftValue<V, K> extends SoftReference<V>
	{
		
		private final K key;
		
		/**
		 * Constructs a new instance, wrapping the value, key, and queue, as required by the superclass.
		 *
		 * @param value the map value
		 * @param key   the map key
		 * @param queue the soft reference queue to poll to determine if the entry had been reaped by the GC.
		 */
		private SoftValue(final V value, final K key, final ReferenceQueue<? super V> queue)
		{
			super(value, queue);
			this.key = key;
		}
	}
}
