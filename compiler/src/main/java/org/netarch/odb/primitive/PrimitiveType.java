package org.netarch.odb.primitive;

public enum PrimitiveType {
    /**
     * Add a header.
     */
    ADD_HEADER(1),

    /**
     * Copy a header to somewhere.
     */
    COPY_HEADER(2),

    /**
     * Rremove a header from a packet.
     */
    REMOVE_HEADER(3),

    /**
     * Modify fields.
     */
    MODIFY_FIELD(4),

    /**
     * Add something to a field.
     */
    ADD_TO_FIELD(5),

    /**
     * Add something to something.
     */
    ADD(6),

    /**
     * SUBSTRACT something from the field.
     */
    SUBSTRACT_FROM_FIELD(7),

    /**
     * Substract something.
     */
    SUBSTRACT(8),

    /**
     * And bits.
     */
    BIT_AND(11),

    /**
     * Or bits.
     */
    BIT_OR(12),

    /**
     * Xor bits.
     */
    BIT_XOR(13),

    /**
     * Shift bits left.
     */
    SHIFT_LEFT(14),

    /**
     * Shift bits right.
     */
    SHIFT_RIGHT(15),

    /**
     * Truncate
     */
    TRUNCATE(16),

    /**
     * Drop
     */
    DROP(17),

    /**
     * No operation.
     */
    NO_OP(18),

    /**
     * Push a header into a header stack.
     */
    PUSH(19),

    /**
     * Pop a header from a header stack.
     */
    POP(20),

    /**
     * Count packets or packets.
     */
    COUNT(21),

    /**
     * Execute meter.
     */
    EXECUTE_METER(22),

    /**
     * Read a register.
     */
    REGISTER_READ(23),

    /**
     * Write a register.
     */
    REGISTER_WRITE(24),

    /**
     * Generate a digest of a packet.
     */
    GENERATE_DIGEST(25),

    /**
     * Resubmit a packet.
     */
    RESUBMIT(26),

    /**
     * Recirculate a packet.
     */
    RECIRCULATE(27),

    /**
     * Undefined primitive type.
     */
    UNDEFINED(64);

    private int value;

    /**
     * Create a primitive type with value.
     *
     * @param value value for the primitive type
     */
    private PrimitiveType(int value) {
        this.value = value;
    }

    /**
     * Get a type from the string.
     */
    public static PrimitiveType getPrimitiveType(String typeStr) {
        if (typeStr.equals("modify_field")) {
            return MODIFY_FIELD;
        }

        if (typeStr.equals("drop")) {
            return DROP;
        }

        if (typeStr.equals("no_op")) {
            return NO_OP;
        }

        return UNDEFINED;
    }

    /**
     * Get the value
     *
     * @return value of the primitive.
     */
    public int getValue() {
        return value;
    }
}
