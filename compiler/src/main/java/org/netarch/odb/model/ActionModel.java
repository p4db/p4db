package org.netarch.odb.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

public class ActionModel {

    private final String name;
    private final int id;
    private final LinkedHashMap<String, DataModel> runtimeDatas = Maps.newLinkedHashMap();
    private final List<PrimitiveModel> primitiveModels = Lists.newArrayList();

    /**
     * Creates a new action model.
     *
     * @param name         name
     * @param id           id
     * @param runtimeDatas list of runtime data
     */
    protected ActionModel(String name, int id, List<DataModel> runtimeDatas, List<PrimitiveModel> primitiveModels) {
        this.name = name;
        this.id = id;
        runtimeDatas.forEach(r -> this.runtimeDatas.put(r.name(), r));
        primitiveModels.forEach(primitiveModel -> this.primitiveModels.add(primitiveModel));
    }

    /**
     * Returns the name of this action.
     *
     * @return a string value
     */
    public String name() {
        return name;
    }

    /**
     * Returns the id of this action.
     *
     * @return an integer value
     */
    public int id() {
        return id;
    }

    /**
     * Returns this action's runtime data defined by the given name, null
     * if not present.
     *
     * @return runtime data or null
     */
    public DataModel runtimeData(String name) {
        return runtimeDatas.get(name);
    }

    /**
     * Returns an immutable list of runtime data for this action.
     * The list is ordered according to the values defined in the configuration.
     *
     * @return list of runtime data.
     */
    public List<DataModel> runtimeDatas() {
        return ImmutableList.copyOf(runtimeDatas.values());
    }


    /**
     * Get a list of primitives.
     *
     * @return list of primitive models
     */
    public List<PrimitiveModel> getPrimitiveModels() {
        return primitiveModels;
    }


    /**
     * Get action bitmap.
     *
     * @return action bitmap
     */
    public Long getActionBitMap() {
        Long actionBitMap = (long) 0;
        for (PrimitiveModel model : primitiveModels) {
            actionBitMap |= 1 << model.getType().getValue();
        }
        return actionBitMap;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, runtimeDatas);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ActionModel other = (ActionModel) obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.id, other.id)
                && Objects.equals(this.runtimeDatas, other.runtimeDatas);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("id", id)
                .add("runtimeDatas", runtimeDatas)
                .toString();
    }


}
