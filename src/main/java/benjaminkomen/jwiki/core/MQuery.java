package benjaminkomen.jwiki.core;

import benjaminkomen.jwiki.core.WQuery.QTemplate;
import benjaminkomen.jwiki.dwrap.ImageInfo;
import benjaminkomen.jwiki.util.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

/**
 * Perform multi-title queries. Use of these methods is intended for
 * <span style="text-decoration:underline;">advanced</span> users who wish to make queries to the server over a large
 * data set. These methods are optimized for performance, and will consolidate titles into single queries to fetch the
 * most data possible per query. If you're looking to make simple, single-item queries, (which is suitable for most
 * users) please use the methods in Wiki.java.
 *
 * @author Fastily
 * @see Wiki
 */
public final class MQuery {
    /**
     * The group {@code prop} query (multiple titles query) maximum
     */
    private static final int GROUP_QUERY_MAX = 50;
    private static final String ERROR_MESSAGE_NULL_INPUT = "null is not an acceptable title to query with";
    private static final String VAR_TITLES = "titles";
    private static final String VAR_TITLE = "title";

    private MQuery() {
        // no-args constructor
    }

    /**
     * Generic page property ({@code prop}) fetching. This implementation fetches *all* available properties. Use this
     * for prop queries that only return one String of interest per nested JsonObject.
     *
     * @param wiki       The Wiki to use
     * @param titles     The titles to query for.
     * @param qut        The query template to use. Set this according to the fetching method being implemented
     * @param pl         Additional custom parameters to apply to each generated WQuery. Optional, set null to disable.
     * @param elemArrKey The key for each JsonArray for each title the resulting set
     * @return A Map where the key is the title of the page, and the value is the List of properties fetched.
     */
    private static MultiMap<String, JsonObject> getContProp(Wiki wiki, Collection<String> titles, QTemplate qut,
                                                            Map<String, String> pl, String elemArrKey) {
        MultiMap<String, JsonObject> l = new MultiMap<>();

        if (FL.containsNull(titles)) {
            throw new IllegalArgumentException(ERROR_MESSAGE_NULL_INPUT);
        }

        GroupQueue<String> gq = new GroupQueue<>(titles, GROUP_QUERY_MAX);

        while (gq.has()) {
            WQuery wq = new WQuery(wiki, qut).set(VAR_TITLES, gq.poll());
            if (pl != null) {
                pl.forEach(wq::set);
            }

            while (wq.has()) {
                wq.next().propComp(VAR_TITLE, elemArrKey).forEach((k, v) -> {
                    l.touch(k);
                    if (v != null) {
                        l.put(k, GSONP.getJsonArrayofJsonObject(v.getAsJsonArray()));
                    }
                });
            }
        }
        return l;
    }

    /**
     * Performs a non-continuing {@code prop} query. Grabs a title and an element from each returned page.
     *
     * @param wiki   The Wiki to query.
     * @param titles The titles to use
     * @param qut    The QTemplate to use
     * @param pl     Additional parameters to pass to each created WQuery, set null to disable.
     * @param eKey   The value key to get from each page element. If this cannot be found, then it is set to null.
     * @return The {@code title} of each page as the key, and the value of the associated {@code eKey}.
     */
    private static Map<String, JsonElement> getNoContProp(Wiki wiki, Collection<String> titles, QTemplate qut,
                                                          Map<String, String> pl, String eKey) {
        Map<String, JsonElement> m = new HashMap<>();

        if (FL.containsNull(titles)) {
            throw new IllegalArgumentException(ERROR_MESSAGE_NULL_INPUT);
        }

        GroupQueue<String> gq = new GroupQueue<>(titles, GROUP_QUERY_MAX);
        while (gq.has()) {
            WQuery wq = new WQuery(wiki, qut).set(VAR_TITLES, gq.poll());
            if (pl != null) {
                pl.forEach(wq::set);
            }

            m.putAll(wq.next().propComp(VAR_TITLE, eKey));
        }
        return m;
    }

    /**
     * Performs a non-continuing {@code list} query. Grabs JsonObjects from the JsonArray in the server Response.
     *
     * @param wiki   The Wiki to query
     * @param titles The titles to use
     * @param qut    The QTemplate to use
     * @param pl     Additional parameters to pass to each created WQuery, set null to disable.
     * @param tQKey  The variable name to use for each set of 50 {@code titles} in the url passed to the server.
     * @param aKey   The key pointing to the JsonArray of JsonObject in the server's Response.
     * @return An ArrayList of JsonObject collected from the server Response(s).
     */
    private static List<JsonObject> getNoContList(Wiki wiki, Collection<String> titles, QTemplate qut, Map<String, String> pl,
                                                  String tQKey, String aKey) {
        List<JsonObject> l = new ArrayList<>();

        if (FL.containsNull(titles)) {
            throw new IllegalArgumentException(ERROR_MESSAGE_NULL_INPUT);
        }

        GroupQueue<String> gq = new GroupQueue<>(titles, GROUP_QUERY_MAX);
        while (gq.has()) {
            WQuery wq = new WQuery(wiki, qut).set(tQKey, gq.poll());
            if (pl != null) {
                pl.forEach(wq::set);
            }

            l.addAll(wq.next().listComp(aKey));
        }

        return l;
    }

    /**
     * Retrieve one String value from each JsonObject ArrayList for each pair in a MapList.
     *
     * @param m       The MapList to work with
     * @param elemKey The key pointing to String to get in each JsonObject.
     * @return Each title, and the values that were found for it.
     */
    private static Map<String, List<String>> parsePropToSingle(MultiMap<String, JsonObject> m, String elemKey) {
        Map<String, List<String>> xl = new HashMap<>();
        m.getBackingMap().forEach((k, v) -> xl.put(k, FL.toArrayList(v.stream().map(e -> GSONP.getString(e, elemKey)))));

        return xl;
    }

    /**
     * Retrieve one String value from each JsonObject ArrayList for each pair in a MapList. Assumes that the key to
     * select from each JsonObject is {@code title}.
     *
     * @param m The MapList to work with
     * @return Each title, and the values that were found for it.
     */
    private static Map<String, List<String>> parsePropToSingle(MultiMap<String, JsonObject> m) {
        return parsePropToSingle(m, VAR_TITLE);
    }

    /**
     * Retrieve two String value from each JsonObject ArrayList for each pair in a MapList.
     *
     * @param m        The MapList to work with
     * @param elemKey1 The key pointing to the first String to get in each JsonObject.
     * @param elemKey2 The key pointing to the second String to get in each JsonObject.
     * @return Each title, and the values that were found for it.
     */
    private static Map<String, List<Tuple<String, String>>> parsePropToDouble(MultiMap<String, JsonObject> m, String elemKey1,
                                                                              String elemKey2) {
        Map<String, List<Tuple<String, String>>> xl = new HashMap<>();
        m.getBackingMap().forEach(
                (k, v) -> xl.put(k, FL.toArrayList(v.stream().map(e -> new Tuple<>(GSONP.getString(e, elemKey1), GSONP.getString(e, elemKey2))))));

        return xl;
    }

    /**
     * Gets the list of usergroups (rights) users belong to. Sample groups: sysop, user, autoconfirmed, editor.
     *
     * @param wiki  The Wiki object to use.
     * @param users Users to get rights information for. Do not include {@code User:} prefix.
     * @return A Map such that the key is the user and the value a List of the user's rights (or null if the user does not exist)
     */
    public static Map<String, List<String>> listUserRights(Wiki wiki, Collection<String> users) {
        Map<String, List<String>> l = new HashMap<>();
        getNoContList(wiki, users, WQuery.USERRIGHTS, null, "ususers", "users")
                .forEach(jo -> l.put(GSONP.getString(jo, "name"), jo.has("groups") ? GSONP.convertJsonArrayToList(jo.getAsJsonArray("groups")) : null));

        return l;
    }

    /**
     * Gets ImageInfo objects for each revision of a File.
     *
     * @param wiki   The Wiki object to use
     * @param titles The titles to query
     * @return A map with titles keyed to respective lists of ImageInfo.
     */
    public static Map<String, List<ImageInfo>> getImageInfo(Wiki wiki, Collection<String> titles) {
        Map<String, List<ImageInfo>> l = new HashMap<>();
        getContProp(wiki, titles, WQuery.IMAGEINFO, null, "imageinfo").getBackingMap()
                .forEach((k, v) -> l.put(k, FL.toArrayList(v.stream().map(jo -> GSONP.getGson().fromJson(jo, ImageInfo.class)))));

        // MediaWiki imageinfo is not a well-behaved module
        l.forEach((k, v) -> v.sort(ImageInfo.comparator()));

        return l;
    }

    /**
     * Gets the list of categories on a page.
     *
     * @param wiki   The wiki object to use
     * @param titles The titles to query.
     * @return A list of results keyed by title.
     */
    public static Map<String, List<String>> getCategoriesOnPage(Wiki wiki, Collection<String> titles) {
        return parsePropToSingle(getContProp(wiki, titles, WQuery.PAGECATEGORIES, null, "categories"));
    }

    /**
     * Gets the number of elements contained in a category.
     *
     * @param wiki   The wiki object to use
     * @param titles The titles to query. PRECONDITION: Titles *must* begin with the "Category:" prefix
     * @return A list of results keyed by title. Value returned will be -1 if Category entered was empty <b>and</b>
     * non-existent.
     */
    public static Map<String, Integer> getCategorySize(Wiki wiki, Collection<String> titles) {
        Map<String, Integer> l = new HashMap<>();
        getNoContProp(wiki, titles, WQuery.CATEGORYINFO, null, "categoryinfo")
                .forEach((k, v) -> l.put(k, v == null ? 0 : v.getAsJsonObject().get("size").getAsInt()));
        return l;
    }

    /**
     * Gets the text of a page.
     *
     * @param wiki   The wiki to use
     * @param titles The titles to query
     * @return A list of results keyed by title.
     */
    public static Map<String, String> getPageText(Wiki wiki, Collection<String> titles) {
        Map<String, String> l = new HashMap<>();
        getNoContProp(wiki, titles, WQuery.PAGETEXT, null, "revisions").forEach((k, v) -> {
            if (v == null) {
                l.put(k, "");
            } else {
                List<JsonObject> jl = GSONP.getJsonArrayofJsonObject(v.getAsJsonArray());
                l.put(k, jl == null || jl.isEmpty() ? "" : GSONP.getString(jl.get(0), "*"));
            }
        });

        return l;
    }

    /**
     * Get wiki links on a page.
     *
     * @param wiki   The wiki object to use
     * @param titles The titles to query.
     * @param ns     Namespaces to include-only. Optional param: leave blank to disable.
     * @return A list of results keyed by title.
     */
    public static Map<String, List<String>> getLinksOnPage(Wiki wiki, Collection<String> titles, NS... ns) {
        Map<String, String> pl = new HashMap<>();
        if (ns != null && ns.length > 0) {
            pl.put("plnamespace", wiki.getNamespaceManager().createFilter(ns));
        }

        return parsePropToSingle(getContProp(wiki, titles, WQuery.LINKSONPAGE, pl, "links"));
    }

    /**
     * Get pages redirecting to or linking to a page.
     *
     * @param wiki      The wiki object to use
     * @param redirects Set to true to search for redirects. False searches for non-redirects.
     * @param titles    The titles to query
     * @return A list of results keyed by title.
     */
    public static Map<String, List<String>> linksHere(Wiki wiki, boolean redirects, Collection<String> titles) {
        return parsePropToSingle(
                getContProp(wiki, titles, WQuery.LINKSHERE, FL.produceMap("lhshow", (redirects ? "" : "!") + "redirect"), "linkshere"));
    }

    /**
     * Gets a list of pages transcluding a template.
     *
     * @param wiki   The wiki object to use
     * @param titles The titles to query
     * @param ns     Only return results from this/these namespace(s). Optional param: leave blank to disable.
     * @return A list of results keyed by title.
     */
    public static Map<String, List<String>> transcludesIn(Wiki wiki, Collection<String> titles, NS... ns) {
        Map<String, String> pl = new HashMap<>();
        if (ns.length > 0) {
            pl.put("tinamespace", wiki.getNamespaceManager().createFilter(ns));
        }

        return parsePropToSingle(getContProp(wiki, titles, WQuery.TRANSCLUDEDIN, pl, "transcludedin"));
    }

    /**
     * Gets a list of pages linking (displaying/thumbnailing) a file.
     *
     * @param wiki   The wiki to use
     * @param titles The titles to query. PRECONDITION: These must be valid file names prefixed with the "File:" prefix.
     * @return A Map of results keyed by title.
     */
    public static Map<String, List<String>> fileUsage(Wiki wiki, Collection<String> titles) {
        return parsePropToSingle(getContProp(wiki, titles, WQuery.FILEUSAGE, null, "fileusage"));
    }

    /**
     * Gets a list of external (non-interwiki) links on the specified titles.
     *
     * @param wiki   The Wiki object to use
     * @param titles The titles to query
     * @return A Map of results keyed by title.
     */
    public static Map<String, List<String>> getExternalLinks(Wiki wiki, Collection<String> titles) {
        return parsePropToSingle(getContProp(wiki, titles, WQuery.EXTLINKS, null, "extlinks"), "*");
    }

    /**
     * Checks if list of titles exists.
     * FIXME getNoContProp - doubled output on non-normal titles
     *
     * @param wiki   The wiki object to use
     * @param titles The titles to query.
     * @return Results keyed by title. {@code true} means the title exists.
     */
    public static Map<String, Boolean> exists(Wiki wiki, Collection<String> titles) {
        Map<String, Boolean> l = new HashMap<>();
        getNoContProp(wiki, titles, WQuery.EXISTS, null, "missing").forEach((k, v) -> l.put(k, v == null));
        return l;
    }

    /**
     * Checks if a title exists. Can filter results based on whether pages exist.
     *
     * @param wiki   The wiki object to use
     * @param exists Set to true to select all pages that exist. False selects all that don't exist
     * @param titles The titles to query.
     * @return A list of titles that exist or don't exist.
     */
    public static List<String> exists(Wiki wiki, boolean exists, Collection<String> titles) {
        List<String> l = new ArrayList<>();
        exists(wiki, titles).forEach((k, v) -> {
            if (v == exists) {
                l.add(k);
            }
        });

        return l;
    }

    /**
     * Gets titles of images linked on a page.
     *
     * @param wiki   The wiki object to use
     * @param titles The titles to query
     * @return A list of results keyed by title.
     */
    public static Map<String, List<String>> getImagesOnPage(Wiki wiki, Collection<String> titles) {
        return parsePropToSingle(getContProp(wiki, titles, WQuery.IMAGES, null, "images"));
    }

    /**
     * Get templates transcluded on a page.
     *
     * @param wiki   The wiki object to use
     * @param titles The titles to query
     * @return A list of results keyed by title.
     */
    public static Map<String, List<String>> getTemplatesOnPage(Wiki wiki, Collection<String> titles) {
        return parsePropToSingle(getContProp(wiki, titles, WQuery.TEMPLATES, null, "templates"));
    }

    /**
     * Gets the global usage of a file.
     *
     * @param wiki   The wiki object to use
     * @param titles The titles to query
     * @return A list of results keyed by title. The inner tuple is of the form (title, shorthand url notation).
     */
    public static Map<String, List<Tuple<String, String>>> globalUsage(Wiki wiki, Collection<String> titles) {
        return parsePropToDouble(getContProp(wiki, titles, WQuery.GLOBALUSAGE, null, "globalusage"), VAR_TITLE, "wiki");
    }

    /**
     * Resolves title redirects on a Wiki.
     *
     * @param wiki   The Wiki to run the query against
     * @param titles The titles to attempt resolving.
     * @return A HashMap where each key is the original title, and the value is the resolved title.
     */
    public static Map<String, String> resolveRedirects(Wiki wiki, Collection<String> titles) {
        Map<String, String> l = new HashMap<>();
        for (String s : titles) {
            l.put(s, s);
        }

        getNoContList(wiki, titles, WQuery.RESOLVEREDIRECT, null, VAR_TITLES, "redirects")
                .forEach(jo -> l.put(GSONP.getString(jo, "from"), GSONP.getString(jo, "to")));

        return l;
    }

    /**
     * Gets duplicates of a file.
     *
     * @param wiki      The wiki object to use
     * @param localOnly Set to true if you only want to look for files in the local repository.
     * @param titles    The titles to query
     * @return A list of results keyed by title.
     */
    public static Map<String, List<String>> getDuplicatesOf(Wiki wiki, boolean localOnly, Collection<String> titles) {
        Map<String, String> pl = new HashMap<>();
        if (localOnly) {
            pl.put("dflocalonly", "");
        }

        Map<String, List<String>> l = parsePropToSingle(getContProp(wiki, titles, WQuery.DUPLICATEFILES, pl, "duplicatefiles"),
                "name");
        l.forEach((k, v) -> v.replaceAll(s -> wiki.convertIfNotInNS(s.replace('_', ' '), NS.FILE)));

        return l;
    }

    /**
     * Gets shared (non-local) duplicates of a file. PRECONDITION: The Wiki this query is run against has the
     * <a href="https://www.mediawiki.org/wiki/Extension:GlobalUsage">GlobalUsage</a> extension installed.
     *
     * @param wiki   The wiki object to use
     * @param titles The titles to query
     * @return A list of results keyed by title.
     */
    public static Map<String, List<String>> getSharedDuplicatesOf(Wiki wiki, Collection<String> titles) {
        Map<String, List<Tuple<String, String>>> xl = parsePropToDouble(
                getContProp(wiki, titles, WQuery.DUPLICATEFILES, null, "duplicatefiles"), "name", "shared");

        Map<String, List<String>> l = new HashMap<>();
        xl.forEach((k, v) -> l.put(k,
                FL.toArrayList(v.stream()
                        .filter(t -> t.getValue2() != null)
                        .map(t -> wiki.convertIfNotInNS(t.getValue1().replace('_', ' '), NS.FILE))
                )
        ));
        return l;
    }

    /**
     * Gets a text extract (the lead paragraph) of a page.
     *
     * @param wiki   The Wiki object to use.
     * @param titles The titles to get a text extract for.
     * @return A Map of results keyed by title.  A null mapping means that the page doesn't exist or is not eligble for text extract.
     */
    public static Map<String, String> getTextExtracts(Wiki wiki, Collection<String> titles) {
        Map<String, String> l = new HashMap<>();
        getNoContProp(wiki, titles, WQuery.TEXTEXTRACTS, null, "extract").forEach((k, v) -> l.put(k, v == null ? null : v.getAsString()));

        return l;
    }
}