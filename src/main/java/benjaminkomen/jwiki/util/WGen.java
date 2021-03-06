package benjaminkomen.jwiki.util;

import benjaminkomen.jwiki.core.Wiki;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple, pluggable key-store manager.
 *
 * @author Fastily
 */
public class WGen {

    private static final Logger LOG = LoggerFactory.getLogger(WGen.class);

    /**
     * The default filenames to save credentials under.
     */
    private static Path px = Paths.get(".px.txt");
    private static Path homePX = Paths.get(System.getProperty("user.home") + FSystem.PATHNAME_SEPARATOR + px);

    /**
     * Cache Wiki objects to avoid multiple logins.
     */
    private static Map<String, Wiki> cache = new HashMap<>();

    private WGen() {
        // no-args constructor
    }

    /**
     * Main driver, runs the WGen application.
     *
     * @param args Program arguments, not used
     */
    public static void main(String[] args) {
        Console c = System.console();
        if (c == null) {
            FSystem.errAndExit("You are not running in CLI mode.");
            return;
        }

        c.printf("Welcome to WGen!%nThis utility encodes and stores usernames/passwords%n%n");

        Map<String, String> ul = new HashMap<>();

        while (true) {
            String u = c.readLine("Enter a username: ").trim();
            c.printf("*** Characters hidden for security ***%n");
            char[] p1 = c.readPassword("Enter password for %s: ", u);
            char[] p2 = c.readPassword("Re-enter password for %s: ", u);

            if (Arrays.equals(p1, p2)) {
                ul.put(u, new String(p1));
            } else {
                c.printf("ERROR: Entered passwords do not match!%n");
            }

            if (!c.readLine("Continue? (y/N): ").trim().matches("(?i)(y|yes)")) {
                break;
            }

            c.printf("%n");
        }

        if (ul.isEmpty()) {
            FSystem.errAndExit("You did not make any entries.  Doing nothing.");
        }

        StringBuilder sb = new StringBuilder();
        ul.forEach((k, v) -> sb.append(String.format("%s\t%s%n", k, v)));

        byte[] bytes = Base64.getEncoder().encode(sb.toString().getBytes());

        try {
            Files.write(px, bytes);
            Files.write(homePX, bytes);
            c.printf("Successfully written out to '%s' and '%s'%n", px, homePX);
        } catch (Exception e) {
            LOG.error("ERROR: unable to write to output files.  Are you missing write permissions?", e);
        }
    }

    /**
     * Gets a the password for a user.
     *
     * @param user The username to get a password for.
     * @return The key associated with {@code user}
     */
    public static synchronized String passwordFor(String user) {
        Path f;
        if (Files.isRegularFile(px)) {
            f = px;
        } else if (Files.isRegularFile(homePX)) {
            f = homePX;
        } else {
            throw new RuntimeException("ERROR: Could not find px or homePX.  Have you run WGen yet?");
        }

        try {
            for (String[] a : FL
                    .toArrayList(Arrays.stream(new String(Base64.getDecoder().decode(Files.readAllBytes(f))).split("\n")).map(s -> s.split("\t"))))
                if (a[0].equals(user)) {
                    return a[1];
                }
        } catch (Exception e) {
            LOG.error("Error during obtaining password", e);
        }

        return null;
    }

    /**
     * Creates or returns a Wiki using locally stored credentials previously saved with the main method. This method is
     * cached.
     *
     * @param user   The username to use
     * @param domain The domain (shorthand) to login at.
     * @return The requested Wiki, or null if we have no such user/password combination.
     */
    public static synchronized Wiki get(String user, String domain) {
        if (cache.containsKey(user)) {
            return cache.get(user).getWiki(domain);
        }

        try {
            Wiki wiki = new Wiki.Builder().withLogin(user, passwordFor(user)).withDomain(domain).build();
            cache.put(user, wiki);
            return wiki;
        } catch (Exception e) {
            LOG.error("Error during obtaining wiki", e);
            return null;
        }
    }
}