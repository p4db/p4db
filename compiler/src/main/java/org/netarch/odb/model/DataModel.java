package org.netarch.odb.model;

import com.google.common.base.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Objects.equal;

public class DataModel {
    private final String name;
    private final int bitWidth;

    /**
     * Creates a new runtime data model.
     *
     * @param name     name
     * @param bitWidth bitwidth
     */
    protected DataModel(String name, int bitWidth) {
        this.name = name;
        this.bitWidth = bitWidth;
    }

    /**
     * Return the name of this runtime data.
     *
     * @return a string value
     */
    public String name() {
        return name;
    }

    /**
     * Return the bit width of this runtime data.
     *
     * @return an integer value
     */
    public int bitWidth() {
        return bitWidth;
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
        final DataModel other = (DataModel) obj;
        return equal(this.name, other.name)
                && equal(this.bitWidth, other.bitWidth);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("bitWidth", bitWidth)
                .toString();
    }
}
