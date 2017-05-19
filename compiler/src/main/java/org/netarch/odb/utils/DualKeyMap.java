package org.netarch.odb.utils;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

public class DualKeyMap<T> {
    private final SortedMap<Integer, T> intMap = Maps.newTreeMap();
    private final Map<String, Integer> strToIntMap = Maps.newHashMap();

    /**
     * Put the object into the map
     *
     * @param name   name of the obejct
     * @param id     id the object
     * @param object object
     * @return thie
     */
    public DualKeyMap put(String name, int id, T object) {
        strToIntMap.put(name, id);
        intMap.put(id, object);
        return this;
    }

    /**
     * Get the object according the id.
     *
     * @param id id
     * @return the object
     */
    public T get(int id) {
        return intMap.get(id);
    }

    /**
     * Get the object according to the name.
     *
     * @param name name
     * @return the object
     */
    public T get(String name) {
        return get(strToIntMap.get(strToIntMap.getOrDefault(name, null)));
    }

    /**
     * Get the object with the least key
     *
     * @return the object.
     */
    public T getWithLeastKey() {
        return intMap.get(intMap.firstKey());
    }

    /**
     * Ge tthe least key.
     *
     * @return least key
     */
    public Integer getLeastKey() {
        return intMap.firstKey();
    }

    /**
     * Get thte largest key.
     *
     * @return largest key
     */
    public Integer getLargestKey() {
        return intMap.lastKey();
    }

    /**
     * Get the object with the largest key.
     *
     * @return the object
     */
    public T getWithLargestKey() {
        return intMap.get(intMap.lastKey());
    }

    /**
     * Get sorted map.
     *
     * @return sorted map
     */
    public SortedMap<Integer, T> sortedMap() {
        return intMap;
    }

    /**
     * Get values.
     *
     * @return value list
     */
    public Collection<T> values() {
        return intMap.values();
    }

    /**
     * Remove the object from the map.
     *
     * @param name object name
     * @return object
     */
    public T remove(String name) {
        if (strToIntMap.get(name) == null) {
            return null;
        }
        return intMap.remove(strToIntMap.remove(name));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(intMap, strToIntMap);
    }

    /**
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DualKeyMap other = (DualKeyMap) obj;
        return Objects.equal(this.intMap, other.intMap)
                && Objects.equal(this.strToIntMap, other.strToIntMap);
    }
}