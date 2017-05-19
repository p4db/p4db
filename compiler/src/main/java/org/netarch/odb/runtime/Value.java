package org.netarch.odb.runtime;

import java.nio.ByteBuffer;

public class Value {
    private byte[] data;
    private int bitWidth;

    private Value(byte[] data) {
        this.data = data;
        this.bitWidth = data.length * 8;
    }

    private Value(byte[] data, int bitWidth) {
        this.data = data;
        this.bitWidth = bitWidth;

    }

    /**
     * Create a value with specific value.
     *
     * @param bitLength bit length
     * @return value with specific length
     */
    public static Value createValue(int bitLength) {
        return new Value(new byte[(bitLength + 7) / 8], bitLength);
    }


    /**
     * Create a value from the hex string.
     *
     * @param hexString hex string
     * @return a value
     */
    public static Value createValueFromHex(String hexString) {
        if (hexString == null || hexString.length() < 2) {
            return null;
        }

        int bitWidth = (hexString.length() - 2) * 4;
        byte[] data = new byte[hexString.length() / 2];

        for (int i = 2; i < hexString.getBytes().length; i++) {
            char c = hexString.charAt(i);
            if (i % 2 == 1) {
                if (c <= '9' && c >= '0') {
                    data[i / 2 - 1] |= (byte) (c - '0');
                } else if (c <= 'F' && c >= 'A') {
                    data[i / 2 - 1] |= (byte) (c - 'A' + 10);
                } else if (c <= 'f' && c >= 'a') {
                    data[i / 2 - 1] |= (byte) (c - 'a' + 10);
                } else {
                    throw new RuntimeException("Error code: " + hexString);
                }
            } else {
                if (c <= '9' && c >= '0') {
                    data[i / 2 - 1] |= (byte) ((c - '0') << 4);
                } else if (c <= 'F' && c >= 'A') {
                    data[i / 2 - 1] |= (byte) ((c - 'A' + 10) << 4);
                } else if (c <= 'f' && c >= 'a') {
                    data[i / 2 - 1] |= (byte) ((c - 'a' + 10) << 4);
                } else {
                    throw new RuntimeException("Error code: " + hexString);
                }
            }
        }
        return new Value(data, bitWidth);
    }

    /**
     * Create a value from the hex string.
     *
     * @param hexString hex string
     * @return a value
     */
    public static Value createValueFromHexWithPrefix(String hexString, int fullLength) {
        if (hexString == null || hexString.length() < 2 || fullLength == 0) {
            return null;
        }

        int bitWidth = (hexString.length() - 2) * 4;
        byte[] data = new byte[fullLength];
        int offset = fullLength - hexString.length() / 2;
        for (int i = 2; i < hexString.getBytes().length; i++) {
            char c = hexString.charAt(i);
            if (i % 2 == 1) {
                if (c <= '9' && c >= '0') {
                    data[i / 2 + offset] |= (byte) (c - '0');
                } else if (c <= 'F' && c >= 'A') {
                    data[i / 2 + offset] |= (byte) (c - 'A' + 10);
                } else if (c <= 'f' && c >= 'a') {
                    data[i / 2 + offset] |= (byte) (c - 'a' + 10);
                } else {
                    throw new RuntimeException("Error code: " + hexString);
                }
            } else {
                if (c <= '9' && c >= '0') {
                    data[i / 2 + offset] |= (byte) ((c - '0') << 4);
                } else if (c <= 'F' && c >= 'A') {
                    data[i / 2 + offset] |= (byte) ((c - 'A' + 10) << 4);
                } else if (c <= 'f' && c >= 'a') {
                    data[i / 2 + offset] |= (byte) ((c - 'a' + 10) << 4);
                } else {
                    throw new RuntimeException("Error code: " + hexString);
                }
            }
        }
        return new Value(data, bitWidth);
    }


    /**
     * Create byte array value.
     *
     * @param value array value
     * @return Value
     */
    public static Value createArrayValue(byte[] value) {
        byte[] data = new byte[value.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.put(value);
        return new Value(data);
    }

    /**
     * Create short value.
     *
     * @param value byte value
     * @return Value
     */
    public static Value createByteValue(byte value) {
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.put(value);
        return new Value(data);
    }

    /**
     * Create short value.
     *
     * @param value int value
     * @return Value
     */
    public static Value createShortValue(short value) {
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.putShort(value);
        return new Value(data);
    }

    /**
     * Create int value.
     *
     * @param value int value
     * @return Value
     */
    public static Value createIntValue(int value) {
        byte[] data = new byte[4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.putInt(value);
        return new Value(data);
    }

    /**
     * Create int value.
     *
     * @param value long value
     * @return Value
     */
    public static Value createLongValue(long value) {
        byte[] data = new byte[8];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.putLong(value);
        return new Value(data);
    }

    public int getBitWidth() {
        if (data == null) {
            return 0;
        }
        return bitWidth;
    }

    /**
     * Mod value.
     *
     * @param data         mod data
     * @param bitLength    mod bit length
     * @param leftBitShift left bit shift
     * @return this
     */
    public Value mod(byte[] data, int bitLength, int leftBitShift) {
        if (leftBitShift < 0
                || (this.data.length * 8) < leftBitShift
                || (data.length * 8) < bitLength) {
            throw new RuntimeException("Value mod error.");
        }

        int leftByteShift = leftBitShift / 8;
        int remainBitShift = leftBitShift % 8;
        int end = (bitLength + remainBitShift + 7) / 8 + leftByteShift;
        byte[] shiftedData = new byte[data.length + 1];
        for (int i = data.length; i >= 1; i--) {
            shiftedData[i] |= (data[i - 1] >> (8 - remainBitShift));
            shiftedData[i - 1] |= (data[i - 1] << remainBitShift);
        }
        byte mask = (byte) (((1 << remainBitShift) - 1) << (8 - remainBitShift));

        this.data[leftByteShift] = (byte) ((this.data[leftByteShift] & mask) | shiftedData[0]);
        bitLength /= 8;
        for (int i = leftByteShift + 1; i < end; i++) {
            mask = bitLength >= 8 ? (byte) (0xFF) : (byte) ((1 << bitLength) - 1);
            this.data[i] = (byte) ((this.data[i] & mask) | shiftedData[i - leftByteShift]);
        }
        return this;
    }

    public Value mod(Value value, int leftBitShift) {
        return mod(value.getValue(), value.getBitWidth(), leftBitShift);
    }

    /**
     * Get value as byte array.
     *
     * @return byte array.
     */
    public byte[] getValue() {
        return data;
    }

    /**
     * Get ByteBuffer.
     *
     * @return byte buffer
     */
    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(data);
    }

    /**
     * Get byte from Value.
     *
     * @return byte
     */
    public byte getByte() {
        return getByteBuffer().get();
    }

    /**
     * Get short from Value.
     *
     * @return short
     */
    public short getShort() {
        return getByteBuffer().getShort();
    }

    /**
     * Get int from Value.
     *
     * @return int
     */
    public int getInt() {
        return getByteBuffer().getInt();
    }

    /**
     * Get long from Value.
     *
     * @return long
     */
    public long getLong() {
        return getByteBuffer().getLong();
    }

    /**
     * Compare with value.
     *
     * @param value a value object
     * @return false if it is not equal to the value, otherwise true.
     */
    private boolean compare(Value value) {
        if (bitWidth != value.bitWidth) {
            return false;
        }

        if (data.length != value.getBitWidth()) {
            return false;
        }

        for (int i = 0; i < data.length; i++) {
            if (data[i] != value.data[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Value)) {
            return false;
        }

        return compare((Value) obj);
    }
}
