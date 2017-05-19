package org.netarch.odb.runtime;

public class FlowKey {
    private FlowKeyType type;
    private String header;
    private String field;
    private String name;
    private Value value;

    public FlowKey(FlowKeyType tyep, String header, String field, Value value) {
        this.type = tyep;
        this.header = header;
        this.field = field;
        this.value = value;
        this.name = this.header + "." + this.field;
    }

    /**
     * Create a header field key.
     *
     * @param header header name
     * @param field  field name
     * @param value  key value
     * @return FlowKey of HEADER_FIELD type
     */
    public static FlowKey buildHeaderFieldKey(String header, String field, Value value) {
        return new FlowKey(FlowKeyType.HEADER_FIELD, header, field, value);
    }

    /**
     * Create a metadata field key.
     *
     * @param header header name
     * @param field  field name
     * @param value  key value
     * @return FlowKey of METADATA_FIELD type.
     */
    public static FlowKey buildMetadataFieldKey(String header, String field, Value value) {
        return new FlowKey(FlowKeyType.METADATA_TYPE, header, field, value);
    }

    /**
     * Get Flow key type.
     *
     * @return flow key type
     */
    public FlowKeyType getType() {
        return type;
    }

    /**
     * Get the value of the key.
     *
     * @return key value
     */
    public Value getValue() {
        return value;
    }

    /**
     * Get field name.
     *
     * @return field name
     */
    public String getFieldName() {
        return field;
    }

    /**
     * Get header name.
     *
     * @return header name
     */
    public String getHeaderName() {
        return header;
    }

    /**
     * Get key name in the format of 'header.field'.
     *
     * @return key name
     */
    public String getKeyName() {
        return this.name;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof FlowKey)) {
            return false;
        }

        FlowKey key = (FlowKey) obj;
        return type == key.type
                && this.name.equals(key.name)
                && this.value.equals(key.value);
    }
}
