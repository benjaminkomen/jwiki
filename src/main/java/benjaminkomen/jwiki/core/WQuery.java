package benjaminkomen.jwiki.core;

import benjaminkomen.jwiki.util.FL;
import benjaminkomen.jwiki.util.GSONP;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Wraps the various functions of API functions of {@code action=query}.
 *
 * @author Fastily
 */
@Getter
class WQuery {

    private static final Logger LOG = LoggerFactory.getLogger(WQuery.class);
    private static final String VAR_CATEGORY_MEMBERS = "categorymembers";
    private static final String VAR_QUERY = "query";
    private static final String VAR_REVISIONS = "revisions";
    private static final String VAR_TITLE = "title";
    private static final String VAR_TITLES = "titles";

    /**
     * Default parameters for getting category size info
     */
    public static final QTemplate ALLOWEDFILEXTS = new QTemplate(FL.produceMap("meta", "siteinfo", "siprop", "fileextensions"),
            "fileextensions");

    /**
     * Default parameters for getting category size info
     */
    public static final QTemplate ALLPAGES = new QTemplate(FL.produceMap("list", "allpages"), "aplimit", "allpages");

    /**
     * Default parameters for getting category size info
     */
    public static final QTemplate CATEGORYINFO = new QTemplate(FL.produceMap("prop", "categoryinfo", VAR_TITLES, null), "categoryinfo");

    /**
     * Default parameters for listing category members
     */
    public static final QTemplate CATEGORYMEMBERS = new QTemplate(FL.produceMap("list", VAR_CATEGORY_MEMBERS, "cmtitle", null), "cmlimit",
            VAR_CATEGORY_MEMBERS);

    /**
     * Default parameters for getting Namespace information on a Wiki.
     */
    public static final QTemplate NAMESPACES = new QTemplate(FL.produceMap("meta", "siteinfo", "siprop", "namespaces|namespacealiases"),
            null);

    /**
     * Default parameters for getting duplicate files
     */
    public static final QTemplate DUPLICATEFILES = new QTemplate(FL.produceMap("prop", "duplicatefiles", VAR_TITLES, null), "dflimit",
            "duplicatefiles");

    /**
     * Default parameters for determining if a page exists.
     */
    public static final QTemplate EXISTS = new QTemplate(FL.produceMap("prop", "pageprops", "ppprop", "missing", VAR_TITLES, null), null);

    /**
     * Default parameters for fetching external links on a page
     */
    public static final QTemplate EXTLINKS = new QTemplate(FL.produceMap("prop", "extlinks", "elexpandurl", "1", VAR_TITLES, null), "ellimit",
            "extlinks");

    /**
     * Default parameters for getting file usage
     */
    public static final QTemplate FILEUSAGE = new QTemplate(FL.produceMap("prop", "fileusage", VAR_TITLES, null), "fulimit", "fileusage");

    /**
     * Default parameters for getting global usage of a file
     */
    public static final QTemplate GLOBALUSAGE = new QTemplate(FL.produceMap("prop", "globalusage", VAR_TITLES, null), "gulimit", "globalusage");

    /**
     * Default parameters for getting files on a page
     */
    public static final QTemplate IMAGES = new QTemplate(FL.produceMap("prop", "images", VAR_TITLES, null), "imlimit", "images");

    /**
     * Default parameters for getting image info of a file.
     */
    public static final QTemplate IMAGEINFO = new QTemplate(
            FL.produceMap("prop", "imageinfo", "iiprop", "canonicaltitle|url|size|sha1|mime|user|timestamp|comment", VAR_TITLES, null), "iilimit",
            "imageinfo");

    /**
     * Default parameters for getting links to a page
     */
    public static final QTemplate LINKSHERE = new QTemplate(
            FL.produceMap("prop", "linkshere", "lhprop", VAR_TITLE, "lhshow", null, VAR_TITLES, null), "lhlimit", "linkshere");

    /**
     * Default parameters for getting links on a page
     */
    public static final QTemplate LINKSONPAGE = new QTemplate(FL.produceMap("prop", "links", VAR_TITLES, null), "pllimit", "links");

    /**
     * Default parameters for listing logs.
     */
    public static final QTemplate LOGEVENTS = new QTemplate(FL.produceMap("list", "logevents"), "lelimit", "logevents");

    /**
     * Default parameters for getting page categories.
     */
    public static final QTemplate PAGECATEGORIES = new QTemplate(FL.produceMap("prop", "categories", VAR_TITLES, null), "cllimit",
            "categories");

    /**
     * Default parameters for getting page text.
     */
    public static final QTemplate PAGETEXT = new QTemplate(FL.produceMap("prop", VAR_REVISIONS, "rvprop", "content", VAR_TITLES, null), null);

    /**
     * Default parameters for listing protected titles.
     */
    public static final QTemplate PROTECTEDTITLES = new QTemplate(
            FL.produceMap("list", "protectedtitles", "ptprop", "timestamp|level|user|comment"), "ptlimit", "protectedtitles");

    /**
     * Default parameters for listing the results of querying Special pages.
     */
    public static final QTemplate QUERYPAGES = new QTemplate(FL.produceMap("list", "querypage", "qppage", null), "qplimit", "querypage");

    /**
     * Default parameters for listing random pages
     */
    public static final QTemplate RANDOM = new QTemplate(FL.produceMap("list", "random", "rnfilterredir", "nonredirects"), "rnlimit",
            "random");

    /**
     * Default parameters for listing recent changes.
     */
    public static final QTemplate RECENTCHANGES = new QTemplate(
            FL.produceMap("list", "recentchanges", "rcprop", "title|timestamp|user|comment", "rctype", "edit|new|log"), "rclimit",
            "recentchanges");

    /**
     * Default parameters for resolving redirects
     */
    public static final QTemplate RESOLVEREDIRECT = new QTemplate(FL.produceMap("redirects", "", VAR_TITLES, null), "redirects");

    /**
     * Default parameters for listing page revisions
     */
    public static final QTemplate REVISIONS = new QTemplate(
            FL.produceMap("prop", VAR_REVISIONS, "rvprop", "timestamp|user|comment|content", VAR_TITLES, null), "rvlimit", VAR_REVISIONS);

    /**
     * Default parameters for listing searches
     */
    public static final QTemplate SEARCH = new QTemplate(FL.produceMap("list", "search", "srprop", "", "srnamespace", "*", "srsearch", null), "srlimit", "search");


    /**
     * Default parameters for getting templates on a page
     */
    public static final QTemplate TEMPLATES = new QTemplate(FL.produceMap("prop", "templates", "tiprop", VAR_TITLE, VAR_TITLES, null), "tllimit",
            "templates");

    /**
     * Default parameters for getting text extracts from a page
     */
    public static final QTemplate TEXTEXTRACTS = new QTemplate(
            FL.produceMap("prop", "extracts", "exintro", "1", "explaintext", "1", VAR_TITLES, null), "exlimit", "extract");

    /**
     * Default parameters for getting a csrf token.
     */
    public static final QTemplate TOKENS_CSRF = new QTemplate(FL.produceMap("meta", "tokens", "type", "csrf"), null);

    /**
     * Default parameters for getting a login token. Only works with MediaWiki version 1.20.0 or higher.
     */
    public static final QTemplate TOKENS_LOGIN_NEW = new QTemplate(FL.produceMap("meta", "tokens", "type", "login"), null);

    /**
     * Default parameters for getting a login token. Works with MediaWiki version 1.19.0 and older.
     */
    public static final QTemplate TOKENS_LOGIN_LEGACY = new QTemplate(FL.produceMap("prop", "info", "intoken", "edit"), null);

    /**
     * Default parameters for getting a page's transclusions.
     */
    public static final QTemplate TRANSCLUDEDIN = new QTemplate(FL.produceMap("prop", "transcludedin", "tiprop", VAR_TITLE, VAR_TITLES, null),
            "tilimit", "transcludedin");

    /**
     * Default parameters for listing user contributions.
     */
    public static final QTemplate USERCONTRIBS = new QTemplate(FL.produceMap("list", "usercontribs", "ucuser", null), "uclimit",
            "usercontribs");

    /**
     * Default parameters for getting a user's username and id.
     */
    public static final QTemplate USERINFO = new QTemplate(FL.produceMap("meta", "userinfo"), null);

    /**
     * Default parameters for listing users and their rights.
     */
    public static final QTemplate USERRIGHTS = new QTemplate(FL.produceMap("list", "users", "usprop", "groups", "ususers", null), "users");

    /**
     * Default parameters for listing user uploads
     */
    public static final QTemplate USERUPLOADS = new QTemplate(FL.produceMap("list", "allimages", "aisort", "timestamp", "aiuser", null),
            "ailimit", "allimages");

    /**
     * Type describing a HashMap with a String key and String value.
     */
    private static Type strMapT = new TypeToken<Map<String, String>>() {
    }.getType();

    /**
     * The master parameter list. Tracks current query status.
     */
    private final Map<String, String> parameterList = FL.produceMap("action", VAR_QUERY, "format", "json");

    /**
     * The List of limit Strings.
     */
    private final List<String> limitStrings = new ArrayList<>();

    /**
     * The Wiki object to perform queries with
     */
    private final Wiki wiki;

    /**
     * Flag indicating if this query can be continued.
     */
    private boolean canContinue = true;

    /**
     * Tracks and limits entries returned, if applicable.
     */
    private int queryLimit;
    private int totalLimit = -1;
    private int currCount = 0;

    /**
     * Constructor, creates a new WQuery
     *
     * @param wiki The Wiki object to perform queries with
     * @param qut  The QueryUnitTemplate objects to instantiate this WQuery with.
     */
    public WQuery(Wiki wiki, QTemplate... qut) {
        this.wiki = wiki;
        this.queryLimit = wiki.getWikiConfiguration().getMaxResultLimit();

        for (QTemplate qt : qut) {
            parameterList.putAll(qt.defaultFields);
            if (qt.limString != null) {
                limitStrings.add(qt.limString);
            }
        }
    }

    /**
     * Constructor, creates a limited WQuery.
     *
     * @param wiki       The Wiki object to perform queries with.
     * @param totalLimit The maximum number of items to return until WQuery is exhausted. Actual number of items returned
     *                   may be less. Optional, disable with -1.
     * @param qut        The QueryUnitTemplate objects to instantiate this WQuery with.
     */
    public WQuery(Wiki wiki, int totalLimit, QTemplate... qut) {
        this(wiki, qut);
        this.totalLimit = totalLimit;
    }

    /**
     * Test if this WQuery has any queries remaining.
     *
     * @return True if this WQuery can still be used to make continuation queries.
     */
    public boolean has() {
        return canContinue;
    }

    /**
     * Attempts to perform the next query in this sequence.
     *
     * @return A JsonObject with the response from the server, or null if something went wrong.
     */
    public QReply next() {
        // sanity check
        if (parameterList.containsValue(null)) {
            throw new IllegalStateException(String.format("Fill in *all* the null fields -> %s", parameterList));
        } else if (!canContinue) {
            return null;
        }

        JsonObject result;

        try {
            final int increasedCount = currCount += queryLimit;
            if (totalLimit > 0 && increasedCount > totalLimit) {
                adjustLimit(queryLimit - (currCount - totalLimit));
                canContinue = false;
            }

            result = JsonParser.parseString(wiki.getApiclient().basicGET(parameterList).body().string()).getAsJsonObject();
            if (result.has("continue")) {
                parameterList.putAll(GSONP.getGson().fromJson(result.getAsJsonObject("continue"), strMapT));
            } else if (result.has("query-continue")) {
                parameterList.putAll(GSONP.getGson().fromJson(result.getAsJsonObject("query-continue").getAsJsonObject(VAR_CATEGORY_MEMBERS), strMapT));
            } else {
                canContinue = false;
            }

            if (wiki.getWikiConfiguration().isDebug()) {
                wiki.getWikiConfiguration().getLog().debug(wiki, GSONP.getGsonPrettyPrint().toJson(result));
            }

            return new QReply(result);
        } catch (Exception e) {
            LOG.error("Error during performing next query", e);
            return null;
        }
    }

    /**
     * Sets a key-value pair. DO NOT URL-encode. These are the parameters that will be passed to the MediaWiki API.
     *
     * @param key   The parameter key to set
     * @param value The parameter value to set
     * @return This WQuery. Useful for chaining.
     */
    public WQuery set(String key, String value) {
        parameterList.put(key, value);
        return this;
    }

    /**
     * Sets a key-values pair. DO NOT URL-encode. These are the parameters that will be passed to the MediaWiki API.
     *
     * @param key    The parameter key to set
     * @param values The parameter value to set; these will be pipe-fenced.
     * @return This WQuery. Useful for chaining.
     */
    public WQuery set(String key, List<String> values) {
        return set(key, FL.pipeFence(values));
    }

    /**
     * Configure this WQuery to fetch a maximum of {@code limit} items per query. Does nothing if this query does not use
     * limit Strings.
     *
     * @param limit The new limit. Set as -1 to get the maximum number of items per query.
     * @return This WQuery, for chaining convenience.
     */
    public WQuery adjustLimit(int limit) {
        String limitString;
        if (limit <= 0 || limit > wiki.getWikiConfiguration().getMaxResultLimit()) {
            limitString = "max";
            queryLimit = wiki.getWikiConfiguration().getMaxResultLimit();
        } else {
            limitString = "" + limit;
            queryLimit = limit;
        }

        for (String s : limitStrings) {
            parameterList.put(s, limitString);
        }

        return this;
    }

    /**
     * Stores parameter definition rules for a given query and can use these rules to generate a QueryUnit.
     *
     * @author Fastily
     */
    @Getter
    protected static class QTemplate {
        /**
         * The default fields for this query type
         */
        private final Map<String, String> defaultFields;

        /**
         * Optional limit parameter. Will be null if not applicable in this definition.
         */
        private final String limString;

        /**
         * An id which can be used to lookup a query result (in JSON) for a query created from this Object.
         */
        private final String id;

        /**
         * Constructor, creates a new QueryUnitTemplate
         *
         * @param defaultFields The default parameters for the query described by this QueryUnitTemplate.
         * @param id            The id to use to lookup a query result for queries created with this Object.
         */
        public QTemplate(Map<String, String> defaultFields, String id) {
            this(defaultFields, null, id);
        }

        /**
         * Constructor, creates a new QueryUnitTemplate with a limit String.
         *
         * @param defaultFields The default parameters for the query described by this QueryUnitTemplate.
         * @param limString     The limit String parameter. Optional, set null to disable.
         * @param id            The id to use to lookup a query result for queries created with this Object.
         */
        public QTemplate(Map<String, String> defaultFields, String limString, String id) {
            this.defaultFields = defaultFields;
            this.id = id;

            this.limString = limString;
            if (limString != null) {
                defaultFields.put(limString, "max");
            }
        }
    }

    /**
     * A Response from the server for query modules. Contains pre-defined comprehension methods for convenience.
     *
     * @author Fastily
     */
    @Getter
    protected static class QReply {
        /**
         * Default path to json for {@code prop} queries.
         */
        private static final List<String> defaultPropPTJ = FL.toStringArrayList(VAR_QUERY, "pages");

        /**
         * Tracks {@code normalized} titles. The key is the {@code from} (non-normalized) title and the value is the
         * {@code to} (normalized) title.
         */
        private Map<String, String> normalized = null;

        /**
         * The JsonObject which was passed as input
         */
        private final JsonObject input;

        /**
         * Creates a new QReply. Will parse the {@code normalized} JsonArray if it is found in {@code input}.
         *
         * @param input The Response received from the server.
         */
        private QReply(JsonObject input) {
            this.input = input;

            if (GSONP.nestedHas(input, FL.toStringArrayList(VAR_QUERY, "normalized"))) {
                normalized = GSONP.pairOff(GSONP.getJsonArrayofJsonObject(GSONP.getNestedJsonArray(input, FL.toStringArrayList(VAR_QUERY, "normalized"))), "from", "to");
            }
        }

        /**
         * Performs simple {@code list} query Response comprehension. Collects listed JsonObject items in a List.
         *
         * @param k Points to the JsonArray of JsonObject, under {@code query}, of interest.
         * @return A lightly processed List of {@code list} data.
         */
        protected List<JsonObject> listComp(String k) {
            return input.has(VAR_QUERY)
                    ? GSONP.getJsonArrayofJsonObject(input.getAsJsonObject(VAR_QUERY), k)
                    : Collections.emptyList();
        }

        /**
         * Performs simple {@code prop} query Response comprehension. Collects two values from each returned {@code prop}
         * query item in a HashMap. Title normalization is automatically applied.
         *
         * @param kk Points to the String to set as the HashMap key in each {@code prop} query item.
         * @param vk Points to the JsonElement to set as the HashMap value in each {@code prop} query item.
         * @return A lightly processed HashMap of {@code prop} data.
         */
        protected Map<String, JsonElement> propComp(String kk, String vk) {
            Map<String, JsonElement> m = new HashMap<>();

            JsonObject x = GSONP.getNestedJsonObject(input, defaultPropPTJ);
            if (x == null) {
                return m;
            }

            for (JsonObject jo : GSONP.convertJsonObjectToList(x)) {
                m.put(GSONP.getString(jo, kk), jo.get(vk));
            }

            return normalize(m);
        }

        /**
         * Performs simple {@code meta} query Response comprehension.
         *
         * @param key The key to get a JsonElement for.
         * @return The JsonElement pointed to by {@code key} or null/empty JsonObject on error.
         */
        protected JsonElement metaComp(String key) {
            return input.has(VAR_QUERY)
                    ? input.getAsJsonObject(VAR_QUERY).get(key)
                    : new JsonObject();
        }

        /**
         * Performs title normalization when it is automatically done by MediaWiki. MediaWiki will return a
         * {@code normalized} JsonArray when it fixes lightly malformed titles. This is intended for use with {@code prop}
         * style queries.
         *
         * @param <V> Any Object.
         * @param m   The Map of elements to normalize.
         * @return {@code m}, for chaining convenience.
         */
        protected <V> Map<String, V> normalize(Map<String, V> m) {
            if (normalized != null) {
                normalized.forEach((f, t) -> {
                    if (m.containsKey(t))
                        m.put(f, m.get(t));
                });
            }

            return m;
        }
    }
}