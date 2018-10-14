package benjaminkomen.jwiki.dwrap;

import lombok.Getter;

import java.util.Objects;

/**
 * Represents a contribution made by a user.
 *
 * @author Fastily
 */
@Getter
public class Contrib extends DataEntry {
    /**
     * This contribution's revision id.
     */
    private long revid;

    /**
     * This contribution's parent ID
     */
    private long parentid;

    /**
     * Constructor, creates a Contrib with all null fields.
     */
    private Contrib() {

    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Contrib &&
                Objects.equals(revid, ((Contrib) other).revid) &&
                Objects.equals(parentid, ((Contrib) other).parentid) &&
                super.equals(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revid, parentid) + super.hashCode();
    }

    @Override
    public String toString() {
        return "Contrib{" +
                "revid=" + revid +
                ", parentid=" + parentid +
                '}';
    }
}