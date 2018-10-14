package benjaminkomen.jwiki.test;

import benjaminkomen.jwiki.core.NS;
import benjaminkomen.jwiki.core.Wiki;
import benjaminkomen.jwiki.dwrap.Contrib;
import benjaminkomen.jwiki.dwrap.ImageInfo;
import benjaminkomen.jwiki.dwrap.PageSection;
import benjaminkomen.jwiki.dwrap.Revision;
import benjaminkomen.jwiki.util.FL;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Query tests (non-admin) for jwiki's Wiki.java.  These are only simple sanity checks; this is not a comprehensive test suite.
 *
 * @author Fastily
 */
public class QueryTests {
    /**
     * The wiki object to use for this test set.
     */
    private static Wiki wiki = new Wiki("test.wikipedia.org");

    /**
     * Tests for namespace handling
     */
    @Test
    public void testBasicNSHandling() {
        assertEquals(NS.MAIN, wiki.getNS("Main"));
        assertNull(wiki.getNS("blahblahblah"));

        assertEquals(NS.USER, wiki.whichNS("User:TestUser"));
        assertEquals("ABC.jpg", wiki.nss("File:ABC.jpg"));
        assertEquals("ABC.jpg", wiki.nss("fIlE:ABC.jpg"));
        assertEquals("TestUser", wiki.nss("user tALk:TestUser"));

        assertEquals("File:ABC.jpg", wiki.convertIfNotInNS("ABC.jpg", NS.FILE));
        assertEquals("Testing", wiki.convertIfNotInNS("Testing", NS.MAIN));
        assertEquals("User talk:TestUser", wiki.convertIfNotInNS("TestUser", NS.USER_TALK));
    }

    /**
     * Test for prefix index.
     */
    @Test
    public void testPrefixIndex() {
        List<String> result = wiki.prefixIndex(NS.USER, "Fastily/Sandbox/Page/");
        List<String> expected = FL.toStringArrayList("User:Fastily/Sandbox/Page/1", "User:Fastily/Sandbox/Page/2",
                "User:Fastily/Sandbox/Page/3");

        assertEquals(3, result.size());
        assertTrue(result.containsAll(expected));
    }

    /**
     * Tests PrefixIndex functionality of allPages. NB: PrefixIndex is built off all-pages.
     */
    @Test
    public void testAllpages1() {
        List<String> result = wiki.allPages("Fastily/Sandbox/Page/", false, false, -1, NS.USER);
        List<String> expected = FL.toStringArrayList("User:Fastily/Sandbox/Page/1", "User:Fastily/Sandbox/Page/2",
                "User:Fastily/Sandbox/Page/3");

        assertEquals(3, result.size());
        assertTrue(result.containsAll(expected));
    }

    /**
     * Tests allpages() for redirect selection.
     */
    @Test
    public void testAllPages2() {
        List<String> result = wiki.allPages("Fastily/Sandbox/Redirect", true, false, -1, NS.USER);
        List<String> expected = FL.toStringArrayList("User:Fastily/Sandbox/Redirect1", "User:Fastily/Sandbox/Redirect2");

        assertEquals(2, result.size());
        assertTrue(result.containsAll(expected));

        assertFalse(result.contains("User:Fastily/Sandbox/Redirect3")); // Redirect3 isn't actually a redirect
    }

    /**
     * Test exists()
     */
    @Test
    public void testExists() {
        assertTrue(wiki.exists("Main Page"));
        assertTrue(wiki.exists("User:Fastily/Sandbox"));
        assertTrue(wiki.exists("uSeR:fastily/Sandbox"));

        assertFalse(wiki.exists("User:Fastily/NoPageHere"));
        assertFalse(wiki.exists("user:fastily/noPageHere"));
    }

    /**
     * Test for fileUsage()
     */
    @Test
    public void testFileUsage() {
        // Test 1
        List<String> result = wiki.fileUsage("File:FastilyTest.svg");
        List<String> expected = FL.toStringArrayList("User:Fastily/Sandbox/ImageLinks", "User:Fastily/Sandbox/Page");

        assertEquals(2, result.size());
        assertTrue(result.containsAll(expected));

        // Test 2
        result = wiki.fileUsage("File:Fastily NonExistent File.png");
        //expected = new ArrayList<>();

        assertEquals(0, result.size());
    }

    /**
     * Test to determine if namespace filtering is working
     */
    @Test
    public void testFilterByNS() {
        List<String> input = new ArrayList<>(
                Arrays.asList("File:123.png", "User:Fastily", "Category:Foo", "Template:Tester", "User:Fastily/Sandbox"));

        // test 1
        List<String> result = wiki.filterByNS(input, NS.USER);

        assertEquals(2, result.size());
        assertTrue(result.contains("User:Fastily"));
        assertTrue(result.contains("User:Fastily/Sandbox"));

        // test 2
        result = wiki.filterByNS(input, NS.TEMPLATE);

        assertEquals(1, result.size());
        assertTrue(result.contains("Template:Tester"));
    }

    /**
     * Test to determine if we're getting all categories on a page.
     */
    @Test
    public void testGetCategoriesOnPage() {
        List<String> result = wiki.getCategoriesOnPage("User:Fastily/Sandbox/Page/2");
        List<String> expected = FL.toStringArrayList("Category:Fastily Test", "Category:Fastily Test2");

        assertEquals(2, result.size());
        assertTrue(result.containsAll(expected));
    }

    /**
     * Test for most basic usage of getCategoryMembers. No namespace filter.
     */
    @Test
    public void testGetCategoryMembers1() {
        List<String> result = wiki.getCategoryMembers("Fastily Test2");
        List<String> expected = FL.toStringArrayList("User:Fastily/Sandbox/Page/2", "File:FastilyTest.png");

        assertEquals(2, result.size());
        assertTrue(result.containsAll(expected));
    }

    /**
     * Test for getCategoryMembers with namespace filter.
     */
    @Test
    public void testGetCategoryMembers2() {
        List<String> result = wiki.getCategoryMembers("Fastily Test2", NS.FILE);

        assertEquals(1, result.size());
        assertTrue(result.contains("File:FastilyTest.png"));
    }

    /**
     * Test for getCategoryMembers with limit and filter.
     */
    @Test
    public void testGetCategoryMembers3() {
        List<String> result = wiki.getCategoryMembers("Fastily Test", 2, NS.USER);
        List<String> possible = FL.toStringArrayList("User:Fastily/Sandbox/Page/1", "User:Fastily/Sandbox/Page/2",
                "User:Fastily/Sandbox/Page/3");

        assertEquals(2, result.size());
        assertTrue(possible.containsAll(result));
    }

    /**
     * Test for getCategorySize
     */
    @Test
    public void testGetCategorySize() {
        assertEquals(4, wiki.getCategorySize("Category:Fastily Test"));
        assertEquals(2, wiki.getCategorySize("Category:Fastily Test2"));

        assertEquals(0, wiki.getCategorySize("Category:Does0Not0Exist6"));
    }

    /**
     * Tests getContribs
     */
    @Test
    public void testGetContribs() {
        // Test 1
        List<Contrib> result = wiki.getContribs("FastilyClone", -1, false, NS.FILE);

        assertEquals("File:FCTest2.svg", result.get(0).getTitle()); // descending
        assertEquals("File:FCTest1.png", result.get(1).getTitle());

        assertEquals(244225, result.get(0).getRevid());
        assertEquals(244224, result.get(1).getRevid());

        assertEquals(0, result.get(0).getParentid());
        assertEquals(0, result.get(1).getParentid());

        assertEquals(Instant.parse("2015-10-20T00:28:54Z"), result.get(0).getTimestamp());
        assertEquals(Instant.parse("2015-10-20T00:28:32Z"), result.get(1).getTimestamp());

        // Test 2
        result = wiki.getContribs("FastilyClone", 1, true, NS.FILE);
        assertEquals("File:FCTest1.png", result.get(0).getTitle());
    }

    /**
     * Tests getDuplicatesOf()
     */
    @Test
    public void testGetDuplicatesOf() {
        List<String> result = wiki.getDuplicatesOf("File:FastilyTest.svg", true);

        assertEquals(1, result.size());
        assertEquals("FastilyTestCopy.svg", result.get(0));
    }

    /**
     * Test for getImageInfo()
     */
    @Test
    public void testGetImageInfo() {
        // Test 1
        ImageInfo result = wiki.getImageInfo("File:FastilyTestR.svg").get(0);
        assertEquals(477, result.getHeight());
        assertEquals(512, result.getWidth());
        assertEquals(876, result.getSize());
        assertEquals("275e96b2660f761cca02b8d2cb5425bcaab4dd98", result.getSha1());

        // Test 2
        result = wiki.getImageInfo("File:FastilyTest.svg").get(0);
        assertEquals(477, result.getHeight());
        assertEquals(512, result.getWidth());
        assertEquals(876, result.getSize());
        assertEquals("275e96b2660f761cca02b8d2cb5425bcaab4dd98", result.getSha1());
        assertEquals("image/svg+xml", result.getMime());
        assertEquals(HttpUrl.parse("https://upload.wikimedia.org/wikipedia/test/f/f7/FastilyTest.svg"), result.getUrl());
        assertEquals("part of unit test for jwiki", result.getSummary());
    }

    /**
     * Test for getImagesOnPage()
     */
    @Test
    public void testGetImagesOnPage() {
        List<String> result = wiki.getImagesOnPage("User:Fastily/Sandbox/Page");
        List<String> expected = FL.toStringArrayList("File:FastilyTest.svg", "File:FastilyTest.png");

        assertEquals(2, result.size());
        assertTrue(result.containsAll(expected));
    }

    /**
     * Test for getting *all* links on a page
     */
    @Test
    public void testGetLinksOnPage1() {
        List<String> result = wiki.getLinksOnPage("User:Fastily/Sandbox/Page", NS.USER);
        List<String> expected = FL.toStringArrayList("User:Fastily/Sandbox/Page/1", "User:Fastily/Sandbox/Page/2",
                "User:Fastily/Sandbox/Page/3", "User:Fastily/Sandbox/Page/4");

        assertEquals(4, result.size());
        assertTrue(result.containsAll(expected));
    }

    /**
     * Test for getting only dead links on a page
     */
    @Test
    public void testGetLinksOnPage2() {
        List<String> result = wiki.getLinksOnPage(false, "User:Fastily/Sandbox/Page", NS.USER);

        assertEquals(1, result.size());
        assertEquals("User:Fastily/Sandbox/Page/4", result.get(0));
    }

    /**
     * Tests getPageText()
     */
    @Test
    public void testGetPageText() {
        assertEquals("Hello World!", wiki.getPageText("User:Fastily/Sandbox/HelloWorld"));
        assertEquals("jwiki unit testing!", wiki.getPageText("Category:Fastily Test"));

        assertTrue(wiki.getPageText("User:Fastily/NoPageHere").isEmpty());
    }

    /**
     * Tests getRevisions()
     */
    @Test
    public void testGetRevisions() {
        // Test 1
        List<Revision> result = wiki.getRevisions("User:FastilyClone/Page/1", -1, false, null, null);

        assertEquals(3, result.size());
        assertEquals("1", result.get(1).getText());
        assertEquals("s0", result.get(2).getSummary());
        assertEquals(Instant.parse("2015-10-23T05:58:54Z"), result.get(0).getTimestamp());

        // Test 2
        result = wiki.getRevisions("User:FastilyClone/Page/1", 2, true, null, null);

        assertEquals(2, result.size());
        assertEquals("s1", result.get(1).getSummary());
        assertEquals("0", result.get(0).getText());
    }

    /**
     * Test for getTemplatesOnPage()
     */
    @Test
    public void testGetTemplatesOnPage() {
        List<String> result = wiki.getTemplatesOnPage("User:Fastily/Sandbox/T");
        List<String> expected = FL.toStringArrayList("User:Fastily/Sandbox/T/1", "Template:FastilyTest");

        assertEquals(2, result.size());
        assertTrue(result.containsAll(expected));
    }

    /**
     * Test getting a user's uploads
     */
    @Test
    public void testGetUserUploads() {
        List<String> result = wiki.getUserUploads("FastilyClone");
        List<String> expected = FL.toStringArrayList("File:FCTest2.svg", "File:FCTest1.png");

        assertEquals(2, result.size());
        assertTrue(result.containsAll(expected));
    }

    /**
     * Tests user list group rights.
     */
    @Test
    public void testListGroupRights() {
        List<String> l = wiki.listUserRights("Fastily");
        assertTrue(l.contains("sysop"));
        assertTrue(l.contains("autoconfirmed"));
    }

    /**
     * Tests what links here
     */
    @Test
    public void testWhatLinksHere() {
        // test case where just getting *direct* links (no links to redirects considered)
        List<String> l = wiki.whatLinksHere("User:Fastily/Sandbox/Link/1");

        assertEquals(3, l.size());
        assertTrue(l.contains("User:Fastily/Sandbox/Link"));
        assertTrue(l.contains("User:Fastily/Sandbox/Link/2"));
        assertTrue(l.contains("User:Fastily/Sandbox/Link/3"));

        // test case where fetching redirects
        l = wiki.whatLinksHere("User:Fastily/Sandbox/Link/1", true);

        assertEquals(1, l.size());
        assertTrue(l.contains("User:Fastily/Sandbox/Link/4"));
    }

    /**
     * Tests whatTranscludesHere()
     */
    @Test
    public void testWhatTranscludesHere() {
        List<String> l = wiki.whatTranscludesHere("Template:FastilyTest");

        assertEquals(2, l.size());
        assertTrue(l.contains("User:Fastily/Sandbox/T"));
        assertTrue(l.contains("FastilyTest"));

        l = wiki.whatTranscludesHere("Template:FastilyTest", NS.MAIN);
        assertEquals(1, l.size());
        assertTrue(l.contains("FastilyTest"));
    }

    /**
     * Tests external link fetching.
     */
    @Test
    public void testGetExternalLinks() {
        List<String> l = wiki.getExternalLinks("User:Fastily/Sandbox/ExternalLink");

        assertTrue(l.contains("https://www.google.com"));
        assertTrue(l.contains("https://www.facebook.com"));
        assertTrue(l.contains("https://github.com"));

        assertEquals(3, l.size());
    }

    /**
     * Test getting text extracts
     */
    @Test
    public void testGetTextExtracts() {
        assertEquals("Start of an article", wiki.getTextExtract("User:Fastily/Sandbox/Article"));
    }

    /**
     * Test getting talk page of a title
     */
    @Test
    public void testGetTalkPage() {
        assertEquals("File talk:Example.jpg", wiki.talkPageOf("File:Example.jpg"));
        assertEquals("Talk:Main Page", wiki.talkPageOf("Main Page"));
        assertEquals("Wikipedia talk:Test", wiki.talkPageOf("Wikipedia:Test"));
        assertEquals("TimedText talk:File:Test.webm.srt", wiki.talkPageOf("TimedText:File:Test.webm.srt"));

        // check error conditions
        assertNull(wiki.talkPageOf("Talk:Main Page"));
        assertNull(wiki.talkPageOf("File talk:Example.jpg"));
        assertNull(wiki.talkPageOf("Special:Upload"));
    }

    /**
     * Test getting the content page associated with a talk page
     */
    @Test
    public void testTalkPageBelongsTo() {
        assertEquals("File:Example.jpg", wiki.talkPageBelongsTo("File talk:Example.jpg"));
        assertEquals("Main Page", wiki.talkPageBelongsTo("Talk:Main Page"));
        assertEquals("Wikipedia:Test", wiki.talkPageBelongsTo("Wikipedia talk:Test"));
        assertEquals("TimedText:File:Test.webm.srt", wiki.talkPageBelongsTo("TimedText talk:File:Test.webm.srt"));

        //check error conditions
        assertNull(wiki.talkPageBelongsTo("Main Page"));
        assertNull(wiki.talkPageBelongsTo("File:Example.jpg"));
        assertNull(wiki.talkPageBelongsTo("Special:Upload"));
    }

    /**
     * Test splitting a page by header
     */
    @Test
    public void testSplitPageByHeader() {
        List<PageSection> l = wiki.splitPageByHeader("User:Fastily/Sandbox/HelloWorld2");

        assertEquals(1, l.size());
        assertEquals("Hello, World!", l.get(0).getHeader());
        assertEquals(2, l.get(0).getLevel());


        l = wiki.splitPageByHeader("User:Fastily/Sandbox/HelloWorld");
        assertEquals(1, l.size());
        assertNull(l.get(0).getHeader());
        assertEquals(-1, l.get(0).getLevel());
        assertEquals("Hello World!", l.get(0).getText());

        l = wiki.splitPageByHeader("User:Fastily/Sandbox/Article");
        assertEquals(3, l.size());
        assertNull(l.get(0).getHeader());
        assertEquals("Section 1", l.get(1).getHeader());
        assertEquals("Section 2", l.get(2).getHeader());

        assertEquals("Start of an article\n\n", l.get(0).getText());
        assertEquals("==Section 2==\nFoo Baz Bar", l.get(2).getText());
    }

    /**
     * Tests resolving of redirects
     */
    @Test
    public void testResolveRedirect() {
        assertEquals("User:Fastily/Sandbox/RedirectTarget", wiki.resolveRedirect("User:Fastily/Sandbox/Redirect2"));

        // test resolving of non-redirect
        assertEquals("User:Fastily/Sandbox/Redirect3", wiki.resolveRedirect("User:Fastily/Sandbox/Redirect3"));
    }
}