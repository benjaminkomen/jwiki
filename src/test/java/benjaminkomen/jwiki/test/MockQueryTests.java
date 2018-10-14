package benjaminkomen.jwiki.test;

import benjaminkomen.jwiki.core.NS;
import benjaminkomen.jwiki.dwrap.LogEntry;
import benjaminkomen.jwiki.dwrap.ProtectedTitleEntry;
import benjaminkomen.jwiki.dwrap.RecentChangesEntry;
import benjaminkomen.jwiki.util.Tuple;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests queries which may have dynamic/variable outputs.
 *
 * @author Fastily
 */
public class MockQueryTests extends BaseMockTemplate {
    /**
     * Mock fetching of random pages
     */
    @Test
    public void testGetRandomPages() {
        addResponse("mockRandom");
        List<String> result = wiki.getRandomPages(3, NS.FILE, NS.MAIN);

        assertEquals(3, result.size());
        assertEquals(3, new HashSet<>(result).size());
    }

    /**
     * Test fetching of global usage.
     */
    @Test
    public void testGlobalUsage() {
        addResponse("mockGlobalUsage");

        List<Tuple<String, String>> result = wiki.globalUsage("File:Example.jpg");

        assertFalse(result.isEmpty());

        assertEquals("TestTest", result.get(0).getValue1());
        assertEquals("ay.wikipedia.org", result.get(0).getValue2());

        assertEquals("Foobar", result.get(1).getValue1());
        assertEquals("bat-smg.wikipedia.org", result.get(1).getValue2());

        assertEquals("Hello", result.get(2).getValue1());
        assertEquals("ka.wiktionary.org", result.get(2).getValue2());
    }

    /**
     * Test protected title fetching
     */
    @Test
    public void testProtectedTitles() {
        addResponse("mockProtectedTitles");

        List<ProtectedTitleEntry> l = wiki.getProtectedTitles(3, true);

        assertFalse(l.isEmpty());

        assertEquals("File:Test.jpg", l.get(0).getTitle());
        assertEquals("Foo", l.get(0).getUser());
        assertEquals("summary1", l.get(0).getSummary());
        assertEquals(Instant.parse("2007-12-28T03:22:03Z"), l.get(0).getTimestamp());
        assertEquals("sysop", l.get(0).getLevel());

        assertEquals("TestTest", l.get(1).getTitle());
        assertEquals("Foo", l.get(1).getUser());
        assertEquals("summary2", l.get(1).getSummary());
        assertEquals(Instant.parse("2007-12-28T06:41:03Z"), l.get(1).getTimestamp());
        assertEquals("sysop", l.get(1).getLevel());

        assertEquals("File:Example.jpg", l.get(2).getTitle());
        assertEquals("Bar", l.get(2).getUser());
        assertEquals("summary3", l.get(2).getSummary());
        assertEquals(Instant.parse("2007-12-28T06:43:00Z"), l.get(2).getTimestamp());
        assertEquals("autoconfirmed", l.get(2).getLevel());
    }

    /**
     * Test recent changes fetching.
     */
    @Test
    public void testRecentChanges() {
        addResponse("mockRecentChanges");

        List<RecentChangesEntry> l = wiki.getRecentChanges(Instant.parse("2017-12-31T02:06:08Z"), Instant.parse("2017-12-31T02:06:09Z"));

        assertFalse(l.isEmpty());

        assertEquals("edit", l.get(0).getType());
        assertEquals("Title1", l.get(0).getTitle());
        assertEquals("127.0.0.1", l.get(0).getUser());
        assertEquals(Instant.parse("2017-12-31T02:06:09Z"), l.get(0).getTimestamp());
        assertEquals("comment1", l.get(0).getSummary());

        assertEquals("new", l.get(1).getType());
        assertEquals("Title2", l.get(1).getTitle());
        assertEquals("TestUser", l.get(1).getUser());
        assertEquals(Instant.parse("2017-12-31T02:10:32Z"), l.get(1).getTimestamp());
        assertEquals("comment2", l.get(1).getSummary());

        assertEquals("log", l.get(2).getType());
        assertEquals("Title3", l.get(2).getTitle());
        assertEquals("Foobar", l.get(2).getUser());
        assertEquals(Instant.parse("2017-12-31T02:09:57Z"), l.get(2).getTimestamp());
        assertEquals("", l.get(2).getSummary());
    }

    /**
     * Test log entry fetching.
     */
    @Test
    public void testGetLogs() {
        // Test 1
        addResponse("mockLogEntry1");
        List<LogEntry> l = wiki.getLogs("File:Example.jpg", "Fastily", "delete", -1);

        assertEquals(3, l.size());

        assertEquals("File:Example.jpg", l.get(0).getTitle());
        assertEquals("Fastily", l.get(0).getUser());
        assertEquals("summary1", l.get(0).getSummary());
        assertEquals("delete", l.get(0).getAction());
        assertEquals("delete", l.get(0).getType());
        assertEquals(Instant.parse("2010-04-25T01:17:52Z"), l.get(0).getTimestamp());

        assertEquals(Instant.parse("2010-04-25T01:17:48Z"), l.get(1).getTimestamp());
        assertEquals("summary2", l.get(1).getSummary());

        assertEquals(Instant.parse("2010-04-25T01:17:45Z"), l.get(2).getTimestamp());
        assertEquals("summary3", l.get(2).getSummary());

        // Test 2
        addResponse("mockLogEntry2");
        l = wiki.getLogs("Test", null, null, -1);

        assertEquals(3, l.size());

        assertEquals("Test", l.get(0).getTitle());
        assertEquals("Fastily", l.get(0).getUser());
        assertEquals("restore reason", l.get(0).getSummary());
        assertEquals("restore", l.get(0).getAction());
        assertEquals("delete", l.get(0).getType());
        assertEquals(Instant.parse("2017-12-29T07:19:21Z"), l.get(0).getTimestamp());

        assertEquals(Instant.parse("2017-12-19T08:06:22Z"), l.get(1).getTimestamp());
        assertEquals("delete reason", l.get(1).getSummary());
        assertEquals("delete", l.get(1).getAction());
        assertEquals("delete", l.get(1).getType());

        assertEquals(Instant.parse("2017-12-19T07:57:31Z"), l.get(2).getTimestamp());
        assertEquals("", l.get(2).getSummary());
        assertEquals("patrol", l.get(2).getAction());
        assertEquals("patrol", l.get(2).getType());
        assertEquals("FastilyBot", l.get(2).getUser());
        assertEquals("Test", l.get(2).getTitle());
    }

    /**
     * Tests querying of special pages.
     */
    @Test
    public void testQuerySpecialPage() {
        addResponse("mockQuerySpecialPage");

        List<String> l = wiki.querySpecialPage("Deadendpages", 10);

        assertEquals(3, l.size());

        assertTrue(l.contains("TestPage"));
        assertTrue(l.contains("File:Example.jpg"));
        assertTrue(l.contains("Talk:Main page"));
    }

    /**
     * Tests listing of all pages
     */
    @Test
    public void testGetAllPages() {
        addResponse("mockAllPages");

        List<String> l = wiki.allPages(null, false, false, 3, NS.MAIN);

        assertEquals(3, l.size());

        assertTrue(l.contains("Test"));
        assertTrue(l.contains("Foobar"));
        assertTrue(l.contains("Cats"));
    }
}