package org.netarch.odb.model;

import com.google.common.base.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

public class FieldModel {
    private final HeaderModel header;
    private final FieldTypeModel type;
    private final int bitOffset;

    protected FieldModel(HeaderModel header, FieldTypeModel type, int bitOffset) {
        this.header = header;
        this.type = type;
        this.bitOffset = bitOffset;
    }

    /**
     * Get the header of the field.
     *
     * @return header object
     */
    public HeaderModel getHeader() {
        return header;
    }

    /**
     * Get header name.
     *
     * @return header name
     */
    public String getHeaderName() {
        return this.header.getName();
    }

    /**
     * Get the type of the field.
     *
     * @return field type
     */
    public FieldTypeModel getType() {
        return type;
    }

    /**
     * Get the length of the field.
     */
    public int getLength() {
        return type.getBitWidth();
    }

    /**
     * Get the name of the field.
     */
    public String getName() {
        return type.getName();
    }

    public boolean isMetadata() {
        return this.header.isMetadata();
    }

    /**
     * Get bit offset from the start of the packet.
     *
     * @return bit offset
     */
    public int getBitOffset() {
        return bitOffset;
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(header, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final FieldModel other = (FieldModel) obj;
        return Objects.equal(this.header, other.header)
                && Objects.equal(this.type, other.type);
    }


    @Override
    public String toString() {
        return toStringHelper(this).add("header", header)
                .add("type", type).toString();
    }
}
