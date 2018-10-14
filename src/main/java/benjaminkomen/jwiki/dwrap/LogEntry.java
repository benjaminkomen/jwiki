package benjaminkomen.jwiki.dwrap;

import lombok.Getter;

import java.util.Objects;

/**
 * Represents a MediaWiki Log entry
 *
 * @author Fastily
 */
@Getter
public class LogEntry extends DataEntry {
    /**
     * The log that this Log Entry belongs to. (e.g. 'delete', 'block')
     */
    private String type;

    /**
     * The action that was performed in this log. (e.g. 'restore', 'revision')
     */
    private String action;

    /**
     * Constructor, creates a LogEntry with all null fields.
     */
    protected LogEntry() {

    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LogEntry &&
                Objects.equals(type, ((LogEntry) other).type) &&
                Objects.equals(action, ((LogEntry) other).action) &&
                super.equals(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, action) + super.hashCode();
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "type='" + type + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}