package genericqueue;

import java.util.List;

/**
 * A queue of (name, value) pairs. Names are non-null strings and uniquely
 * identify entries; each name appears at most once. Values have type V and
 * may be null. The first pair in the list (at index 0) is the front of the
 * queue.
 *
 * For example, a GenericQueue<String> might contain
 *   [("Lawrence", "what is an ADT?"),
 *    ("Faraz", "will this be on the final exam?"),
 *    ("Owen", null)]
 * representing a queue of three entries, the last with a null value.
 *
 * Implementations must use O(n) space, where n is the current size of the
 * queue.
 *
 * @param <V> the value type
 */
public interface GenericQueue<V> {
    /**
     * An immutable (name, value) pair.
     *
     * We use a Java record for this. A record is an immutable class whose
     * fields, constructor, getter methods, equals, hashCode, and toString are
     * all generated automatically. For example, the getter method for the name
     * field is called `name()` (not `getName()`).
     *
     * Nested records in a generic interface are implicitly static, so Entry
     * carries its own type parameter rather than inheriting V from the
     * enclosing interface.
     *
     * @param <V>   the value type
     * @param name  the entry's name
     * @param value the entry's value, or null
     */
    public static record Entry<V>(String name, V value) {}

    /**
     * Returns the number of entries currently in the queue.
     *
     * Runs in O(1) time.
     *
     * @return the number of entries in this queue
     */
    public int size();

    /**
     * Returns whether the queue is empty.
     *
     * Runs in O(1) time.
     *
     * @return true if and only if this queue contains no entries
     */
    public boolean isEmpty();

    /**
     * Adds an entry to the back of the queue.
     *
     * Runs in O(1) time.
     *
     * @param name  the entry's name; must be non-null and must not already be
     *              in the queue
     * @param value the entry's value, or null
     * @modifies obj
     * @effects appends (name, value) to the back of the queue
     */
    public void enqueue(String name, V value);

    /**
     * Removes and returns the entry at the front of the queue.
     *
     * Runs in O(1) time.
     *
     * @requires the queue to be non-empty
     * @modifies obj
     * @effects removes the front entry from the queue
     * @return the Entry that was at the front of this queue
     */
    public Entry<V> dequeue();

    /**
     * Removes the entry with the given name from the queue, if present.
     *
     * Runs in O(n) time, where n is the current size of the queue.
     *
     * @param name the name of the entry to remove; must be non-null
     * @modifies obj
     * @effects removes the entry with the given name, if any; otherwise leaves
     *          the queue unchanged
     * @return true if an entry was found and removed, false otherwise
     */
    public boolean remove(String name);

    /**
     * Inserts a new entry into the queue immediately after an existing entry.
     *
     * Runs in O(n) time, where n is the current size of the queue.
     *
     * @param existingName the name of the entry already in the queue after
     *                     which the new entry will be inserted; must be
     *                     non-null and already present in the queue
     * @param name         the new entry's name; must be non-null and must
     *                     *not* already be in the queue
     * @param value        the new entry's value, or null
     * @modifies obj
     * @effects inserts (name, value) immediately after the entry for
     *          existingName
     */
    public void insertAfter(String existingName, String name, V value);

    /**
     * Returns the entry at the front of the queue without removing it.
     *
     * Runs in O(1) time.
     *
     * @return the Entry at the front of this queue, or null if the queue is empty
     */
    public Entry<V> peek();

    /**
     * Returns whether an entry with the given name is in the queue.
     *
     * Runs in O(log n) time, where n is the current size of the queue.
     *
     * @param name the name to look for; must be non-null
     * @return true if and only if an entry with the given name is in this queue
     */
    public boolean contains(String name);

    /**
     * Finds the index of the entry with the given name.
     *
     * Runs in O(log n) time, where n is the current size of the queue.
     *
     * @param name the name to look for; must be non-null
     * @return the 0-based index of the entry with the given name, or -1 if no
     *         such entry is in the queue
     */
    public int indexOfName(String name);

    /**
     * Returns the entries in the queue in order from front to back. The
     * returned list is independent of the queue: modifying it has no effect
     * on the queue, and subsequent mutations of the queue have no effect on
     * it.
     *
     * @return a list of entries that represent the queue
     */
    public List<Entry<V>> entries();

    /**
     * Returns the entry with the given name.
     *
     * Runs in O(log n) time, where n is the current size of the queue.
     *
     * @param name the entry's name; must be non-null and present in the queue
     * @return the Entry with the given name
     */
    public Entry<V> getEntry(String name);

    /**
     * Updates the value for the entry with the given name. Does not change
     * the entry's position in the queue.
     *
     * Runs in O(log n) time, where n is the current size of the queue.
     *
     * @param name  the entry's name; must be non-null and present in the queue
     * @param value the new value, or null
     * @modifies obj
     * @effects replaces the value for the entry with the given name
     */
    public void updateValue(String name, V value);

    /**
     * Changes the name of an entry already in the queue, preserving its
     * position and its value.
     *
     * If oldName equals newName, then oldName is required to be present in
     * the queue, but the effect of the method is to do nothing.
     *
     * Runs in O(log n) time, where n is the current size of the queue.
     *
     * @param oldName the current name of the entry; must be non-null and
     *                present in the queue
     * @param newName the new name; must be non-null and, unless equal to
     *                oldName, must not already be in the queue
     * @modifies obj
     * @effects replaces the entry's name with newName; position and value are
     *          unchanged
     */
    public void rename(String oldName, String newName);
}
