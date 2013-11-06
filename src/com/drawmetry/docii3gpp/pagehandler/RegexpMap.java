package com.drawmetry.docii3gpp.pagehandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
public class RegexpMap<K extends String, V> implements Map<K, V> {

	private Map<K, V> map = new HashMap<K, V>();
	private List<String> keyList = new ArrayList<String>();

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
	 * Returns the object that is mapped to the first regular expression that
	 * the given key matches.
	 */
	@Override
	public V get(Object key) {
		String keyString = (String) key;
		for (Iterator<String> iterator = keyList.iterator(); iterator.hasNext();) {
			String regexp = iterator.next();
			if (keyString.matches(regexp)) {
				return map.get(regexp);
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
		if (!keyList.contains(key)) {
			keyList.add(key);
		}
		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Iterator<? extends K> iterator = m.keySet().iterator(); iterator
				.hasNext();) {
			String key = (String) iterator.next();
			if (!keyList.contains(key)) {
				keyList.add(key);
			}
		}
		map.putAll(m);
	}

	@Override
	public V remove(Object key) {
		keyList.remove(key);
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
