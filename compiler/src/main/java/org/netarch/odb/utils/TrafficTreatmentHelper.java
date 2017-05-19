package org.netarch.odb.utils;

import org.netarch.odb.runtime.CompoundAction;
import org.netarch.odb.runtime.Data;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;

public class TrafficTreatmentHelper {
    private Bmv2ExtensionTreatment.Builder builder;

    /**
     * Create a bmv2 entension treatment.
     *
     * @param configuration bmv2 configuration
     */
    public TrafficTreatmentHelper(Bmv2Configuration configuration) {
        this.builder = Bmv2ExtensionTreatment.builder().forConfiguration(configuration);
    }

    /**
     * Set compound action into traffic treatment.
     *
     * @param action compound action
     * @return this
     */
    public TrafficTreatmentHelper withCoumpoundAction(CompoundAction action) {
        // this.builder.setActionName(action.getName());
        withActionName(action.getName());
        action.getParameters().forEach(data -> {
            addParameter(data);
        });
        return this;
    }

    /**
     * Add compound action name.
     *
     * @param name action name
     * @return this
     */
    private TrafficTreatmentHelper withActionName(String name) {
        this.builder.setActionName(name);
        return this;
    }

    /**
     * Add parameter into traffic treatment.
     *
     * @param data parameter
     * @return this
     */
    private TrafficTreatmentHelper addParameter(Data data) {
        this.builder.addParameter(data.getName(), data.getValue().getValue());
        return this;
    }

    /**
     * Build traffic treatment.
     *
     * @return bmv2 extension treatment
     */
    public Bmv2ExtensionTreatment build() {
        return this.builder.build();
    }
}
