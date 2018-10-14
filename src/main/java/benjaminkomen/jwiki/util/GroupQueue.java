package benjaminkomen.jwiki.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A simple read-only Queue that allows multiple items to be polled at once.
 *
 * @param <T> The type of Object contained in the queue.
 * @author Fastily
 */
public class GroupQueue<T> {
    /**
     * The backing data structure.
     */
    private List<T> backingList;

    /**
     * The size, start, end, and maximum size of polls.
     */
    private int size;
    private int start = 0;
    private int end;
    private int maxPoll;

    /**
     * Constructor, creates a new GroupQueue.
     *
     * @param backingList The backing ArrayList to use. This will not be modified.
     * @param maxPoll     The maximum number of elements to poll at once.
     */
    public GroupQueue(Collection<T> backingList, int maxPoll) {
        this.backingList = backingList instanceof ArrayList<?>
                ? (ArrayList<T>) backingList
                : new ArrayList<>(backingList);
        size = backingList.size();
        end = maxPoll;

        this.maxPoll = maxPoll;
    }

    /**
     * Polls this Queue and returns &le; {@code maxPoll} elements.
     *
     * @return An ArrayList with the first {@code maxPoll} elements if possible. Returns the empty list if there is
     * nothing left.
     */
    public List<T> poll() {
        if (!has()) {
            return Collections.emptyList();
        }

        if (size - start < maxPoll) {
            end = size;
        }

        List<T> temp = backingList.subList(start, end);

        start += maxPoll;
        end += maxPoll;

        return temp;
    }

    /**
     * Determines whether there are elements remaining in the queue.
     *
     * @return True if there are elements left in the queue.
     */
    public boolean has() {
        return start < size;
    }
}