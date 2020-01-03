package benjaminkomen.jwiki.test;

import benjaminkomen.jwiki.core.MQuery;
import benjaminkomen.jwiki.core.Wiki;
import benjaminkomen.jwiki.dwrap.ImageInfo;
import benjaminkomen.jwiki.util.FL;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MQuery in jwiki. These are only simple sanity checks; this is not a comprehensive test suite.
 *
 * @author Fastily
 */
public class MQueryTests {
    /**
     * The Wiki object to use for this test set.
     */
    private static Wiki wiki = new Wiki.Builder().withDomain("test.wikipedia.org").build();

    /**
     * Test for listUserRights.
     */
    @Test
    public void testListUserRights() {
        Map<String, List<String>> result = MQuery.listUserRights(wiki, FL.toStringArrayList("FastilyClone", "Fastily"));

        assertTrue(result.containsKey("Fastily"));
        assertTrue(result.containsKey("FastilyClone"));

        List<String> subResult = result.get("Fastily");
        assertTrue(subResult.contains("sysop"));
        assertTrue(subResult.contains("autoconfirmed"));

        subResult = result.get("FastilyClone");
        assertTrue(subResult.contains("autoconfirmed"));
    }

    /**
     * Test for getImageInfo.
     */
    @Test
    public void testGetImageInfo() {
        Map<String, List<ImageInfo>> result = MQuery.getImageInfo(wiki,
                FL.toStringArrayList("File:FastilyTestCircle1.svg", "File:FastilyTestCircle2.svg"));

        assertTrue(result.containsKey("File:FastilyTestCircle1.svg"));
        assertTrue(result.containsKey("File:FastilyTestCircle2.svg"));

        ImageInfo subResult = result.get("File:FastilyTestCircle1.svg").get(0);
        assertEquals("Fastily", subResult.getUser());
        assertEquals(Instant.parse("2016-03-21T02:12:43Z"), subResult.getTimestamp());
        assertEquals(502, subResult.getHeight());
        assertEquals(512, subResult.getWidth());
        assertEquals("0bfe3100d0277c0d42553b9d16db71a89cc67ef7", subResult.getSha1());

        subResult = result.get("File:FastilyTestCircle2.svg").get(0);
        assertEquals("Fastily", subResult.getUser());
        assertEquals(Instant.parse("2016-03-21T02:13:15Z"), subResult.getTimestamp());
        assertEquals(502, subResult.getHeight());
        assertEquals(512, subResult.getWidth());
        assertEquals("bbe1ffbfb03ec9489ffdb3f33596b531c7b222ef", subResult.getSha1());
    }

    /**
     * Test for getCategoriesOnPage.
     */
    @Test
    public void testGetCategoriesOnPage() {
        Map<String, List<String>> result = MQuery.getCategoriesOnPage(wiki,
                FL.toStringArrayList("User:Fastily/Sandbox/Page/2", "User:Fastily/Sandbox/Page/3"));

        assertTrue(result.containsKey("User:Fastily/Sandbox/Page/2"));
        assertTrue(result.containsKey("User:Fastily/Sandbox/Page/3"));

        List<String> subResult = result.get("User:Fastily/Sandbox/Page/2");
        assertEquals(2, subResult.size());
        assertTrue(subResult.contains("Category:Fastily Test"));
        assertTrue(subResult.contains("Category:Fastily Test2"));

        subResult = result.get("User:Fastily/Sandbox/Page/3");
        assertEquals(1, subResult.size());
        assertTrue(subResult.contains("Category:Fastily Test"));
    }

    /**
     * Test for getCategorySize.
     */
    @Test
    public void testGetCategorySize() {
        Map<String, Integer> result = MQuery.getCategorySize(wiki, FL.toStringArrayList("Category:Fastily Test", "Category:Fastily Test2"));

        assertTrue(result.containsKey("Category:Fastily Test"));
        assertTrue(result.containsKey("Category:Fastily Test2"));

        assertEquals(4, result.get("Category:Fastily Test"));
        assertEquals(2, result.get("Category:Fastily Test2"));
    }

    /**
     * Test for getPageText.
     */
    @Test
    public void testGetPageText() {
        Map<String, String> result = MQuery.getPageText(wiki, FL.toStringArrayList("User:Fastily/Sandbox/HelloWorld", "Category:Fastily Test"));

        assertTrue(result.containsKey("User:Fastily/Sandbox/HelloWorld"));
        assertTrue(result.containsKey("Category:Fastily Test"));

        assertEquals("Hello World!", result.get("User:Fastily/Sandbox/HelloWorld"));
        assertEquals("jwiki unit testing!", result.get("Category:Fastily Test"));
    }

    /**
     * Tests resolving of redirects
     */
    @Test
    public void testResolveRedirects() {
        Map<String, String> result = MQuery.resolveRedirects(wiki,
                FL.toStringArrayList("User:Fastily/Sandbox/Redirect1", "User:Fastily/Sandbox/Redirect2", "User:Fastily/Sandbox/Redirect3"));

        assertEquals("User:Fastily/Sandbox/RedirectTarget", result.get("User:Fastily/Sandbox/Redirect1"));
        assertEquals("User:Fastily/Sandbox/RedirectTarget", result.get("User:Fastily/Sandbox/Redirect2"));
        assertEquals("User:Fastily/Sandbox/Redirect3", result.get("User:Fastily/Sandbox/Redirect3"));
    }

    /**
     * Verifies that passing {@code null} as an element in a {@code titles} Collection is not permitted.
     */
    @Test
    public void testNullTitles() {
        assertThrows(IllegalArgumentException.class, () -> MQuery.exists(wiki, FL.toStringArrayList("", null, "test")));
    }
}