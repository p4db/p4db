package org.netarch.odb.model;

import com.google.common.collect.Maps;

import java.util.Map;

public class HeaderModel {
    private final String name;
    private final int id;
    private final HeaderTypeModel type;
    private final boolean isMetadata;
    private final Map<String, FieldModel> fieldMap;
    private int bitLength;
    private HeaderModel preHeader;

    /**
     * Creates a new header instance model.
     *
     * @param name     name
     * @param id       id
     * @param type     header type
     * @param metadata if is metadata
     */
    protected HeaderModel(String name, int id, HeaderTypeModel type, boolean metadata) {
        this.name = name;
        this.id = id;
        this.type = type;
        this.isMetadata = metadata;
        this.fieldMap = Maps.newHashMap();

        this.bitLength = 0;
        this.preHeader = null;

        type.getFields().forEach(fieldTypeModel -> {
            fieldMap.put(fieldTypeModel.getName(), fieldTypeModel.toFieldModel(this));
            bitLength += fieldTypeModel.getBitWidth();
        });
    }

    /**
     * Returns the name of this header instance.
     *
     * @return a string value
     */
    public String getName() {
        return name;
    }

    /**
     * Get previous header.
     *
     * @return last header
     */
    public HeaderModel getPreHeader() {
        return preHeader;
    }

    /**
     * Set previous header.
     *
     * @param preHeader previous header
     */
    public void setPreHeader(HeaderModel preHeader) {
        this.preHeader = preHeader;
    }

    /**
     * Return the id of this header instance.
     *
     * @return an integer value
     */
    public int getId() {
        return id;
    }

    /**
     * Get the type of this header instance.
     *
     * @return a header type
     */
    public HeaderTypeModel getType() {
        return type;
    }

    /**
     * Whether the header is  metadata.
     *
     * @return true if yes, or false.
     */
    public boolean isMetadata() {
        return isMetadata;
    }

    /**
     * Get the field model from the fiel name.
     *
     * @param fieldName field name
     * @return field model or null
     */
    public FieldModel getField(String fieldName) {
        return fieldMap.get(fieldName);
    }

    public int getBitOffset() {
        if (this.preHeader == null) {
            return 0;
        } else {
            return this.preHeader.getEndBitOffset();
        }
    }

    public int getEndBitOffset() {
        return getBitOffset() + bitLength;
    }
}
