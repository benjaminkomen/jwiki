package benjaminkomen.jwiki.core;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Colors a String and logs it to console. Your terminal must support ASCII escapes for this to work, otherwise the text
 * will not be colored.
 *
 * @author Fastily
 */
@Getter
class ColorLog {
    /**
     * The date formatter prefixing output.
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm:ss a");

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
     * Logs a message for a wiki method.
     *
     * @param wiki     The wiki object to use
     * @param message  The String to print
     * @param logLevel The identifier to log the message at (e.g. "INFO", "WARNING")
     * @param color    The color to print the message with. Output will only be colored if this terminal supports it.
     */
    @SuppressWarnings("squid:S1192")
    private void log(Wiki wiki, String message, LogLevel logLevel, Color color) {
        if (enabled && (LOG.isDebugEnabled() || LOG.isErrorEnabled() || LOG.isInfoEnabled() || LOG.isTraceEnabled() || LOG.isWarnEnabled())) {
            final String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            switch (logLevel) {
                case WARNING:
                    LOG.warn(String.format("%s%n%s: \u001B[3%dm%s: %s\u001B[0m%n", timestamp, logLevel, color.colorValue, wiki, message));
                    break;
                case INFO:
                    LOG.info(String.format("%s%n%s: \u001B[3%dm%s: %s\u001B[0m%n", timestamp, logLevel, color.colorValue, wiki, message));
                    break;
                case ERROR:
                    LOG.error(String.format("%s%n%s: \u001B[3%dm%s: %s\u001B[0m%n", timestamp, logLevel, color.colorValue, wiki, message));
                    break;
                case DEBUG:
                    LOG.debug(String.format("%s%n%s: \u001B[3%dm%s: %s\u001B[0m%n", timestamp, logLevel, color.colorValue, wiki, message));
                    break;
                case FYI:
                    LOG.info(String.format("%s%n%s: \u001B[3%dm%s: %s\u001B[0m%n", timestamp, logLevel, color.colorValue, wiki, message));
                    break;
                default:
                    throw new IllegalStateException("Log level " + logLevel + " is unknown.");
            }
        }
    }

    /**
     * Output warning message for wiki. Text is yellow.
     *
     * @param wiki The wiki object to use
     * @param s    The String to print.
     */
    protected void warn(Wiki wiki, String s) {
        log(wiki, s, LogLevel.WARNING, Color.YELLOW);
    }

    /**
     * Output info message for wiki. Text is green.
     *
     * @param wiki The wiki object to use
     * @param s    The String to print.
     */
    protected void info(Wiki wiki, String s) {
        log(wiki, s, LogLevel.INFO, Color.GREEN);
    }

    /**
     * Output error message for wiki. Text is red.
     *
     * @param wiki The wiki object to use
     * @param s    The String to print.
     */
    protected void error(Wiki wiki, String s) {
        log(wiki, s, LogLevel.ERROR, Color.RED);
    }

    /**
     * Output debug message for wiki. Text is purple.
     *
     * @param wiki The wiki object to use
     * @param s    The String to print.
     */
    protected void debug(Wiki wiki, String s) {
        log(wiki, s, LogLevel.DEBUG, Color.PURPLE);
    }

    /**
     * Output miscellaneous message for wiki. Text is blue.
     *
     * @param wiki The wiki object to use
     * @param s    The String to print.
     */
    protected void fyi(Wiki wiki, String s) {
        log(wiki, s, LogLevel.FYI, Color.CYAN);
    }

    /**
     * Represents ASCII colors.
     *
     * @author Fastily
     */
    private enum Color {
        /**
         * A font color, black, which can be applied to a String if your terminal supports it.
         */
        BLACK(0),

        /**
         * A font color, red, which can be applied to a String if your terminal supports it.
         */
        RED(1),

        /**
         * A font color, green, which can be applied to a String if your terminal supports it.
         */
        GREEN(2),

        /**
         * A font color, yellow, which can be applied to a String if your terminal supports it.
         */
        YELLOW(3),

        /**
         * A font color, blue, which can be applied to a String if your terminal supports it.
         */
        BLUE(4),

        /**
         * A font color, purple, which can be applied to a String if your terminal supports it.
         */
        PURPLE(5),

        /**
         * A font color, cyan, which can be applied to a String if your terminal supports it.
         */
        CYAN(6),

        /**
         * A font color, white, which can be applied to a String if your terminal supports it.
         */
        WHITE(7);

        /**
         * The ascii color value.
         */
        private final int colorValue;

        /**
         * Constructor, creates a new Color.
         *
         * @param colorValue The color code to use.
         */
        Color(int colorValue) {
            this.colorValue = colorValue;
        }
    }

    private enum LogLevel {
        WARNING,
        INFO,
        ERROR,
        DEBUG,
        FYI;
    }
}