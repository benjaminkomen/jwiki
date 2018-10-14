package benjaminkomen.jwiki.core;

import benjaminkomen.jwiki.util.FL;
import benjaminkomen.jwiki.util.GSONP;
import com.google.gson.JsonObject;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains default namespaces and methods to get non-standard namespaces.
 *
 * @author Fastily
 */
@Getter
public final class NS {
    /**
     * Main namespace
     */
    public static final NS MAIN = new NS(0);

    /**
     * Talk namespace for main
     */
    public static final NS TALK = new NS(1);
    /**
     * User namespace
     */
    public static final NS USER = new NS(2);

    /**
     * User talk namespace
     */
    public static final NS USER_TALK = new NS(3);

    /**
     * Project namespace
     */
    public static final NS PROJECT = new NS(4);

    /**
     * Project talk namespace
     */
    public static final NS PROJECT_TALK = new NS(5);

    /**
     * File namespace
     */
    public static final NS FILE = new NS(6);

    /**
     * File talk namespace
     */
    public static final NS FILE_TALK = new NS(7);

    /**
     * MediaWiki namespace
     */
    public static final NS MEDIAWIKI = new NS(8);

    /**
     * Media talk namespace
     */
    public static final NS MEDIA_TALK = new NS(9);

    /**
     * Template namespace
     */
    public static final NS TEMPLATE = new NS(10);

    /**
     * Template talk namespace
     */
    public static final NS TEMPLATE_TALK = new NS(11);

    /**
     * Help namespace
     */
    public static final NS HELP = new NS(12);

    /**
     * Help talk namespace
     */
    public static final NS HELP_TALK = new NS(13);

    /**
     * Category namespace
     */
    public static final NS CATEGORY = new NS(14);

    /**
     * Category talk namespace.
     */
    public static final NS CATEGORY_TALK = new NS(15);

    /**
     * This NS's value.
     */
    private final int value;

    /**
     * Constructor
     *
     * @param value The namespace value to initialize the NS with.
     */
    protected NS(int value) {
        this.value = value;
    }

    /**
     * Gets a hash code for this object. This is simply the namespace number.
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Determines if two namespaces are the same namespace
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof NS && value == ((NS) other).value;
    }

    /**
     * A namespace manager object. One for each Wiki object.
     *
     * @author Fastily
     */
    @Getter
    protected static class NSManager {
        /**
         * The Map of all valid namespace-number pairs.
         */
        private final Map<Object, Object> validNamespacesAndNumbers = new HashMap<>();

        /**
         * The List of valid namespaces as Strings.
         */
        private final List<String> validNamespaces = new ArrayList<>();

        /**
         * Regex used to strip the namespace from a title.
         */
        private final String nssRegex;

        /**
         * Pattern version of <code>nssRegex</code>
         */
        private final Pattern pattern;

        /**
         * Constructor, takes a Reply with Namespace data.
         *
         * @param reply A Reply object with a <code>namespaces</code> JSONObject.
         */
        protected NSManager(JsonObject reply) {
            for (JsonObject x : GSONP.convertJsonObjectToList(reply.getAsJsonObject("namespaces"))) {
                String name = x.get("*").getAsString();
                if (name.isEmpty()) {
                    name = "Main";
                }

                int id = x.get("id").getAsInt();
                validNamespacesAndNumbers.put(name, id);
                validNamespacesAndNumbers.put(id, name);

                validNamespaces.add(name);
            }

            for (JsonObject x : GSONP.getJsonArrayofJsonObject(reply.getAsJsonArray("namespacealiases"))) {
                String name = x.get("*").getAsString();
                validNamespacesAndNumbers.put(name, x.get("id").getAsInt());
                validNamespaces.add(name);
            }

            nssRegex = String.format("(?i)^(%s):", FL.pipeFence(FL.toArrayList(validNamespaces.stream().map(s -> s.replace(" ", "(_| )")))));
            pattern = Pattern.compile(nssRegex);
        }

        /**
         * Generates a filter for use with params passed to API. This DOES NOT URLEncode.
         *
         * @param namespaces The namespaces to select.
         * @return The raw filter string.
         */
        protected String createFilter(NS... namespaces) {
            return FL.pipeFence(FL.toSet(Stream.of(namespaces).map(e -> "" + e.value)));
        }
    }
}