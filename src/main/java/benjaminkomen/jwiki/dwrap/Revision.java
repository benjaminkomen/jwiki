package benjaminkomen.jwiki.dwrap;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.Objects;

/**
 * Represents a single revision in the history of a page.
 *
 * @author Fastily
 */
@Getter
public class Revision extends DataEntry {
    /**
     * The text of this revision
     */
    @SerializedName("*")
    private String text;

    /**
     * Constructor, creates a Revision with all null fields.
     */
    protected Revision() {
        // no-args constructor
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Revision &&
                Objects.equals(text, ((Revision) other).text) &&
                super.equals(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text) + super.hashCode();
    }

    @Override
    public String toString() {
        return "Revision{" +
                "text='" + text + '\'' +
                '}';
    }
}