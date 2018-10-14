package benjaminkomen.jwiki.test;

import benjaminkomen.jwiki.core.Wiki;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for WAction. Tests are performed as if the user is logged in.
 *
 * @author Fastily
 */
public class AuthActionTests extends BaseMockTemplate {
    /**
     * Initializes a logged-in Wiki.
     */
    protected void initWiki() {
        addResponse("mockTokenNotLoggedIn");
        addResponse("mockLoginSuccess");
        addResponse("mockUserInfo");
        addResponse("mockTokenLoggedIn");
        addResponse("mockListSingleUserRights");
        addResponse("mockNSInfo");

        wiki = new Wiki("Test", "password", server.url("/w/api.php"), null, null, true);
    }

    /**
     * Verify that the username was set properly.
     */
    @Test
    public void testWhoAmI() {
        assertEquals("Test", wiki.whoami());
    }

    /**
     * Test privileged delete.
     */
    @Test
    public void testDelete() {
        addResponse("mockDeleteSuccess");
        assertTrue(wiki.delete("Test", "Test Reason"));
    }

    /**
     * Test privileged undelete.
     */
    @Test
    public void testUndelete() {
        addResponse("mockUndeleteSuccess");
        assertTrue(wiki.undelete("Test", "test"));
    }
}