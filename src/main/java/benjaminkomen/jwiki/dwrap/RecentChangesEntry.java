package benjaminkomen.jwiki.dwrap;

import lombok.Getter;

import java.util.Objects;

/**
 * Represents a Recent Changes entry.
 *
 * @author Fastily
 */
@Getter
public class RecentChangesEntry extends DataEntry {
    /**
     * The type of entry this RecentChangesEntry represents (ex: log, edit, new)
     */
    private String type;

    /**
     * Constructor, creates an RecentChangesEntry with all null fields.
     */
    protected RecentChangesEntry() {
        // no-args constructor
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RecentChangesEntry &&
                Objects.equals(type, ((RecentChangesEntry) other).type) &&
                super.equals(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type) + super.hashCode();
    }

    @Override
    public String toString() {
        return "RecentChangesEntry{" +
                "type='" + type + '\'' +
                '}';
    }
}