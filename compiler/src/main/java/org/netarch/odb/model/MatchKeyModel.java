package org.netarch.odb.model;

import com.google.common.base.Objects;
import org.netarch.odb.runtime.Value;

import static com.google.common.base.MoreObjects.toStringHelper;

public class MatchKeyModel {

    private final MatchKeyType matchType;
    private final FieldModel field;
    private final String name;

    /**
     * Creates a new table key model.
     *
     * @param matchType match type
     * @param field     field instance
     */
    protected MatchKeyModel(MatchKeyType matchType, FieldModel field) {
        this.matchType = matchType;
        this.field = field;
        this.name = this.field.getHeader().getName() + "." + this.field.getName();
    }

    /**
     * Get name of the match key.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get field name of the key
     *
     * @return field name
     */
    public String getFieldName() {
        return this.field.getName();
    }

    /**
     * Returns the match type of this key.
     *
     * @return a string value
     */
    public MatchKeyType matchType() {
        return matchType;
    }

    /**
     * Returns the header field instance matched by this key.
     *
     * @return a header field value
     */
    public FieldModel field() {
        return field;
    }

    public Value getMaskValue() {
        Value mask = Value.createValue(this.field.getLength());
        byte[] data = new byte[(this.field.getLength() + 7) / 8];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) 0xFF;
        }
        mask.mod(data, this.field.getLength(), 0);
        return mask;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(matchType, field);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MatchKeyModel other = (MatchKeyModel) obj;
        return Objects.equal(this.matchType, other.matchType)
                && Objects.equal(this.field, other.field);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("matchType", matchType)
                .add("field", field)
                .toString();
    }

    public enum MatchKeyType {
        /**
         * Exact match type.
         */
        EXACT,
        /**
         * Ternary match type.
         */
        TERNARY,
        /**
         * Longest-prefix match type.
         */
        LPM,
        /**
         * Valid match type.
         */
        VALID,
        /**
         * Undefined match type.
         */
        UNDEFINED;

        public static MatchKeyType getType(String matchTypeStr) {
            switch (matchTypeStr) {
                case "ternary":
                    return TERNARY;
                case "exact":
                    return EXACT;
                case "lpm":
                    return LPM;
                case "valid":
                    return VALID;
                default:
                    return UNDEFINED;
            }
        }
    }
}
