package org.netarch.odb.runtime;

public class Data {
    private String name;
    private Value value;

    /**
     * Create a specific data with the name.
     *
     * @param name name of the data
     */
    public Data(String name) {
        this.name = name;
        this.value = null;
    }

    /**
     * Create a specific data with the name.
     *
     * @param name name of the data
     */
    public Data(String name, Value value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Create a int data.
     *
     * @param name  name of the data.
     * @param value int value of the data
     * @return the data
     */
    public static Data createIntData(String name, int value) {
        return new Data(name, Value.createIntValue(value));
    }

    /**
     * Create a byte data.
     *
     * @param name  name of the data
     * @param value byte value of the data
     * @return the data
     */
    public static Data createByteData(String name, byte value) {
        return new Data(name, Value.createByteValue(value));
    }

    /**
     * Create a short data.
     *
     * @param name  name of the data
     * @param value short value of the data
     * @return the data
     */
    public static Data createShortData(String name, short value) {
        return new Data(name, Value.createShortValue(value));
    }

    /**
     * Create long data.
     *
     * @param name  name of the long data
     * @param value value of the long data
     * @return the new data
     */
    public static Data createLongData(String name, long value) {
        return new Data(name, Value.createLongValue(value));
    }


    /**
     * Create a data from the value.
     *
     * @param name  name of th data
     * @param value value of the data
     * @return this
     */
    public static Data createData(String name, Value value) {
        return new Data(name, value);
    }

    /**
     * Get the name of the data.
     *
     * @return Name of the data
     */
    public String getName() {
        return name;
    }

    /**
     * Get the value.
     *
     * @return Value
     */
    public Value getValue() {
        return value;
    }

    /**
     * Set the value of the data.
     *
     * @param value Value
     */
    public void setValue(Value value) {
        this.value = value;
    }


}
