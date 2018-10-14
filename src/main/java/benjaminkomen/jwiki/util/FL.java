package benjaminkomen.jwiki.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Static Collections and Stream utilities.
 *
 * @author Fastily
 */
public final class FL {

    private FL() {
        // no-args constructor
    }

    /**
     * Collects the elements of a Stream into an ArrayList.
     *
     * @param inputStream The target Stream
     * @param <T1>        The resulting ArrayList will be created containing this type.
     * @return A List containing the elements of the <code>inputStream</code>
     */
    public static <T1> List<T1> toArrayList(Stream<T1> inputStream) {
        return inputStream.collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Collects the elements of a Stream into a HashSet.
     *
     * @param inputStream The target Stream
     * @param <T1>        The resulting HashSet will be created containing this type.
     * @return A HashSet containing the elements of the <code>inputStream</code>
     */
    public static <T1> Set<T1> toSet(Stream<T1> inputStream) {
        return inputStream.collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Creates a Map from a Stream.
     *
     * @param inputStream The Stream to reduce into a Map.
     * @param keyMapper   The function mapping each element of <code>inputStream</code> to a key in the resulting Map.
     * @param valueMapper The function mapping each element of <code>inputStream</code> to a value in the resulting Map.
     * @param <K>         The type of the key in the resulting HashMap.
     * @param <V>         The type of the value in the resulting HashMap.
     * @param <T1>        The type of Object in the Stream.
     * @return A Map, as specified.
     */
    public static <K, V, T1> Map<K, V> toHM(Stream<T1> inputStream, Function<T1, K> keyMapper, Function<T1, V> valueMapper) {
        return new HashMap<>(inputStream.collect(Collectors.toMap(keyMapper, valueMapper)));
    }

    /**
     * Turns an array of Strings into an ArrayList of Strings.
     *
     * @param strings The list of Strings to incorporate.
     * @return The array as an ArrayList
     */
    public static List<String> toStringArrayList(String... strings) {
        return new ArrayList<>(Arrays.asList(strings));
    }

    /**
     * Extracts each key-value pair from a Map and return the pairs as an ArrayList of Tuple objects.
     *
     * @param <T1> The key type of the Map
     * @param <T2> The value type of the Map
     * @param h    The Map to work with
     * @return An ArrayList of Tuples extracted from <code>h</code>.
     */
    public static <T1, T2> List<Tuple<T1, T2>> mapToList(Map<T1, T2> h) {
        return toArrayList(h.entrySet().stream().map(e -> new Tuple<>(e.getKey(), e.getValue())));
    }

    /**
     * Creates a HashMap with String keys and values. Pass in each pair and value (in that order) into <code>inputStrings</code>.
     * This will be one pair entered into resulting HashMap.
     *
     * @param inputStrings The list of elements to turn into a HashMap.
     * @return The resulting HashMap, or null if you specified an odd number of elements.
     */
    public static Map<String, String> produceMap(String... inputStrings) {
        if (inputStrings.length % 2 == 1) {
            throw new IllegalArgumentException("produceMap() cannot work with an odd number of entries!");
        }

        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < inputStrings.length; i += 2) {
            result.put(inputStrings[i], inputStrings[i + 1]);
        }
        return result;
    }

    /**
     * Creates a Stream from an Iterable.
     *
     * @param i   The Iterable to make into a Stream
     * @param <T> {@code i}'s type
     * @return The Stream
     */
    public static <T> Stream<T> streamFrom(Iterable<T> i) {
        return StreamSupport.stream(i.spliterator(), false);
    }

    /**
     * Makes a fence with pipe characters as posts
     *
     * @param planks The planks to use, in order.
     * @return A String with the specified planks and pipe characters as posts
     */
    public static String pipeFence(Collection<String> planks) {
        return String.join("|", planks);
    }

    /**
     * Check if a Collection contains null.
     *
     * @param <T> The type of {@code l}
     * @param l   The Collection to check.
     * @return True if there are no null elements in the Collection.
     */
    public static <T> boolean containsNull(Collection<T> l) {
        return l.stream().anyMatch(Objects::isNull);
    }
}