package benjaminkomen.jwiki.core;

import benjaminkomen.jwiki.dwrap.Contrib;
import benjaminkomen.jwiki.dwrap.ImageInfo;
import benjaminkomen.jwiki.dwrap.LogEntry;
import benjaminkomen.jwiki.dwrap.PageSection;
import benjaminkomen.jwiki.dwrap.ProtectedTitleEntry;
import benjaminkomen.jwiki.dwrap.RecentChangesEntry;
import benjaminkomen.jwiki.dwrap.Revision;
import benjaminkomen.jwiki.util.FL;
import benjaminkomen.jwiki.util.GSONP;
import benjaminkomen.jwiki.util.Tuple;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Main entry point of jwiki. This class aggregates most of the queries/actions which jwiki can perform on a wiki. All
 * methods are backed by static functions and are therefore thread-safe.
 *
 * @author Fastily
 */
@Getter
public class Wiki {

    private static final Logger LOG = LoggerFactory.getLogger(Wiki.class);
    private static final String LOGIN = "login";
    private static final String LGTOKEN = "lgtoken";

    /**
     * Our list of currently logged in Wiki's associated with this object. Useful for global operations.
     */
    private Map<String, Wiki> wikis = new HashMap<>();

    /**
     * Our namespace manager
     */
    private final NS.NSManager namespaceManager;

    /**
     * Default configuration and settings for this Wiki.
     */
    private Conf wikiConfiguration;

    /**
     * Used to make calls to and from the API.
     */
    private final ApiClient apiclient;
    private static final String VAR_TITLE = "title";
    private static final String VAR_NEWER = "newer";

    /**
     * Builder used to create Wiki objects. All options are optional. If you're lazy and just want an anonymous Wiki
     * pointing to en.wikipedia.org, use {@code new Wiki.Builder().build()}
     *
     * @author Fastily
     */
    public static class Builder {
        /**
         * The Proxy to use
         */
        private Proxy proxy;

        /**
         * The api endpoint to use
         */
        private HttpUrl apiEndpoint;

        /**
         * Flag indicating whether to enable logging
         */
        private boolean enableLogging = true;

        /**
         * The User-Agent to header to use for API requests.
         */
        private String userAgent;

        /**
         * Username to login as.
         */
        private String username;

        /**
         * Password to login with.
         */
        private String password;

        /**
         * Creates a new Wiki Builder.
         */
        public Builder() {
            // Intentionally empty
        }

        /**
         * Configures the Wiki to be created to use the specified User-Agent for HTTP requests.
         *
         * @param userAgent The User-Agent to use
         * @return This Builder
         */
        public Builder withUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Configures the Wiki to be created with the specified Proxy.
         *
         * @param proxy The Proxy to use
         * @return This Builder
         */
        public Builder withProxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * Configures the Wiki to be created with the specified api endpoint. This is the base endpoint of the MediaWiki
         * instance you are targeting. Example: <a href="https://en.wikipedia.org/w/api.php">Wikipedia API</a>.
         *
         * @param apiEndpoint The base api endpoint to target
         * @return This Builder
         */
        public Builder withApiEndpoint(HttpUrl apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
            return this;
        }

        /**
         * Configures the Wiki to be created with the specified domain. This method assumes that the target API endpoint
         * is located at {@code https://<YOUR_DOMAIN_HERE>/w/api.php}; if this is not the case, then use
         * {@link #withApiEndpoint(HttpUrl)}
         *
         * @param domain The domain to target. Example: {@code en.wikipedia.org}.
         * @return This Builder
         */
        public Builder withDomain(String domain) {
            return withApiEndpoint(HttpUrl.parse(String.format("https://%s/w/api.php", domain)));
        }

        /**
         * Configures the Wiki to use the default jwiki logger. This is enabled by default.
         *
         * @param enableLogging Set false to disable jwiki's built-in logging.
         * @return This Builder
         */
        public Builder withDefaultLogger(boolean enableLogging) {
            this.enableLogging = enableLogging;
            return this;
        }

        /**
         * Configures the Wiki to be created with the specified username and password combination. Login will be attempted
         * when {@link #build()} is called.
         *
         * @param username The username to use
         * @param password The password to use
         * @return This Builder
         */
        public Builder withLogin(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        /**
         * Performs the task of creating the Wiki object as configured. If {@link #withApiEndpoint(HttpUrl)} or
         * {@link #withDomain(String)} were not called, then the resulting Wiki will default to the
         * <a href="https://en.wikipedia.org/w/api.php">Wikipedia API</a>.
         *
         * @return A Wiki object
         */
        public Wiki build() {
            if (apiEndpoint == null) {
                withDomain("en.wikipedia.org");
            }

            Wiki wiki = new Wiki(username, password, apiEndpoint, proxy, null, enableLogging);

            // apply post-create settings
            if (userAgent != null) {
                wiki.getWikiConfiguration().setUserAgent(userAgent);
            }

            return wiki;
        }
    }

    /**
     * Constructor, configures all possible params. If the username and password are set but not valid then a
     * SecurityException will be thrown.
     *
     * @param user          The username to use. Optional - set null to disable.
     * @param password      The password to login with. Optional - depends on user not being null, set null to disable.
     * @param baseURL       The URL pointing to the target MediaWiki API endpoint.
     * @param proxy         The Proxy to use. Optional - set null to disable.
     * @param parent        The parent Wiki which spawned this Wiki using {@code getWiki()}. If this is the first Wiki, disable
     *                      with null.
     * @param enableLogging Set true to enable std err log messages. Set false to disable std err log messages.
     */
    private Wiki(String user, String password, HttpUrl baseURL, Proxy proxy, Wiki parent, boolean enableLogging) {
        wikiConfiguration = new Conf(baseURL, new ColorLog(enableLogging));

        // CentralAuth login
        if (parent != null) {
            wikis = parent.wikis;
            apiclient = new ApiClient(parent, this);

            refreshLoginStatus();
        } else {
            apiclient = new ApiClient(this, proxy);

            if (user != null && password != null && !login(user, password))
                throw new SecurityException(String.format("Failed to log-in as %s @ %s", wikiConfiguration.getUname(), wikiConfiguration.getHostname()));
        }

        wikiConfiguration.getLog().info(this, "Fetching Namespace List");
        namespaceManager = new NS.NSManager(new WQuery(this, WQuery.NAMESPACES).next().getInput().getAsJsonObject("query"));
    }

    /* //////////////////////////////////////////////////////////////////////////////// */
    /* ///////////////////////////// AUTH FUNCTIONS /////////////////////////////////// */
    /* //////////////////////////////////////////////////////////////////////////////// */

    /**
     * Performs a login with the specified username and password. Does nothing if this Wiki is already logged in as a
     * user.
     * <p>
     * TODO change this method to prevent using synchronized, anti-pattern
     *
     * @param user     The username to use
     * @param password The password to use
     * @return True if the user is now logged in.
     */
    public synchronized boolean login(String user, String password) {
        // do not login more than once
        if (wikiConfiguration.getUname() != null) {
            return true;
        }

        wikiConfiguration.getLog().info(this, "Try login for " + user);
        try {

            final String firstToken = getFirstToken(user, password);
            final String secondToken = getSecondToken(user, password, firstToken);
            final String editToken = getEditToken(secondToken);

            if (editToken != null && !"".equals(editToken)) {
                refreshLoginStatus(editToken);

                wikiConfiguration.getLog().info(this, "Logged in as " + user);
                return true;
            }
        } catch (Exception e) {
            LOG.error("Exception during login", e);
        }

        return false;
    }

    /**
     * Attempt to login by posting to wiki/api.php?action=login&format=json with the lgname and lgpassword in the body as form-data.
     * This will return an object containing: result = NeedToken and token = the_actual_token
     *
     * @return the first token
     */
    private String getFirstToken(String username, String password) {
        final Tuple<WAction.ActionResult, JsonObject> result = WAction.postAction(this, LOGIN, false,
                FL.produceMap("lgname", username, "lgpassword", password));

        if (result.getValue1() == WAction.ActionResult.NOTOKEN) {
            return GSONP.getString(result.getValue2().get(LOGIN).getAsJsonObject(), "token");
        } else {
            return null;
        }
    }

    /**
     * Do another login attempt by posting to wiki/api.php?action=login&format=json with the lgname,
     * lgpassword and the obtained token as lgtoken as form-data.
     * This will return an object containing: result = Succes and lgtoken = _another_ token than the first one.
     *
     * @return the second token, aka lgtoken
     */
    private String getSecondToken(String username, String password, String firstToken) {
        final Tuple<WAction.ActionResult, JsonObject> result = WAction.postAction(this, LOGIN, false,
                FL.produceMap("lgname", username, "lgpassword", password, LGTOKEN, firstToken));

        if (result.getValue1() == WAction.ActionResult.SUCCESS) {
            return GSONP.getString(result.getValue2().get(LOGIN).getAsJsonObject(), LGTOKEN);
        } else {
            return null;
        }
    }

    /**
     * Perform a GET query to obtain the edittoken, needed to perform edits.
     *
     * @param secondToken the lgtoken obtained from earlier login-attempts.
     * @return the edittoken, needed for editing pages.
     */
    private String getEditToken(String secondToken) {
        final WQuery.QTemplate qTemplate = new WQuery.QTemplate(FL.produceMap("prop", "info",
                "intoken", "edit", "titles", "Main Page", LGTOKEN, secondToken), null);
        final JsonObject pages = new WQuery(this, qTemplate).next().metaComp("pages").getAsJsonObject();
        return pages.entrySet().stream()
                .map(p -> p.getValue().getAsJsonObject().get("edittoken").getAsString())
                .findAny()
                .orElse(null);
    }

    /**
     * Refresh the login status of a Wiki. This runs automatically on login or creation of a new CentralAuth'd Wiki.
     *
     * @param editToken The edittoken obtained earlier in the login-process
     */
    public void refreshLoginStatus(String editToken) {
        wikiConfiguration = Conf.builder()
                .debug(wikiConfiguration.isDebug())
                .userAgent(wikiConfiguration.getUserAgent())
                .baseURL(wikiConfiguration.getBaseURL())
                .scptPath(wikiConfiguration.getScptPath())
                .isBot(wikiConfiguration.isBot())
                .hostname(wikiConfiguration.getHostname())
                .maxResultLimit(wikiConfiguration.getMaxResultLimit())
                .uname(GSONP.getString(new WQuery(this, WQuery.USERINFO).next().metaComp("userinfo").getAsJsonObject(), "name"))
                .log(wikiConfiguration.getLog())
                .token(editToken)
                .build();

        wikis.put(wikiConfiguration.getHostname(), this);

        wikiConfiguration.setBot(listUserRights(wikiConfiguration.getUname()).contains("bot"));
    }

    public void refreshLoginStatus() {
        refreshLoginStatus(null);
    }

    /* //////////////////////////////////////////////////////////////////////////////// */
    /* /////////////////////////// UTILITY FUNCTIONS ////////////////////////////////// */
    /* //////////////////////////////////////////////////////////////////////////////// */

    /**
     * Performs a basic GET action on this Wiki. Use this to implement custom or non-standard API calls.
     *
     * @param action The action to perform.
     * @param params Each parameter and its corresponding value. For example, the parameters,
     *               {@code &amp;foo=bar&amp;baz=blah}, should be passed in as {{@code "foo", "bar", "baz", "blah"}}.
     *               URL-encoding will be applied automatically.
     * @return The Response from the server, or null on error.
     */
    public Response basicGET(String action, String... params) {
        Map<String, String> pl = FL.produceMap(params);
        pl.put("action", action);
        pl.put("format", "json");

        try {
            return apiclient.basicGET(pl);
        } catch (Exception e) {
            LOG.error("Exception during basic GET operation", e);
            return null;
        }
    }

    /**
     * Performs a basic POST action on this Wiki. Use this to implement custom or non-standard API calls.
     *
     * @param action The action to perform.
     * @param form   The form data to post. This will be automatically URL-encoded.
     * @return The Response from the server, or null on error.
     */
    public Response basicPOST(String action, Map<String, String> form) {
        form.put("format", "json");

        try {
            return apiclient.basicPOST(FL.produceMap("action", action), form);
        } catch (Exception e) {
            LOG.error("Exception during basic POST operation", e);
            return null;
        }
    }

    /**
     * Check if a title in specified namespace and convert it if it is not.
     *
     * @param title The title to check
     * @param ns    The namespace to convert the title to.
     * @return The same title if it is in {@code ns}, or the converted title.
     */
    public String convertIfNotInNS(String title, NS ns) {
        return whichNS(title).equals(ns)
                ? title
                : String.format("%s:%s", namespaceManager.getValidNamespacesAndNumbers().get(ns.getValue()), nss(title));
    }

    /**
     * Turns logging to std error on/off.
     *
     * @param enabled Set false to disable logging, or true to enable logging.
     */
    public void enableLogging(boolean enabled) {
        wikiConfiguration.getLog().setEnabled(enabled);
    }

    /**
     * Filters pages by namespace. Only pages with a namespace in {@code ns} are selected.
     *
     * @param pages Titles to filter
     * @param ns    Pages in this/these namespace(s) will be returned.
     * @return Titles belonging to a NS in {@code ns}
     */
    public List<String> filterByNS(List<String> pages, NS... ns) {
        Set<NS> l = new HashSet<>(Arrays.asList(ns));
        return FL.toArrayList(pages.stream().filter(s -> l.contains(whichNS(s))));
    }

    /**
     * Takes a Namespace prefix and gets a NS representation of it. PRECONDITION: the prefix must be a valid namespace
     * prefix. WARNING: This method is CASE-SENSITIVE, so be sure to spell and capitalize the prefix <b>exactly</b> as it
     * would appear on-wiki.
     *
     * @param prefix The prefix to use, without the ":".
     * @return An NS representation of the prefix.
     */
    public NS getNS(String prefix) {
        if (prefix.isEmpty() || prefix.equalsIgnoreCase("main")) {
            return NS.MAIN;
        }

        return namespaceManager.getValidNamespacesAndNumbers().containsKey(prefix)
                ? new NS((int) namespaceManager.getValidNamespacesAndNumbers().get(prefix))
                : null;
    }

    /**
     * Gets a Wiki object for this domain. This method is cached. A new Wiki will be created as necessary. PRECONDITION:
     * The <a href="https://www.mediawiki.org/wiki/Extension:CentralAuth">CentralAuth</a> extension is installed on the
     * target MediaWiki farm.
     *
     * @param domain The domain to use
     * @return The Wiki, or null on error.
     */
    public synchronized Wiki getWiki(String domain) {
        if (wikiConfiguration.getUname() == null) {
            return null;
        }

        wikiConfiguration.getLog().fyi(this, String.format("Get Wiki for %s @ %s", whoami(), domain));
        try {
            return wikis.containsKey(domain)
                    ? wikis.get(domain)
                    : new Wiki(null, null, wikiConfiguration.getBaseURL().newBuilder().host(domain).build(), null, this, wikiConfiguration.getLog().isEnabled());
        } catch (Exception e) {
            LOG.error("Exception during obtaining wiki", e);
            return null;
        }
    }

    /**
     * Strip the namespace from a title.
     *
     * @param title The title to strip the namespace from
     * @return The title without a namespace
     */
    public String nss(String title) {
        return title.replaceAll(namespaceManager.getNssRegex(), "");
    }

    /**
     * Strips the namespaces from a Collection of titles.
     *
     * @param l The Collection of titles to strip namespaces from
     * @return A List where each of the titles does not have a namespace.
     */
    public List<String> nss(Collection<String> l) {
        return FL.toArrayList(l.stream().map(this::nss));
    }

    /**
     * Get the talk page of {@code title}.
     *
     * @param title The title to get a talk page for.
     * @return The talk page of {@code title}, or null if {@code title} is a special page or is already a talk page.
     */
    public String talkPageOf(String title) {
        int i = whichNS(title).getValue();
        return i < 0 || i % 2 == 1
                ? null
                : namespaceManager.getValidNamespacesAndNumbers().get(i + 1) + ":" + nss(title);
    }

    /**
     * Get the name of a page belonging to a talk page ({@code title}).
     *
     * @param title The talk page whose content page will be determined.
     * @return The title of the content page associated with the specified talk page, or null if {@code title} is a
     * special page or is already a content page.
     */
    public String talkPageBelongsTo(String title) {
        NS ns = whichNS(title);

        if (ns.getValue() < 0 || ns.getValue() % 2 == 0) {
            return null;
        } else if (ns.equals(NS.TALK)) {
            return nss(title);
        }

        return namespaceManager.getValidNamespacesAndNumbers().get(ns.getValue() - 1) + ":" + nss(title);
    }

    /**
     * Gets the namespace, in NS form, of a title. No namespace or an invalid namespace is assumed to be part of Main.
     *
     * @param title The title to get an NS for.
     * @return The title's NS.
     */
    public NS whichNS(String title) {
        Matcher m = namespaceManager.getPattern().matcher(title);
        return !m.find()
                ? NS.MAIN
                : new NS((int) namespaceManager.getValidNamespacesAndNumbers().get(title.substring(m.start(), m.end() - 1)));
    }

    /**
     * Gets this Wiki's logged in user.
     *
     * @return The user who is logged in, or null if not logged in.
     */
    public String whoami() {
        return wikiConfiguration.getUname() == null ? "<Anonymous>" : wikiConfiguration.getUname();
    }

    /**
     * Gets a String representation of this Wiki, in the format {@code [username @ domain]}
     */
    public String toString() {
        return String.format("[%s @ %s]", whoami(), wikiConfiguration.getHostname());
    }

    /* //////////////////////////////////////////////////////////////////////////////// */
    /* /////////////////////////////////// ACTIONS //////////////////////////////////// */
    /* //////////////////////////////////////////////////////////////////////////////// */

    /**
     * Appends text to a page. If {@code title} does not exist, then create the page normally with {@code text}
     *
     * @param title  The title to edit.
     * @param add    The text to append
     * @param reason The reason to use.
     * @param top    Set to true to prepend text. False will append text.
     * @return True if we were successful.
     */
    public boolean addText(String title, String add, String reason, boolean top) {
        return WAction.addText(this, title, add, reason, !top);
    }

    /**
     * Edit a page, and check if the request actually went through.
     *
     * @param title  The title to use
     * @param text   The text to use
     * @param reason The edit summary to use
     * @return True if the operation was successful.
     */
    public boolean edit(String title, String text, String reason) {
        return WAction.edit(this, title, text, reason);
    }

    /**
     * Deletes a page. You must have admin rights or this won't work.
     *
     * @param title  Title to delete
     * @param reason The reason to use
     * @return True if the operation was successful.
     */
    public boolean delete(String title, String reason) {
        return WAction.delete(this, title, reason);
    }

    /**
     * Purges page caches.
     *
     * @param titles The titles to purge.
     */
    public void purge(String... titles) {
        WAction.purge(this, FL.toStringArrayList(titles));
    }

    /**
     * Removes text from a page. Does nothing if the replacement requested wouldn't change any text on wiki (method still
     * returns true however).
     *
     * @param title  The title to perform the replacement at.
     * @param regex  A regex matching the text to remove.
     * @param reason The edit summary.
     * @return True if we were successful.
     */
    public boolean replaceText(String title, String regex, String reason) {
        return replaceText(title, regex, "", reason);
    }

    /**
     * Replaces text on a page. Does nothing if the replacement requested wouldn't change any text on wiki (method still
     * returns true however).
     *
     * @param title       The title to perform replacement on.
     * @param regex       The regex matching the text to replace.
     * @param replacement The replacing text.
     * @param reason      The edit summary.
     * @return True if were were successful.
     */
    public boolean replaceText(String title, String regex, String replacement, String reason) {
        String s = getPageText(title);
        String rx = s.replaceAll(regex, replacement);

        return rx.equals(s) || edit(title, rx, reason);
    }

    /**
     * Undelete a page. You must have admin rights on the wiki you are trying to perform this task on, otherwise it won't
     * go through.
     *
     * @param title  The title to undelete
     * @param reason The reason to use
     * @return True if we successfully undeleted the page.
     */
    public boolean undelete(String title, String reason) {
        return WAction.undelete(this, title, reason);
    }

    /**
     * Upload a media file.
     *
     * @param p      The file to use
     * @param title  The title to upload to. Must include "File:" prefix.
     * @param text   The text to put on the file description page
     * @param reason The edit summary
     * @return True if we were successful.
     */
    public boolean upload(Path p, String title, String text, String reason) {
        return WAction.upload(this, title, text, reason, p);
    }

    /* //////////////////////////////////////////////////////////////////////////////// */
    /* ///////////////////////////////// QUERIES ////////////////////////////////////// */
    /* //////////////////////////////////////////////////////////////////////////////// */

    /**
     * Get a list of pages from the Wiki.
     *
     * @param prefix        Only return titles starting with this prefix. DO NOT include a namespace prefix (e.g.
     *                      {@code File:}). Optional param - set null to disable
     * @param redirectsOnly Set true to get redirects only.
     * @param protectedOnly Set true to get protected pages only.
     * @param cap           The max number of titles to return. Optional param - set {@code -1} to get all pages.
     * @param namespace     The namespace to filter by. Optional param - set null to disable
     * @return A list of titles on this Wiki, as specified.
     */
    public List<String> allPages(String prefix, boolean redirectsOnly, boolean protectedOnly, int cap, NS namespace) {
        wikiConfiguration.getLog().info(this, "Doing all pages fetch for " + (prefix == null ? "all pages" : prefix));

        WQuery wq = new WQuery(this, cap, WQuery.ALLPAGES);
        if (prefix != null) {
            wq.set("apprefix", prefix);
        }

        if (namespace != null) {
            wq.set("apnamespace", "" + namespace.getValue());
        }

        if (redirectsOnly) {
            wq.set("apfilterredir", "redirects");
        }

        if (protectedOnly) {
            wq.set("apprtype", "edit|move|upload");
        }

        List<String> l = new ArrayList<>();
        while (wq.has()) {
            l.addAll(FL.toArrayList(wq.next().listComp("allpages").stream()
                    .map(jo -> GSONP.getString(jo, VAR_TITLE))
            ));
        }

        return l;
    }

    /**
     * Checks if a title exists.
     *
     * @param title The title to query.
     * @return True if the title exists.
     */
    public boolean exists(String title) {
        wikiConfiguration.getLog().info(this, "Checking to see if title exists: " + title);
        return MQuery.exists(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Gets a list of pages linking to a file.
     *
     * @param title The title to query. PRECONDITION: This must be a valid file name prefixed with the "File:" prefix, or
     *              you will get strange results.
     * @return A list of pages linking to the file.
     */
    public List<String> fileUsage(String title) {
        wikiConfiguration.getLog().info(this, "Fetching local file usage of " + title);
        return MQuery.fileUsage(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Gets a list of file extensions for the types of files which can be uploaded to this Wiki. WARNING: this method is
     * not cached so save the result.
     *
     * @return A list of file extensions for files which can be uploaded to this Wiki.
     */
    public List<String> getAllowedFileExts() {
        wikiConfiguration.getLog().info(this, "Fetching a list of permissible file extensions");
        return FL
                .toArrayList(new WQuery(this, WQuery.ALLOWEDFILEXTS).next().listComp("fileextensions").stream()
                        .map(e -> GSONP.getString(e, "ext")));
    }

    /**
     * Get the categories of a page.
     *
     * @param title The title to get categories of.
     * @return A list of categories, or the empty list if something went wrong.
     */
    public List<String> getCategoriesOnPage(String title) {
        wikiConfiguration.getLog().info(this, "Getting categories of " + title);
        return MQuery.getCategoriesOnPage(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Get all titles in a category.
     *
     * @param title The category to query, including the "Category:" prefix.
     * @param ns    Namespace filter. Any title not in the specified namespace(s) will be ignored. Leave blank to select all
     *              namespaces.
     * @return The list of titles in the category.
     */
    public List<String> getCategoryMembers(String title, NS... ns) {
        return getCategoryMembers(title, -1, ns);
    }

    /**
     * Get a limited number of titles in a category.
     *
     * @param title The category to query, including the "Category:" prefix.
     * @param cap   The maximum number of elements to return. Optional param - set to 0 to disable.
     * @param ns    Namespace filter. Any title not in the specified namespace(s) will be ignored. Leave blank to select all
     *              namespaces. CAVEAT: skipped items are counted against {@code cap}.
     * @return The list of titles, as specified, in the category.
     */
    public List<String> getCategoryMembers(String title, int cap, NS... ns) {
        wikiConfiguration.getLog().info(this, "Getting category members from " + title);

        WQuery wq = new WQuery(this, cap, WQuery.CATEGORYMEMBERS).set("cmtitle", convertIfNotInNS(title, NS.CATEGORY));
        if (ns.length > 0) {
            wq.set("cmnamespace", namespaceManager.createFilter(ns));
        }

        List<String> l = new ArrayList<>();
        while (wq.has()) {
            l.addAll(FL.toArrayList(wq.next().listComp("categorymembers").stream().map(e -> GSONP.getString(e, VAR_TITLE))));
        }

        return l;
    }

    /**
     * Gets the number of elements contained in a category.
     *
     * @param title The title to query. PRECONDITION: Title *must* begin with the "Category:" prefix
     * @return The number of elements in the category. Value returned will be -1 if Category entered was empty <b>and</b>
     * non-existent.
     */
    public int getCategorySize(String title) {
        wikiConfiguration.getLog().info(this, "Getting category size of " + title);
        return MQuery.getCategorySize(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Gets the contributions of a user.
     *
     * @param user       The user to get contribs for, without the "User:" prefix.
     * @param cap        The maximum number of results to return. Optional, disable with -1 (<b>caveat</b>: this will get *all*
     *                   of a user's contributions)
     * @param olderFirst Set to true to enumerate from older → newer revisions
     * @param ns         Restrict titles returned to the specified Namespace(s). Optional, leave blank to select all namespaces.
     * @return A list of contributions.
     */
    public List<Contrib> getContribs(String user, int cap, boolean olderFirst, NS... ns) {
        wikiConfiguration.getLog().info(this, "Fetching contribs of " + user);

        WQuery wq = new WQuery(this, cap, WQuery.USERCONTRIBS).set("ucuser", user);
        if (ns.length > 0) {
            wq.set("ucnamespace", namespaceManager.createFilter(ns));
        }

        if (olderFirst) {
            wq.set("ucdir", VAR_NEWER);
        }

        List<Contrib> result = new ArrayList<>();
        while (wq.has()) {
            result.addAll(FL.toArrayList(wq.next().listComp("usercontribs").stream()
                    .map(jo -> GSONP.getGson().fromJson(jo, Contrib.class))
            ));
        }

        return result;
    }

    /**
     * List duplicates of a file.
     *
     * @param title     The title to query. PRECONDITION: You MUST include the namespace prefix (e.g. "File:")
     * @param localOnly Set to true to restrict results to <span style="font-weight:bold;">local</span> duplicates only.
     * @return Duplicates of this file.
     */
    public List<String> getDuplicatesOf(String title, boolean localOnly) {
        wikiConfiguration.getLog().info(this, "Getting duplicates of " + title);
        return MQuery.getDuplicatesOf(this, localOnly, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Gets a list of external URLs on a page.
     *
     * @param title The title to query
     * @return A List of external links found on the page.
     */
    public List<String> getExternalLinks(String title) {
        wikiConfiguration.getLog().info(this, "Getting external links on " + title);
        return MQuery.getExternalLinks(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Gets information about a File's revisions. Does not fill the thumbnail param of ImageInfo.
     *
     * @param title The title of the file to use (must be in the file namespace and exist, else return null)
     * @return A list of ImageInfo objects, one for each revision. The order is newer -&gt; older.
     */
    public List<ImageInfo> getImageInfo(String title) {
        wikiConfiguration.getLog().info(this, "Getting image info for " + title);
        return MQuery.getImageInfo(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Gets titles of images linked on a page.
     *
     * @param title The title to query
     * @return The images found on <code>title</code>
     */
    public List<String> getImagesOnPage(String title) {
        wikiConfiguration.getLog().info(this, "Getting files on " + title);
        return MQuery.getImagesOnPage(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Gets the username of the editor who last edited a page.
     *
     * @param title The title to query
     * @return The most recent editor of {@code title} (excluding {@code User:} prefix) or null on error.
     */
    public String getLastEditor(String title) {
        try {
            return getRevisions(title, 1, false, null, null).get(0).getUser();
        } catch (Exception e) {
            LOG.error("Exception during obtaining last editor", e);
            return null;
        }
    }

    /**
     * Gets wiki links on a page.
     *
     * @param title The title to query
     * @param ns    Namespaces to include-only. Optional, leave blank to select all namespaces.
     * @return The list of wiki links on the page.
     */
    public List<String> getLinksOnPage(String title, NS... ns) {
        wikiConfiguration.getLog().info(this, "Getting wiki links on " + title);
        return MQuery.getLinksOnPage(this, FL.toStringArrayList(title), ns).get(title);
    }

    /**
     * Gets all existing or non-existing wiki links on a page.
     *
     * @param exists Fetch mode. Set true to get existing pages and false to get missing/non-existent pages.
     * @param title  The title to query
     * @param ns     Namespaces to include-only. Optional, leave blank to select all namespaces.
     * @return The list of existing links on {@code title}
     */
    public List<String> getLinksOnPage(boolean exists, String title, NS... ns) {
        return FL.toArrayList(MQuery.exists(this, getLinksOnPage(title, ns)).entrySet().stream().filter(t -> t.getValue() == exists)
                .map(Map.Entry::getKey));
    }

    /**
     * List log events. Order is newer -&gt; older.
     *
     * @param title The title to fetch logs for. Optional - set null to disable.
     * @param user  The performing user to filter log entries by. Optional - set null to disable
     * @param type  The type of log to get (e.g. delete, upload, patrol). Optional - set null to disable
     * @param cap   Limits the number of entries returned from this log. Optional - set -1 to disable
     * @return The log entries.
     */
    public List<LogEntry> getLogs(String title, String user, String type, int cap) {
        wikiConfiguration.getLog().info(this, String.format("Fetching log entries -> title: %s, user: %s, type: %s", title, user, type));

        WQuery wq = new WQuery(this, cap, WQuery.LOGEVENTS);
        if (title != null) {
            wq.set("letitle", title);
        }

        if (user != null) {
            wq.set("leuser", nss(user));
        }

        if (type != null) {
            wq.set("letype", type);
        }

        List<LogEntry> result = new ArrayList<>();
        while (wq.has()) {
            result.addAll(FL.toArrayList(wq.next().listComp("logevents").stream()
                    .map(jo -> GSONP.getGson().fromJson(jo, LogEntry.class))));
        }

        return result;
    }

    /**
     * Gets the first editor (creator) of a page. Specifically, get the author of the first revision of {@code title}.
     *
     * @param title The title to query
     * @return The page creator (excluding {@code User:} prefix) or null on error.
     */
    public String getPageCreator(String title) {
        try {
            return getRevisions(title, 1, true, null, null).get(0).getUser();
        } catch (Exception e) {
            LOG.error("Exception during obtaining page creator", e);
            return null;
        }
    }

    /**
     * Gets the text of a page.
     *
     * @param title The title to query
     * @return The text of the page, or an empty string if the page is non-existent/something went wrong.
     */
    public String getPageText(String title) {
        wikiConfiguration.getLog().info(this, "Getting page text of " + title);
        return MQuery.getPageText(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Fetches protected titles (create-protected) on the Wiki.
     *
     * @param limit      The maximum number of returned entries. Set -1 to disable.
     * @param olderFirst Set to true to get older entries first.
     * @param ns         Namespace filter, limits returned titles to these namespaces. Optional param - leave blank to disable.
     * @return An ArrayList of protected titles.
     */
    public List<ProtectedTitleEntry> getProtectedTitles(int limit, boolean olderFirst, NS... ns) {
        wikiConfiguration.getLog().info(this, "Fetching a list of protected titles");

        WQuery wq = new WQuery(this, limit, WQuery.PROTECTEDTITLES);
        if (ns.length > 0) {
            wq.set("ptnamespace", namespaceManager.createFilter(ns));
        }

        if (olderFirst) {
            wq.set("ptdir", VAR_NEWER); // MediaWiki is weird.
        }

        List<ProtectedTitleEntry> result = new ArrayList<>();
        while (wq.has()) {
            result.addAll(
                    FL.toArrayList(wq.next().listComp("protectedtitles").stream()
                            .map(jo -> GSONP.getGson().fromJson(jo, ProtectedTitleEntry.class))
                    ));
        }

        return result;
    }

    /**
     * Gets a list of random pages.
     *
     * @param limit The number of titles to retrieve. PRECONDITION: {@code limit} cannot be a negative number.
     * @param ns    Returned titles will be in these namespaces. Optional param - leave blank to disable.
     * @return A list of random titles on this Wiki.
     */
    public List<String> getRandomPages(int limit, NS... ns) {
        wikiConfiguration.getLog().info(this, "Fetching random page(s)");

        if (limit < 0) {
            throw new IllegalArgumentException("limit for getRandomPages() cannot be a negative number");
        }

        List<String> result = new ArrayList<>();
        WQuery wq = new WQuery(this, limit, WQuery.RANDOM);

        if (ns.length > 0) {
            wq.set("rnnamespace", namespaceManager.createFilter(ns));
        }

        while (wq.has()) {
            result.addAll(FL.toArrayList(wq.next().listComp("random").stream()
                    .map(e -> GSONP.getString(e, VAR_TITLE))));
        }

        return result;
    }

    /**
     * Gets a specified number of Recent Changes in between two timestamps. WARNING: if you use both {@code start} and
     * {@code end}, then {@code start} MUST be earlier than {@code end}. If you set both {@code start} and {@code end} to
     * null, then the default behavior is to fetch the last 30 seconds of recent changes.
     *
     * @param start The Instant to start enumerating from. Can be used without {@code end}. Optional param - set null to
     *              disable.
     * @param end   The Instant to stop enumerating at. {@code start} must be set, otherwise this will be ignored. Optional
     *              param - set null to disable.
     * @return A list Recent Changes where return order is newer -&gt; Older
     */
    public List<RecentChangesEntry> getRecentChanges(Instant start, Instant end) {
        wikiConfiguration.getLog().info(this, "Querying recent changes");

        Instant startInstance = start;
        Instant endInstance = end;
        if (startInstance == null) {
            endInstance = Instant.now();
            startInstance = endInstance.minusSeconds(30);
        } else if (endInstance != null && endInstance.isBefore(startInstance)) {
            // implied startInstance != null
            throw new IllegalArgumentException("start is before end, cannot proceed");
        }

        // MediaWiki has start <-> end backwards
        WQuery wq = new WQuery(this, WQuery.RECENTCHANGES).set("rcend", startInstance.toString());
        if (endInstance != null) {
            wq.set("rcstart", endInstance.toString());
        }

        List<RecentChangesEntry> result = new ArrayList<>();
        while (wq.has()) {
            result.addAll(FL.toArrayList(wq.next().listComp("recentchanges").stream().map(jo -> GSONP.getGson().fromJson(jo, RecentChangesEntry.class))));
        }

        return result;
    }

    /**
     * Gets the revisions of a page.
     *
     * @param title      The title to query
     * @param cap        The maximum number of results to return. Optional param: set to any number zero or less to disable.
     * @param olderFirst Set to true to enumerate from older → newer revisions
     * @param start      The instant to start enumerating from. Start date must occur before end date. Optional param - set
     *                   null to disable.
     * @param end        The instant to stop enumerating at. Optional param - set null to disable.
     * @return A list of page revisions
     */
    public List<Revision> getRevisions(String title, int cap, boolean olderFirst, Instant start, Instant end) {
        wikiConfiguration.getLog().info(this, "Getting revisions from " + title);

        WQuery wq = new WQuery(this, cap, WQuery.REVISIONS).set("titles", title);
        if (olderFirst) {
            wq.set("rvdir", VAR_NEWER); // MediaWiki is weird.
        }

        if (start != null && end != null && start.isBefore(end)) {
            wq.set("rvstart", end.toString()); // MediaWiki has start <-> end reversed
            wq.set("rvend", start.toString());
        }

        List<Revision> result = new ArrayList<>();
        while (wq.has()) {
            JsonElement e = wq.next().propComp(VAR_TITLE, "revisions").get(title);
            if (e != null) {
                result.addAll(FL.toArrayList(GSONP.getJsonArrayofJsonObject(e.getAsJsonArray()).stream()
                        .map(jo -> GSONP.getGson().fromJson(jo, Revision.class))));
            }
        }
        return result;
    }

    /**
     * Gets the shared (non-local) duplicates of a file. PRECONDITION: The Wiki this query is run against has the
     * <a href="https://www.mediawiki.org/wiki/Extension:GlobalUsage">GlobalUsage</a> extension installed.
     *
     * @param title The title of the file to query
     * @return An ArrayList containing shared duplicates of the file
     */
    public List<String> getSharedDuplicatesOf(String title) {
        wikiConfiguration.getLog().info(this, "Getting shared duplicates of " + title);
        return MQuery.getSharedDuplicatesOf(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Gets templates transcluded on a page.
     *
     * @param title The title to query.
     * @return The templates transcluded on <code>title</code>
     */
    public List<String> getTemplatesOnPage(String title) {
        wikiConfiguration.getLog().info(this, "Getting templates transcluded on " + title);
        return MQuery.getTemplatesOnPage(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Gets a text extract (the lead paragraph) of a page.
     *
     * @param title The title to get a text extract for.
     * @return The text extract. Null if {@code title} does not exist or is a special page.
     */
    public String getTextExtract(String title) {
        wikiConfiguration.getLog().info(this, "Getting a text extract for " + title);
        return MQuery.getTextExtracts(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Get a user's uploads.
     *
     * @param user The username, without the "User:" prefix. PRECONDITION: <code>user</code> must be a valid username.
     * @return This user's uploads
     */
    public List<String> getUserUploads(String user) {
        wikiConfiguration.getLog().info(this, "Fetching uploads for " + user);

        List<String> result = new ArrayList<>();
        WQuery wq = new WQuery(this, WQuery.USERUPLOADS).set("aiuser", nss(user));
        while (wq.has()) {
            result.addAll(FL.toArrayList(wq.next().listComp("allimages").stream()
                    .map(e -> GSONP.getString(e, VAR_TITLE))
            ));
        }

        return result;
    }

    /**
     * Gets the global usage of a file. PRECONDITION: GlobalUsage must be installed on the target Wiki.
     *
     * @param title The title to query. Must start with <code>File:</code> prefix.
     * @return A HashMap with the global usage of this file; each element is of the form <code>[ title : wiki ]</code>.
     */
    public List<Tuple<String, String>> globalUsage(String title) {
        wikiConfiguration.getLog().info(this, "Getting global usage for " + title);
        return MQuery.globalUsage(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Gets the list of usergroups (rights) a user belongs to. Sample groups: sysop, user, autoconfirmed, editor.
     *
     * @param user The user to get rights information for. Do not include "User:" prefix.
     * @return The usergroups {@code user} belongs to, or null if {@code user} is an IP or non-existent user.
     */
    public List<String> listUserRights(String user) {
        wikiConfiguration.getLog().info(this, "Getting user rights for " + user);
        return MQuery.listUserRights(this, FL.toStringArrayList(user)).get(user);
    }

    /**
     * Does the same thing as Special:PrefixIndex.
     *
     * @param namespace The namespace to filter by (inclusive)
     * @param prefix    Get all titles in the specified namespace, that start with this String. To select subpages only,
     *                  append a {@code /} to the end of this parameter.
     * @return The list of titles starting with the specified prefix
     */
    public List<String> prefixIndex(NS namespace, String prefix) {
        wikiConfiguration.getLog().info(this, "Doing prefix index search for " + prefix);
        return allPages(prefix, false, false, -1, namespace);
    }

    /**
     * Queries a special page.
     *
     * @param title The special page to query, without the {@code Special:} prefix. CAVEAT: this is CASE-sensitive, so be
     *              sure to use the exact title (e.g. {@code UnusedFiles}, {@code BrokenRedirects}). For a full list of
     *              titles, see <a href="https://www.mediawiki.org/w/api.php?action=help&modules=query+querypage">the
     *              official documentation</a>.
     * @param cap   The maximum number of elements to return. Use {@code -1} to get everything, but be careful because some
     *              pages can have 10k+ entries.
     * @return A List of titles returned by this special page.
     */
    public List<String> querySpecialPage(String title, int cap) {
        wikiConfiguration.getLog().info(this, "Querying special page " + title);

        WQuery wq = new WQuery(this, cap, WQuery.QUERYPAGES).set("qppage", nss(title));
        List<String> result = new ArrayList<>();

        while (wq.has()) {
            try {
                result.addAll(FL.toArrayList(FL.streamFrom(GSONP.getNestedJsonArray(wq.next().getInput(), FL.toStringArrayList("query", "querypage", "results")))
                        .map(e -> GSONP.getString(e.getAsJsonObject(), VAR_TITLE))));
            } catch (Exception e) {
                LOG.error("Exception during obtaining special page", e);
            }
        }
        return result;
    }

    /**
     * Attempts to resolve title redirects on a Wiki.
     *
     * @param title The title to attempt resolution at.
     * @return The resolved title, or the original title if it was not a redirect.
     */
    public String resolveRedirect(String title) {
        wikiConfiguration.getLog().info(this, "Resolving redirect for " + title);
        return MQuery.resolveRedirects(this, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Performs a search on the Wiki.
     *
     * @param query The query string to search the Wiki with.
     * @param limit The maximum number of entries to return. Optional, specify {@code -1} to disable (not recommended if
     *              your wiki is big).
     * @param ns    Limit search to these namespaces. Optional, leave blank to disable. The default behavior is to search
     *              all namespaces.
     * @return A List of titles found by the search.
     */
    public List<String> search(String query, int limit, NS... ns) {
        WQuery wq = new WQuery(this, limit, WQuery.SEARCH).set("srsearch", query);

        if (ns.length > 0) {
            wq.set("srnamespace", namespaceManager.createFilter(ns));
        }

        List<String> resultList = new ArrayList<>();
        while (wq.has()) {
            resultList.addAll(FL.toArrayList(wq.next().listComp("search").stream().map(e -> GSONP.getString(e, VAR_TITLE))));
        }

        return resultList;
    }

    /**
     * Splits the text of a page by header.
     *
     * @param title The title to query
     * @return An ArrayList where each section (in order) is contained in a PageSection object.
     */
    public List<PageSection> splitPageByHeader(String title) {
        wikiConfiguration.getLog().info(this, "Splitting " + title + " by header");

        try {
            return PageSection.pageBySection(GSONP.getJsonArrayofJsonObject(GSONP.getNestedJsonArray(
                    JsonParser.parseString(basicGET("parse", "prop", "sections", "page", title).body().string()).getAsJsonObject(),
                    FL.toStringArrayList("parse", "sections"))), getPageText(title));
        } catch (Exception e) {
            LOG.error("Exception during obtaining page sections", e);
            return Collections.emptyList();
        }
    }

    /**
     * Gets a list of links or redirects to a page.
     *
     * @param title     The title to query
     * @param redirects Set to true to get redirects only. Set to false to filter out all redirects.
     * @return A list of links or redirects to this page.
     */
    public List<String> whatLinksHere(String title, boolean redirects) {
        wikiConfiguration.getLog().info(this, "Getting links to " + title);
        return MQuery.linksHere(this, redirects, FL.toStringArrayList(title)).get(title);
    }

    /**
     * Gets a list of direct links to a page. CAVEAT: This does not get any pages linking to a redirect pointing to this
     * page; in order to do this you will first need to obtain a list of redirects to the target, and then call
     * <code>whatLinksHere()</code> on each of those redirects.
     *
     * @param title The title to query
     * @return A list of links to this page.
     */
    public List<String> whatLinksHere(String title) {
        return whatLinksHere(title, false);
    }

    /**
     * Gets a list of pages transcluding a template.
     *
     * @param title The title to query. You *must* include the namespace prefix (e.g. "Template:") or you will get
     *              strange results.
     * @param ns    Only return results from this/these namespace(s). Optional param: leave blank to disable.
     * @return The pages transcluding <code>title</code>.
     */
    public List<String> whatTranscludesHere(String title, NS... ns) {
        wikiConfiguration.getLog().info(this, "Getting list of pages that transclude " + title);
        return MQuery.transcludesIn(this, FL.toStringArrayList(title), ns).get(title);
    }
}