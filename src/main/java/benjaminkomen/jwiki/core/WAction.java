package benjaminkomen.jwiki.core;

import benjaminkomen.jwiki.util.FL;
import benjaminkomen.jwiki.util.GSONP;
import benjaminkomen.jwiki.util.Tuple;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import okhttp3.Response;
import okio.BufferedSource;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/**
 * Static methods to perform changes to a Wiki.
 *
 * @author Fastily
 */
class WAction {

    private static final String VAR_TITLE = "title";
    private static final String VAR_FILEKEY = "filekey";
    private static final String VAR_UPLOAD = "upload";
    private static final Logger LOG = LoggerFactory.getLogger(WAction.class);

    private WAction() {
        // no-args constructor
    }

    /**
     * {@code POST} an action
     *
     * @param wiki       The Wiki to work on.
     * @param action     The type of action to perform. This is the literal API action
     * @param applyToken Set true to apply {@code wiki}'s edit token
     * @param form       The form data to post. This should not be URL-encoded
     * @return True on success
     */
    protected static Tuple<ActionResult, JsonObject> postAction(Wiki wiki, String action, boolean applyToken, Map<String, String> form) {
        Map<String, String> fl = FL.produceMap("format", "json");
        if (applyToken) {
            fl.put("token", wiki.getWikiConfiguration().getToken());
        }

        fl.putAll(form);

        try {
            JsonObject result = JsonParser.parseString(wiki.getApiclient().basicPOST(FL.produceMap("action", action), fl).body().string()).getAsJsonObject();
            if (wiki.getWikiConfiguration().isDebug()) {
                wiki.getWikiConfiguration().getLog().debug(wiki, GSONP.getGsonPrettyPrint().toJson(result));
            }

            return new Tuple<>(ActionResult.wrap(result, action), result);
        } catch (Exception e) {
            LOG.error("Error during posting action or parsing json.", e);
            return new Tuple<>(ActionResult.NONE, new JsonObject());
        }
    }

    /**
     * Adds text to a page.
     *
     * @param wiki    The Wiki to work on.
     * @param title   The title to edit.
     * @param text    The text to add.
     * @param summary The edit summary to use
     * @param append  Set True to append text, or false to prepend text.
     * @return True on success
     */
    protected static boolean addText(Wiki wiki, String title, String text, String summary, boolean append) {
        wiki.getWikiConfiguration().getLog().info(wiki, "Adding text to " + title);

        Map<String, String> parameterList = FL.produceMap(VAR_TITLE, title, append ? "appendtext" : "prependtext", text, "summary", summary);
        if (wiki.getWikiConfiguration().isBot()) {
            parameterList.put("bot", "");
        }

        return postAction(wiki, "edit", true, parameterList).getValue1() == ActionResult.SUCCESS;
    }

    /**
     * Edits a page.
     *
     * @param wiki    The Wiki to work on.
     * @param title   The title to edit
     * @param text    The text to replace the text of {@code title} with.
     * @param summary The edit summary to use
     * @return True on success.
     */
    protected static boolean edit(Wiki wiki, String title, String text, String summary) {
        wiki.getWikiConfiguration().getLog().info(wiki, "Editing " + title);

        Map<String, String> parameterList = FL.produceMap(VAR_TITLE, title, "text", text, "summary", summary);
        if (wiki.getWikiConfiguration().isBot()) {
            parameterList.put("bot", "");
        }

        for (int i = 0; i < 5; i++) {
            switch (postAction(wiki, "edit", true, parameterList).getValue1()) {
                case SUCCESS:
                    return true;
                case RATELIMITED:
                    try {
                        wiki.getWikiConfiguration().getLog().fyi(wiki, "Ratelimited by server, sleeping 10 seconds");
                        Thread.sleep(10000);
                    } catch (Exception e) {
                        LOG.error("Error during interrupting thread.", e);
                        return false;
                    }
                    break;
                case PROTECTED:
                    wiki.getWikiConfiguration().getLog().error(wiki, title + " is protected, cannot edit.");
                    return false;
                default:
                    wiki.getWikiConfiguration().getLog().warn(wiki, "Got an error, retrying: " + i);
            }
        }

        wiki.getWikiConfiguration().getLog().error(wiki, String.format("Could not edit '%s', aborting.", title));
        return false;
    }

    /**
     * Deletes a page. Wiki must be logged in and have administrator permissions for this to succeed.
     *
     * @param wiki   The Wiki to work on.
     * @param title  The title to delete
     * @param reason The log summary to use
     * @return True on success
     */
    protected static boolean delete(Wiki wiki, String title, String reason) {
        wiki.getWikiConfiguration().getLog().info(wiki, "Deleting " + title);
        return postAction(wiki, "delete", true, FL.produceMap(VAR_TITLE, title, "reason", reason)).getValue1() == ActionResult.NONE;
    }

    /**
     * Undelete a page. Wiki must be logged in and have administrator permissions for this to succeed.
     *
     * @param wiki   The Wiki to work on.
     * @param title  The title to delete
     * @param reason The log summary to use
     * @return True on success
     */
    protected static boolean undelete(Wiki wiki, String title, String reason) {
        wiki.getWikiConfiguration().getLog().info(wiki, "Restoring " + title);

        for (int i = 0; i < 10; i++) {
            if (postAction(wiki, "undelete", true, FL.produceMap(VAR_TITLE, title, "reason", reason)).getValue1() == ActionResult.NONE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Purges the cache of pages.
     *
     * @param wiki   The Wiki to work on.
     * @param titles The title(s) to purge.
     */
    protected static void purge(Wiki wiki, List<String> titles) {
        wiki.getWikiConfiguration().getLog().info(wiki, "Purging: " + titles);

        Map<String, String> pl = FL.produceMap("titles", FL.pipeFence(titles));
        postAction(wiki, "purge", false, pl);
    }

    /**
     * Uploads a file. Caution: overwrites files automatically.
     *
     * @param wiki        The Wiki to work on.
     * @param title       The title to upload the file to, excluding the {@code File:} prefix.
     * @param description The text to put on the newly uploaded file description page
     * @param summary     The edit summary to use when uploading a new file.
     * @param file        The Path to the file to upload.
     * @return True on success.
     */
    protected static boolean upload(Wiki wiki, String title, String description, String summary, Path file) {
        wiki.getWikiConfiguration().getLog().info(wiki, "Uploading " + file);

        try {
            ChunkManager cm = new ChunkManager(file);

            String filekey = null;
            String fileName = file.getFileName().toString();

            Chunk chunk;
            while ((chunk = cm.nextChunk()) != null) {
                wiki.getWikiConfiguration().getLog().fyi(wiki, String.format("Uploading chunk [%d of %d] of '%s'", cm.getChunkCount(), cm.getTotalChunks(), file));

                Map<String, String> parameterList = FL.produceMap("format", "json", "filename", title, "token", wiki.getWikiConfiguration().getToken(), "ignorewarnings", "1",
                        "stash", "1", "offset", "" + chunk.offset, "filesize", "" + chunk.filesize);
                if (filekey != null) {
                    parameterList.put(VAR_FILEKEY, filekey);
                }

                for (int i = 0; i < 5; i++) {
                    try {
                        Response response = wiki.getApiclient().multiPartFilePOST(FL.produceMap("action", VAR_UPLOAD), parameterList, fileName, chunk.getBinaryData());
                        if (!response.isSuccessful()) {
                            wiki.getWikiConfiguration().getLog().error(wiki, "Bad response from server: " + response.code());
                            continue;
                        }

                        filekey = GSONP.getString(JsonParser.parseString(response.body().string()).getAsJsonObject().getAsJsonObject(VAR_UPLOAD), VAR_FILEKEY);
                        if (filekey != null) {
                            break;
                        }
                    } catch (Exception e) {
                        wiki.getWikiConfiguration().getLog().error(wiki, "Encountered an error, retrying - " + i);
                        LOG.error("", e);
                    }
                }
            }

            for (int i = 0; i < 3; i++) {
                wiki.getWikiConfiguration().getLog().info(wiki, String.format("Unstashing '%s' as '%s'", filekey, title));

                if (postAction(wiki, VAR_UPLOAD, true, FL.produceMap("filename", title, "text", description, "comment", summary, VAR_FILEKEY, filekey,
                        "ignorewarnings", "true")).getValue1() == ActionResult.SUCCESS) {
                    return true;
                }

                wiki.getWikiConfiguration().getLog().error(wiki, "Encountered an error while unstashing, retrying - " + i);
            }

            return false;
        } catch (Exception e) {
            LOG.error("", e);
            return false;
        }
    }

    /**
     * Represents the result of an action POSTed to a Wiki
     *
     * @author Fastily
     */
    protected enum ActionResult {
        /**
         * Used for success responses
         */
        SUCCESS,

        /**
         * Catch-all, used for unlisted/other errors
         */
        ERROR,

        /**
         * If no result could be determined.
         */
        NONE,

        /**
         * Error, if the request had an expired/invalid token.
         */
        BADTOKEN,

        /**
         * Error, if the request was missing a valid token.
         */
        NOTOKEN,

        /**
         * Error, if the user lacks permission to perform an action.
         */
        PROTECTED,

        /**
         * Error, if the action could not be completed due to being rate-limited by Wiki.
         */
        RATELIMITED;

        /**
         * Parses and wraps the response from a POST to the server in an ActionResult.
         *
         * @param response The json response from the server
         * @param action   The name of the action which produced this response. e.g. {@code edit}, {@code delete}
         * @return An ActionResult representing the response result of the query.
         */
        private static ActionResult wrap(JsonObject response, String action) {
            try {
                if (response.has(action)) {
                    if ("Success".equals(GSONP.getString(response.getAsJsonObject(action), "result"))) {
                        return SUCCESS;
                    } else if ("NeedToken".equals(GSONP.getString(response.getAsJsonObject(action), "result"))) {
                        return NOTOKEN;
                    } else {
                        if (LOG.isErrorEnabled()) {
                            LOG.error(String.format("Something isn't right.  Got back '%s', missing a 'result'?%n", GSONP.getGson().toJson(response)));
                        }
                    }
                } else if (response.has("error")) {
                    switch (GSONP.getString(response.getAsJsonObject("error"), "code")) {
                        case "notoken":
                            return NOTOKEN;
                        case "badtoken":
                            return BADTOKEN;
                        case "cascadeprotected":
                        case "protectedpage":
                            return PROTECTED;
                        default:
                            return ERROR;
                    }
                }
            } catch (Exception e) {
                // nothing to handle
            }

            return NONE;
        }
    }

    /**
     * Creates and manages Chunk Objects for {@link WAction#upload(Wiki, String, String, String, Path)}.
     *
     * @author Fastily
     */
    @Getter
    private static final class ChunkManager {
        /**
         * The default chunk size is 4 Mb
         */
        private static final int CHUNKSIZE = 1024 * 1024 * 4;

        /**
         * The source file stream
         */
        private BufferedSource src;

        /**
         * The current Chunk offset, in bytes
         */
        private long offset = 0;

        /**
         * The file size (in bytes) of the file being uploaded
         */
        private final long filesize;

        /**
         * The total number of Chunk objects to upload
         */
        private final long totalChunks;

        /**
         * Counts the number of chunks created so far.
         */
        private int chunkCount = 0;

        /**
         * Creates a new Chunk Manager. Create a new ChunkManager for every upload.
         *
         * @param fn The local file to upload
         * @throws IOException I/O error.
         */
        private ChunkManager(Path fn) throws IOException {
            filesize = Files.size(fn);
            src = Okio.buffer(Okio.source(fn, StandardOpenOption.READ));
            totalChunks = filesize / CHUNKSIZE + ((filesize % CHUNKSIZE) > 0 ? 1 : 0);
        }

        /**
         * Determine if there are still Chunk objects to upload.
         *
         * @return True if there are still Chunk objects to upload.
         */
        private boolean has() {
            return offset < filesize;
        }

        /**
         * Create and return the next sequential Chunk to upload.
         *
         * @return The next sequential Chunk to upload, or null on error or if there are no more chunks to upload.
         */
        private Chunk nextChunk() {
            if (!has()) {
                return null;
            }
            Chunk c;

            try {
                c = new Chunk(offset, filesize, ++chunkCount == totalChunks ? src.readByteArray() : src.readByteArray(CHUNKSIZE));

                offset += CHUNKSIZE;

                if (!has()) {
                    src.close();
                }

                return c;

            } catch (Exception e) {
                LOG.error("Error during closing bytearray.", e);
                return null;
            }
        }
    }

    /**
     * Represents an indidual chunk to upload
     *
     * @author Fastily
     */
    @Getter
    private static final class Chunk {
        /**
         * The offset and filesize (both in bytes)
         */
        private final long offset;
        private final long filesize;

        /**
         * The raw binary data for this Chunk
         */
        private final byte[] binaryData;

        /**
         * Creates a new Chunk to upload
         *
         * @param offset     The byte offset of this Chunk
         * @param filesize   The total file size of the file this Chunk belongs to
         * @param binaryData The raw binary data contained by this chunk
         */
        private Chunk(long offset, long filesize, byte[] binaryData) {
            this.offset = offset;
            this.filesize = filesize;

            this.binaryData = binaryData;
        }
    }
}