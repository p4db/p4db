package org.netarch.odb.model;

import com.google.common.base.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

public class FieldTypeModel {
    private final String name;
    private final int bitWidth;
    private int bitOffset;

    /**
     * Create a field type model.
     *
     * @param name     name of the field type
     * @param bitWidth bit width of the field width
     */
    FieldTypeModel(String name, int bitWidth) {
        this.name = name;
        this.bitWidth = bitWidth;
        this.bitOffset = 0;
    }

    public int getBitOffset() {
        return bitOffset;
    }

    public void setBitOffset(int bitOffset) {
        this.bitOffset = bitOffset;
    }

    /**
     * Returns the name of this header type field.
     *
     * @return a string value
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the bit width of this header type field.
     *
     * @return an integer value
     */
    public int getBitWidth() {
        return bitWidth;
    }


    /**
     * Get a field from the field type.
     *
     * @param headerModel header of the model
     * @return field
     */
    public FieldModel toFieldModel(HeaderModel headerModel) {
        return new FieldModel(headerModel, this, this.bitOffset);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, bitWidth);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final FieldTypeModel other = (FieldTypeModel) obj;
        return Objects.equal(this.name, other.name)
                && Objects.equal(this.bitWidth, other.bitWidth);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("bitWidth", bitWidth)
                .toString();
    }
}
