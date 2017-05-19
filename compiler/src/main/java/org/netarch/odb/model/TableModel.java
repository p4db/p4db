package org.netarch.odb.model;

import com.google.common.base.Objects;

import java.util.List;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

public class TableModel {
    private final String name;
    private final int id;
    private final String matchType;
    private final String type;
    private final int maxSize;
    private final boolean hasCounters;
    private final boolean hasTimeouts;
    private final List<MatchKeyModel> keys;
    private final Set<ActionModel> actions;
    private final String next;

    /**
     * Creates a new table model.
     *
     * @param name           name
     * @param id             id
     * @param matchType      match type
     * @param type           type
     * @param maxSize        max number of entries
     * @param withCounters   if table has counters
     * @param supportTimeout if table supports aging
     * @param keys           list of match keys
     * @param actions        list of actions
     * @param next           next component
     */
    protected TableModel(String name, int id, String matchType, String type,
                         int maxSize, boolean withCounters, boolean supportTimeout,
                         List<MatchKeyModel> keys, Set<ActionModel> actions, String next) {
        this.name = name;
        this.id = id;
        this.matchType = matchType;
        this.type = type;
        this.maxSize = maxSize;
        this.hasCounters = withCounters;
        this.hasTimeouts = supportTimeout;
        this.keys = keys;
        this.actions = actions;
        this.next = next;
    }

    /**
     * Returns the name of this table.
     *
     * @return a string value
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the id of this table.
     *
     * @return an integer value
     */
    public int getId() {
        return id;
    }

    /**
     * Return the match type of this table.
     *
     * @return a string value
     */
    public String getMatchType() {
        return matchType;
    }

    /**
     * Return the match type of this table.
     *
     * @return a string value
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the maximum number of entries supported by this table.
     *
     * @return an integer value
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Returns true if this table has counters, false otherwise.
     *
     * @return a boolean value
     */
    public boolean hasCounters() {
        return hasCounters;
    }

    /**
     * Returns true if this table supports aging, false otherwise.
     *
     * @return a boolean value
     */
    public boolean hasTimeouts() {
        return hasTimeouts;
    }

    /**
     * Returns the list of match keys supported by this table.
     * The list is ordered accordingly to the model's table definition.
     *
     * @return a list of match keys
     */
    public List<MatchKeyModel> getKeys() {
        return keys;
    }

    /**
     * Get next component of the table. If the table is at the end of the
     * pipeline, null will be returned.
     *
     * @return next component (table or condition).
     */
    public String getNext() {
        return next;
    }

    /**
     * Returns the set of actions supported by this table.
     *
     * @return a list of actions
     */
    public Set<ActionModel> getActions() {
        return actions;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, id, matchType, type, maxSize, hasCounters,
                hasTimeouts, keys, actions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TableModel other = (TableModel) obj;
        return Objects.equal(this.name, other.name)
                && Objects.equal(this.id, other.id)
                && Objects.equal(this.matchType, other.matchType)
                && Objects.equal(this.type, other.type)
                && Objects.equal(this.maxSize, other.maxSize)
                && Objects.equal(this.hasCounters, other.hasCounters)
                && Objects.equal(this.hasTimeouts, other.hasTimeouts)
                && Objects.equal(this.keys, other.keys)
                && Objects.equal(this.actions, other.actions);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("id", id)
                .add("matchType", matchType)
                .add("type", type)
                .add("maxSize", maxSize)
                .add("hasCounters", hasCounters)
                .add("hasTimeouts", hasTimeouts)
                .add("keys", keys)
                .add("actions", actions)
                .toString();
    }

}