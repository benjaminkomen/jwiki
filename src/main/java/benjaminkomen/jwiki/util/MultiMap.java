package benjaminkomen.jwiki.util;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Map which allows multiple values for each key. Duplicate values are permitted.
 *
 * @param <K> The type of the key
 * @param <V> The type of the values, which will be stored in an ArrayList.
 * @author Fastily
 */
@Getter
public class MultiMap<K, V> {
    /**
     * The backing structure for this MapList. This is public because a getter would just return a reference to this
     * anyways.
     */
    private final Map<K, List<V>> backingMap = new HashMap<>();

    public MultiMap() {
        // no-args constructor
    }

    /**
     * Creates a new empty ArrayList for {@code key} in this MapList if it did not exist already.  Does nothing otherwise.
     *
     * @param key The key to create a new entry for, if applicable.
     */
    public void touch(K key) {
        if (!backingMap.containsKey(key)) {
            backingMap.put(key, new ArrayList<>());
        }
    }

    /**
     * Adds a key-value pair to this MapList.
     *
     * @param key   The key to add
     * @param value The value to add
     */
    public void put(K key, V value) {
        touch(key);
        backingMap.get(key).add(value);
    }

    /**
     * Merges a List of V objects into the value set for a given key.
     *
     * @param key    The key to use
     * @param values The list of values to merge.
     */
    public void put(K key, List<V> values) {
        touch(key);
        backingMap.get(key).addAll(values);
    }
}