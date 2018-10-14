package benjaminkomen.jwiki.dwrap;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * Structured data template class.
 *
 * @author Fastily
 */
@Getter
public abstract class DataEntry {
    /**
     * The name of the user who made the contribution.
     */
    private String user;

    /**
     * Title and edit summary.
     */
    private String title;

    /**
     * The edit summary used in this contribution.
     */
    @SerializedName("comment")
    private String summary;

    /**
     * The date and time at which this edit was made.
     */
    private Instant timestamp;

    /**
     * Constructor, creates a DataEntry with all null fields.
     */
    protected DataEntry() {

    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DataEntry &&
                Objects.equals(user, ((DataEntry) other).user) &&
                Objects.equals(title, ((DataEntry) other).title) &&
                Objects.equals(summary, ((DataEntry) other).summary) &&
                Objects.equals(timestamp, ((DataEntry) other).timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, title, summary, timestamp);
    }

    /**
     * Gets a String representation of this DataEntry. Useful for debugging.
     */
    @Override
    public String toString() {
        return String.format("[ user : %s, title : %s, summary : %s, timestamp : %s ]", user, title, summary, timestamp);
    }


}