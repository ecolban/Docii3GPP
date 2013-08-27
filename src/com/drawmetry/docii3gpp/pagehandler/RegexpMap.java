package com.drawmetry.docii3gpp.pagehandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of Map that maps regular expressions to objects.
 * 
 * @author ecolban
 * 
 * @param <K>
 *            The type of the key. Must be a String
 * @param <V>
 *            the type of the objects mapped to the keys
 */
public class RegexpMap<K, V> implements Map<K, V> {

	private Map<K, V> map = new HashMap<K, V>();

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean containsKey(Object key) {

		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	/**
	 * Returns the object that is mapped to a regular expression that the given
	 * key matches.
	 */
	@Override
	public V get(Object key) {
		String text = (String) key;
		for (Iterator<K> iterator = map.keySet().iterator(); iterator.hasNext();) {
			String regexpKey = (String) iterator.next();
			if (text.matches(regexpKey)) {
				return map.get(regexpKey);
			}
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public V put(K key, V value) {
		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		map.putAll(m);
	}

	@Override
	public V remove(Object key) {
		return map.remove(key);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

}
