package benjaminkomen.jwiki.test;

import benjaminkomen.jwiki.core.Wiki;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Template for mock tests.
 *
 * @author Fastily
 */
public class BaseMockTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(BaseMockTemplate.class);

    /**
     * The mock MediaWiki server
     */
    protected MockWebServer server;

    /**
     * The test Wiki object to use.
     */
    protected Wiki wiki;

    /**
     * Initializes mock objects
     *
     * @throws IOException If the MockWebServer failed to start.
     */
    @BeforeEach
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        if (LOG.isInfoEnabled()) {
            LOG.info(String.format("[FYI]: MockServer is @ [%s]%n", server.url("/w/api.php")));
        }

        initWiki();
    }

    /**
     * Disposes of mock objects
     *
     * @throws IOException If the MockWebServer failed to exit.
     */
    @AfterEach
    public void tearDown() throws IOException {
        wiki = null;

        server.shutdown();
        server = null;
    }

    /**
     * Loads a MockResponse into the {@code server}'s queue.
     *
     * @param fileName The text file, without a {@code .txt} extension, to load a response from.
     */
    protected void addResponse(String fileName) {
        try {
            server.enqueue(new MockResponse()
                    .setBody(String.join("\n", Files.readAllLines(Paths.get(getClass().getResource(fileName + ".json").toURI())))));
        } catch (Exception e) {
            LOG.error("Error reading mock json file", e);
            throw new IllegalStateException("Should *never* reach here. Is a mock configuration file missing?");
        }
    }

    /**
     * Initializes the mock Wiki object. Runs with {@code setUp()}; override this to customize {@code wiki}'s
     * initialization behavior.
     */
    protected void initWiki() {
        addResponse("mockNSInfo");
        wiki = new Wiki.Builder().withApiEndpoint(server.url("/w/api.php")).build();
    }
}