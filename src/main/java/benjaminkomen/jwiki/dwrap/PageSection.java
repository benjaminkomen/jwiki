package benjaminkomen.jwiki.dwrap;

import benjaminkomen.jwiki.util.GSONP;
import com.google.gson.JsonObject;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a paragraph in a page of wiki text.
 *
 * @author Fastily
 */
@Getter
public class PageSection {
    /**
     * The text in the header of the page section section, excluding {@code =} characters. If this is null, then there
     * was no header, i.e. this is the lead paragraph of a page.
     */
    private final String header;

    /**
     * The header level of the section. If this is {@code -1}, then there was no header, i.e. this is the lead paragraph
     * of a page.
     */
    private final int level;

    /**
     * The full text of the section, including headers
     */
    private final String text;

    /**
     * Constructor, creates a new PageSection.
     *
     * @param header The header text to set
     * @param level  The level to set
     * @param text   The text to set
     */
    private PageSection(String header, int level, String text) {
        this.header = header;
        this.level = level;
        this.text = text;
    }


    @Override
    public boolean equals(Object other) {
        return other instanceof PageSection &&
                Objects.equals(header, ((PageSection) other).header) &&
                Objects.equals(level, ((PageSection) other).level) &&
                Objects.equals(text, ((PageSection) other).text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, level, text) + super.hashCode();
    }

    @Override
    public String toString() {
        return "PageSection{" +
                "level=" + level +
                '}';
    }

    /**
     * Constructor, creates a new PageSection from a JsonObject representing a parsed header.
     *
     * @param jsonObject The JsonObject representing a parsed header. {@code line} and {@code level} will be retrieved and set
     *                   automatically.
     * @param text       The text to associate with this PageSection
     */
    private PageSection(JsonObject jsonObject, String text) {
        this(GSONP.getString(jsonObject, "line"), Integer.parseInt(GSONP.getString(jsonObject, "level")), text);
    }

    /**
     * Creates PageSection objects in the order of parsed header information {@code jsonObjects} using {@code text}.
     *
     * @param jsonObjects Parsed header information
     * @param text        The text associated with the {@code jsonObjects}
     * @return A List of PageSection objects in the same order.
     */
    public static List<PageSection> pageBySection(List<JsonObject> jsonObjects, String text) {
        List<PageSection> psl = new ArrayList<>();
        if (text.isEmpty()) {
            return psl;
        } else if (jsonObjects.isEmpty()) {
            psl.add(new PageSection(null, -1, text));
            return psl;
        }

        JsonObject first = jsonObjects.get(0);
        int firstOffset = offsetOf(first);
        if (firstOffset > 0) {
            // handle headerless leads
            psl.add(new PageSection(null, -1, text.substring(0, firstOffset)));
        }

        if (jsonObjects.size() == 1) {
            // handle 1 section pages
            psl.add(new PageSection(first, text.substring(offsetOf(first))));
        } else {
            // everything else
            for (int i = 0; i < jsonObjects.size() - 1; i++) {
                JsonObject curr = jsonObjects.get(i);
                psl.add(new PageSection(curr, text.substring(offsetOf(curr), offsetOf(jsonObjects.get(i + 1)))));
            }

            JsonObject last = jsonObjects.get(jsonObjects.size() - 1);
            psl.add(new PageSection(last, text.substring(offsetOf(last))));
        }

        return psl;
    }

    /**
     * Gets the {@code offset} value of {@code jsonObject} as an int.
     *
     * @param jsonObject The JsonObject to use
     * @return The {@code offset} value of {@code jsonObject} as an int.
     */
    private static int offsetOf(JsonObject jsonObject) {
        return jsonObject.get("byteoffset").getAsInt();
    }
}