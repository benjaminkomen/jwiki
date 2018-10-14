package benjaminkomen.jwiki.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.HttpUrl;

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
    /**
     * Default json deserializer for Instant objects.
     */
    private static JsonDeserializer<Instant> instantDeserializer = (j, t, c) -> Instant.parse(j.getAsJsonPrimitive().getAsString());

    /**
     * Default json deserializer for HttpUrl objects.
     */
    private static JsonDeserializer<HttpUrl> httpurlDeserializer = (j, t, c) -> HttpUrl.parse(j.getAsJsonPrimitive().getAsString());

    /**
     * Static JsonParser, for convenience.
     */
    public static final JsonParser jp = new JsonParser();

    /**
     * Default Gson object, for convenience.
     */
    public static final Gson gson = new GsonBuilder().registerTypeAdapter(Instant.class, instantDeserializer)
            .registerTypeAdapter(HttpUrl.class, httpurlDeserializer).create();

    /**
     * Gson object which generates pretty-print (human-readable) JSON.
     */
    public static final Gson gsonPP = new GsonBuilder().setPrettyPrinting().create();

    private GSONP() {
        // no-args constructor
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
    public static List<JsonObject> getJAofJO(JsonArray input) {
        try {
            return FL.toArrayList(FL.streamFrom(input).map(JsonElement::getAsJsonObject));
        } catch (Exception e) {
            e.printStackTrace();
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
    public static List<JsonObject> getJAofJO(JsonObject input, String key) {
        JsonArray ja = input.getAsJsonArray(key);
        return ja != null
                ? getJAofJO(input.getAsJsonArray(key))
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
                .collect(Collectors.toMap(e -> getStr(e, kk), e -> getStr(e, vk)))
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
        JsonObject jo = inputJsonObject;

        try {
            for (String s : keys) {
                jo = jo.getAsJsonObject(s);
            }

            return jo;
        } catch (Exception e) {
            e.printStackTrace();
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
    public static String getStr(JsonObject inputJsonObject, String key) {
        if (!inputJsonObject.has(key)) {
            return null;
        }

        JsonElement e = inputJsonObject.get(key);
        return e.isJsonPrimitive()
                ? e.getAsString()
                : null;
    }

    /**
     * Get a JsonArray of String objects as an ArrayList of String objects.
     *
     * @param inputJsonArray The source JsonArray
     * @return The ArrayList derived from {@code inputJsonArray}
     */
    public static List<String> jaOfStrToAL(JsonArray inputJsonArray) {
        return FL.toArrayList(FL.streamFrom(inputJsonArray)
                .map(JsonElement::getAsString)
        );
    }
}