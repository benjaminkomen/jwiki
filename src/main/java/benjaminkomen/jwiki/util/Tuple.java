package benjaminkomen.jwiki.util;

/**
 * Simple implementation of a Tuple. A Tuple is an immutable paired 2-set of values (e.g. (x, y)), and may consist of
 * any two Objects (may be in the same class or a different class).
 *
 * @param <K> The type of Object allowed for the first Object in the tuple.
 * @param <V> The type of Object allowed for the second Object in the tuple.
 * @author Fastily
 */
public class Tuple<K, V> {
    /**
     * The first value of the tuple
     */
    public final K value1;

    /**
     * The second value of the tuple
     */
    public final V value2;

    /**
     * Creates a Tuple from the parameter values.
     *
     * @param value1 The first value of the tuple
     * @param value2 The second value of the tuple
     */
    public Tuple(K value1, V value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    /**
     * Gets a String representation of this object. Nice for debugging.
     */
    @Override
    public String toString() {
        return String.format("( %s, %s )", value1, value2);
    }

    /**
     * Gets a hashcode for this object. Good for mapping constructs.
     */
    @Override
    public int hashCode() {
        return value1.hashCode() ^ value2.hashCode();
    }

    /**
     * Determines if two tuples are equal. Equal tuples have the same two elements in the same order.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Tuple)) {
            return false;
        }

        Tuple<?, ?> temp = (Tuple<?, ?>) other;
        return value1.equals(temp.value1) && value2.equals(temp.value2);
    }
}