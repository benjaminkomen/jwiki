package benjaminkomen.jwiki.util;

import com.google.gson.*;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Static utility methods for use with Gson.
 *
 * @author Fastily
 */
public class GSONP {

    private static final Logger LOG = LoggerFactory.getLogger(GSONP.class);

    /**
     * Default json deserializer for Instant objects.
     */
    private static JsonDeserializer<Instant> instantDeserializer = (j, t, c) -> Instant.parse(j.getAsJsonPrimitive().getAsString());

    /**
     * Default json deserializer for HttpUrl objects.
     */
    private static JsonDeserializer<HttpUrl> httpurlDeserializer = (j, t, c) -> HttpUrl.parse(j.getAsJsonPrimitive().getAsString());

    /**
     * Default Gson object, for convenience.
     */
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Instant.class, instantDeserializer)
            .registerTypeAdapter(HttpUrl.class, httpurlDeserializer).create();

    /**
     * Gson object which generates pretty-print (human-readable) JSON.
     */
    private static final Gson GSON_PRETTY_PRINT = new GsonBuilder().setPrettyPrinting().create();

    private GSONP() {
        // no-args constructor
    }

    public static Gson getGsonPrettyPrint() {
        return GSON_PRETTY_PRINT;
    }

    public static Gson getGson() {
        return GSON;
    }

    /**
     * Convert a JsonObject of JsonObject to an ArrayList of JsonObject.
     *
     * @param input A JsonObject containing only other JsonObject objects.
     * @return An ArrayList of JsonObject derived from {@code input}.
     */
    public static List<JsonObject> convertJsonObjectToList(JsonObject input) {
        return FL.toArrayList(input.entrySet().stream().map(e -> e.getValue().getAsJsonObject()));
    }

    /**
     * Convert a JsonArray of JsonObject to an ArrayList of JsonObject.
     *
     * @param input A JsonArray of JsonObject.
     * @return An ArrayList of JsonObject derived from {@code input}.
     */
    public static List<JsonObject> getJsonArrayofJsonObject(JsonArray input) {
        try {
            return FL.toArrayList(FL.streamFrom(input).map(JsonElement::getAsJsonObject));
        } catch (Exception e) {
            LOG.error("Error during json conversion", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get a JsonArray of JsonObject as a List of JsonObject. PRECONDITION: {@code key} points to a JsonArray of
     * JsonObject in {@code input}
     *
     * @param input The source JsonObject.
     * @param key   Points to a JsonArray of JsonObject
     * @return An ArrayList of JsonObject derived from {@code input}, or an empty ArrayList if a JsonArray associated
     * with {@code key} could not be found in {@code input}
     */
    public static List<JsonObject> getJsonArrayofJsonObject(JsonObject input, String key) {
        JsonArray ja = input.getAsJsonArray(key);
        return ja != null
                ? getJsonArrayofJsonObject(input.getAsJsonArray(key))
                : Collections.emptyList();
    }

    /**
     * Extract a pair of String values from each JsonObject in an ArrayList of JsonObject
     *
     * @param input The source List
     * @param kk    Points to each key in to be used in the resulting Map.
     * @param vk    Points to each value in to be used in the resulting Map.
     * @return The pairs of String values.
     */
    public static Map<String, String> pairOff(List<JsonObject> input, String kk, String vk) {
        return new HashMap<>(input.stream()
                .collect(Collectors.toMap(e -> getString(e, kk), e -> getString(e, vk)))
        );
    }

    /**
     * Performs a nested JO lookup for the specified path to see if it exists.
     *
     * @param inputJsonObject The JsonObject to check.
     * @param keys            The key path to follow.
     * @return True if the path specified by {@code keys} exists, or false otherwise.
     */
    public static boolean nestedHas(JsonObject inputJsonObject, List<String> keys) {
        JsonObject last = inputJsonObject;

        try {
            for (int i = 0; i < keys.size() - 1; i++) {
                last = last.getAsJsonObject(keys.get(i));
            }
        } catch (Exception e) {
            return false;
        }

        return last.has(keys.get(keys.size() - 1));
    }

    /**
     * Attempt to get a nested JsonObject inside {@code inputJsonObject}.
     *
     * @param inputJsonObject The parent JsonObject
     * @param keys            The path to follow to access the nested JsonObject.
     * @return The specified JsonObject or null if it could not be found.
     */
    public static JsonObject getNestedJsonObject(JsonObject inputJsonObject, List<String> keys) {
        JsonObject result = inputJsonObject;

        try {
            for (String s : keys) {
                result = result.getAsJsonObject(s);
            }

            return result;
        } catch (Exception e) {
            LOG.error("Error during obtaining nested json object.", e);
            return null;
        }
    }

    /**
     * Attempt to get a nested JsonArray inside {@code inputJsonObject}. This means that the JsonArray is the last element in a set
     * of nested JsonObjects.
     *
     * @param inputJsonObject The parent JsonObject
     * @param keys            The path to follow to access the nested JsonArray.
     * @return The specified JsonArray or null if it could not be found.
     */
    public static JsonArray getNestedJsonArray(JsonObject inputJsonObject, List<String> keys) {
        if (keys.isEmpty()) {
            return new JsonArray();
        } else if (keys.size() == 1) {
            return inputJsonObject.getAsJsonArray(keys.get(0));
        }

        return getNestedJsonObject(inputJsonObject, keys.subList(0, keys.size() - 1)).getAsJsonArray(keys.get(keys.size() - 1));
    }

    /**
     * Get a String from a JsonObject. Returns null if a value for {@code key} was not found.
     *
     * @param inputJsonObject The JsonObject to look for {@code key} in
     * @param key             The key to look for
     * @return The value associated with {@code key} as a String, or null if the {@code key} could not be found.
     */
    public static String getString(JsonObject inputJsonObject, String key) {
        if (!inputJsonObject.has(key)) {
            return null;
        }

        JsonElement e = inputJsonObject.get(key);
        return e.isJsonPrimitive()
                ? e.getAsString()
                : null;
    }

    /**
     * Get a JsonArray of String objects as a List of String objects.
     *
     * @param inputJsonArray The source JsonArray
     * @return The List derived from {@code inputJsonArray}
     */
    public static List<String> convertJsonArrayToList(JsonArray inputJsonArray) {
        return FL.toArrayList(FL.streamFrom(inputJsonArray)
                .map(JsonElement::getAsString)
        );
    }
}