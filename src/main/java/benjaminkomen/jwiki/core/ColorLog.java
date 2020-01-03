package benjaminkomen.jwiki.core;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Colors a String and logs it to console. Your terminal must support ASCII escapes for this to work, otherwise the text
 * will not be colored.
 *
 * @author Fastily
 */
@Getter
class ColorLog {

    private static final Logger LOG = LoggerFactory.getLogger(ColorLog.class);

    /**
     * Flag indicating whether logging with this object is allowed.
     */
    private boolean enabled;

    /**
     * Constructor, creates a new ColorLog.
     *
     * @param enableLogging Set true to allow this ColorLog to print log output.
     */
    protected ColorLog(boolean enableLogging) {
        enabled = enableLogging;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Output warning message for wiki.
     *
     * @param wiki    The wiki object to use
     * @param message The message to print.
     */
    protected void warn(Wiki wiki, String message) {
        LOG.warn("{} - {}", wiki, message);
    }

    /**
     * Output info message for wiki.
     *
     * @param wiki    The wiki object to use
     * @param message The message to print.
     */
    protected void info(Wiki wiki, String message) {
        LOG.info("{} - {}", wiki, message);
    }

    /**
     * Output error message for wiki.
     *
     * @param wiki    The wiki object to use
     * @param message The message to print.
     */
    protected void error(Wiki wiki, String message) {
        LOG.error("{} - {}", wiki, message);
    }

    /**
     * Output debug message for wiki.
     *
     * @param wiki    The wiki object to use
     * @param message The message to print.
     */
    protected void debug(Wiki wiki, String message) {
        LOG.debug("{} - {}", wiki, message);
    }

    /**
     * Output miscellaneous message for wiki.
     *
     * @param wiki    The wiki object to use
     * @param message The message to print.
     */
    protected void fyi(Wiki wiki, String message) {
        LOG.trace("{} - {}", wiki, message);
    }
}