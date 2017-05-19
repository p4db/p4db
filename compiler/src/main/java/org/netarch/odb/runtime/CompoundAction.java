package org.netarch.odb.runtime;

import com.google.common.collect.Lists;

import java.util.List;

public class CompoundAction {
    private String name;
    private List<Data> parameters;

    public CompoundAction(String name) {
        this.name = name;
        this.parameters = Lists.newArrayList();
    }

    /**
     * Get name of the action.
     *
     * @return name of the compound action
     */
    public String getName() {
        return name;
    }

    /**
     * Add parameters to the compound action.
     *
     * @param parameter action parameter
     * @return this
     */
    public CompoundAction addParameter(Data parameter) {
        this.parameters.add(parameter);
        return this;
    }

    /**
     * Get parameters.
     *
     * @return parameters
     */
    public List<Data> getParameters() {
        return parameters;
    }
}
