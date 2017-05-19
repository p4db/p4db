package org.netarch.odb.model;

public class ParameterModel {
    private final ParameterModelType type;
    private final int constData;
    private final String runtimedata;
    private final FieldModel fieldModel;
    private final HeaderModel headerModel;


    /**
     * Create a const parameter
     *
     * @param constData const data
     */
    ParameterModel(int constData) {
        this.type = ParameterModelType.CONST;
        this.headerModel = null;
        this.fieldModel = null;
        this.runtimedata = null;
        this.constData = constData;
    }

    /**
     * Create a runtime data parameter.
     *
     * @param runtimeData run time data
     */
    ParameterModel(String runtimeData) {
        this.type = ParameterModelType.RUNTIME_DATA;
        this.headerModel = null;
        this.fieldModel = null;
        this.runtimedata = runtimeData;
        this.constData = 0;
    }

    /**
     * Create a filed parameter.
     *
     * @param fieldModel field model
     */
    ParameterModel(FieldModel fieldModel) {
        if (fieldModel.isMetadata()) {
            this.type = ParameterModelType.METADATA_FIELD;
        } else {
            this.type = ParameterModelType.PACKET_FIELD;
        }
        this.fieldModel = fieldModel;
        this.headerModel = null;
        this.runtimedata = null;
        this.constData = 0;
    }

    /**
     * Create a header model parameter.
     *
     * @param headerModel heaer model
     */
    ParameterModel(HeaderModel headerModel) {
        if (headerModel.isMetadata()) {
            this.type = ParameterModelType.METADATA_HEADER;
        } else {
            this.type = ParameterModelType.PACKET_HEADER;
        }
        this.fieldModel = null;
        this.headerModel = headerModel;
        this.runtimedata = null;
        this.constData = 0;
    }

    /**
     * Get the type of the parameter.
     *
     * @return parameter type
     */
    public ParameterModelType getType() {
        return type;
    }

    /**
     * Get the field model.
     *
     * @return field mode
     */
    public FieldModel getFieldModel() {
        return fieldModel;
    }

    /**
     * Get the header model.
     *
     * @return header model
     */
    public HeaderModel getHeaderModel() {
        return headerModel;
    }

    /**
     * get the const data.
     *
     * @return const data
     */
    public int getConstData() {
        return constData;
    }

    /**
     * Get the run time data.
     *
     * @return runtime data
     */
    public String getRuntimedata() {
        return runtimedata;
    }

    public enum ParameterModelType {
        /**
         * Const parameter.
         */
        CONST,

        /**
         * Packet field.
         */
        PACKET_FIELD,

        /**
         * Metadata field.
         */
        METADATA_FIELD,

        /**
         * Header
         */
        PACKET_HEADER,

        /**
         * Metadata header.
         */
        METADATA_HEADER,

        /**
         * Runtime data.
         */
        RUNTIME_DATA,

        UNDEFINED;
    }
}
