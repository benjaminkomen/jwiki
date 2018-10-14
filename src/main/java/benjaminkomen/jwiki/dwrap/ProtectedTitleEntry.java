package benjaminkomen.jwiki.dwrap;

import lombok.Getter;

import java.util.Objects;

/**
 * Represents an entry obtained from the {@code protectedtitles} API module.
 *
 * @author Fastily
 */
@Getter
public class ProtectedTitleEntry extends DataEntry {
    /**
     * The protection level
     */
    private String level;

    /**
     * Constructor, creates a ProtectedTitleEntry with all null fields.
     */
    protected ProtectedTitleEntry() {
        // no-args constructor
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ProtectedTitleEntry &&
                Objects.equals(level, ((ProtectedTitleEntry) other).level) &&
                super.equals(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level) + super.hashCode();
    }

    @Override
    public String toString() {
        return "ProtectedTitleEntry{" +
                "level='" + level + '\'' +
                '}';
    }
}