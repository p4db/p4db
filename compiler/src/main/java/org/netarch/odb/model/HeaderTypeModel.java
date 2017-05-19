package org.netarch.odb.model;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;

public class HeaderTypeModel {
    private final String name;
    private final int id;
    private final HashMap<String, FieldTypeModel> fields = Maps.newHashMap();

    private int bitLength;

    /**
     * Creates a new header type model.
     *
     * @param name       name
     * @param id         id
     * @param fieldTypes fields
     */
    HeaderTypeModel(String name, int id, List<FieldTypeModel> fieldTypes) {
        this.name = name;
        this.id = id;
        fieldTypes.forEach(f -> addFieldType(f));
    }

    /**
     * Get the name of the header type.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the id of the header type.
     */
    public int getId() {
        return id;
    }

    /**
     * Get the field type model from the header type.
     *
     * @param name name of the field
     */
    public FieldTypeModel getField(String name) {
        return fields.get(name);
    }


    /**
     * Get fields.
     */
    public List<FieldTypeModel> getFields() {
        return ImmutableList.copyOf(fields.values());
    }

    public void addFieldType(FieldTypeModel fieldTypeModel) {
        fieldTypeModel.setBitOffset(this.bitLength);
        this.bitLength += fieldTypeModel.getBitWidth();
        this.fields.put(fieldTypeModel.getName(), fieldTypeModel);
    }

    public HeaderModel toHeader(String name, int id, boolean isMetadata) {
        return new HeaderModel(name, id, this, isMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, id, fields);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final HeaderTypeModel other = (HeaderTypeModel) obj;
        return Objects.equal(this.name, other.name)
                && Objects.equal(this.id, other.id)
                && Objects.equal(this.fields, equals(fields));

    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", name)
                .add("id", name).add("fields", fields).toString();
    }
}
