package benjaminkomen.jwiki.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import okhttp3.HttpUrl;

/**
 * Per-Wiki configurable settings.
 *
 * @author Fastily
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class Conf {
    /**
     * Toggles logging of debug information to std err. Disabled (false) by default.
     */
    private boolean debug;

    /**
     * The {@code User-Agent} header to use for HTTP requests.
     */
    private String userAgent;

    /**
     * The url pointing to the base MediaWiki API endpoint.
     */
    private HttpUrl baseURL;

    /**
     * Default Wiki API path (goes after domain). Don't change this after logging in.
     */
    private String scptPath;

    /**
     * Flag indicating whether the logged in user is a bot.
     */
    private boolean isBot;

    /**
     * The hostname of the Wiki to target. Example: {@code en.wikipedia.org}
     */
    private String hostname;

    /**
     * The low maximum limit for maximum number of list items returned for queries that return lists. Use this if a max
     * value is needed but where the client does not know the max.
     */
    private int maxResultLimit;

    /**
     * User name (without namespace prefix), only set if user is logged in.
     */
    private String uname;

    /**
     * The logger associated with this Conf.
     */
    private ColorLog log;

    /**
     * CSRF token. Used for actions that change Wiki content.
     */
    private String token;

    {
        this.debug = false;
        this.userAgent = String.format("jwiki on %s %s with JVM %s", System.getProperty("os.name"),
                System.getProperty("os.version"), System.getProperty("java.version"));
        this.scptPath = "w/api.php";
        this.isBot = false;
        this.maxResultLimit = 500;
        this.uname = null;
        this.token = "+\\";
    }

    private Conf() {
        // no-args constructor
    }

    /**
     * Constructor, should only be called by new instances of Wiki.
     *
     * @param baseURL The url pointing to the base MediaWiki API endpoint.
     * @param log     The logger associated with this log
     */
    protected Conf(HttpUrl baseURL, ColorLog log) {
        this.baseURL = baseURL;
        this.hostname = baseURL.host();
        this.log = log;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }
}