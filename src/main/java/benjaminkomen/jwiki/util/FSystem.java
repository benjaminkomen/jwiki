package benjaminkomen.jwiki.util;

import java.nio.file.FileSystems;
import java.time.format.DateTimeFormatter;

/**
 * System properties and static error handling methods.
 *
 * @author Fastily
 */
public final class FSystem {
    /**
     * The default line separator for text files by OS. For Windows it's '\r\n' and for Mac/Unix it's just '\n'.
     */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * The default separator for pathnames by OS. For Windows it is '\' for Mac/Unix it is '/'
     */
    public static final String PATHNAME_SEPARATOR = FileSystems.getDefault().getSeparator();

    /**
     * A date formatter for UTC times.
     */
    public static final DateTimeFormatter ISO_8601_DATETIMEFORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FSystem() {
        // no-args constructor
    }

    /**
     * Prints an error message to std err and exits with status 1.
     *
     * @param errorMessage Error message to print
     */
    public static void errAndExit(String errorMessage) {
        if (errorMessage != null) {
            System.err.println(errorMessage);
        }
        System.exit(1);
    }

    /**
     * Prints stack trace from specified error and exit.
     *
     * @param e The error object.
     * @param s Additional error message. Disable with null.
     */
    public static void errAndExit(Throwable e, String s) {
        e.printStackTrace();
        errAndExit(s);
    }
}