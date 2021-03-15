package cubicchunks.converter.lib.util;

public class ImmutablePair<K, V> {
    private final K key;
    private final V value;

    public ImmutablePair(K k, V v) {
        key = k;
        value = v;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
