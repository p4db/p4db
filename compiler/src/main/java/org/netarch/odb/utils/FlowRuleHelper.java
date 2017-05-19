package org.netarch.odb.utils;

import org.netarch.odb.compiler.Vp4Interpreter;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;

public class FlowRuleHelper {
    private static final int FLOW_PRIORITY = 100;
    private final ApplicationId applicationId;
    private final DeviceId deviceId;
    private final int tableId;
    private FlowRule.Builder builder;


    /**
     * Create a flow rule builder with the application, the device id and the table name.
     *
     * @param applicationId application id
     * @param deviceId      device id
     * @param tableName     table name
     */
    public FlowRuleHelper(ApplicationId applicationId, DeviceId deviceId, String tableName) throws RuntimeException {
        this.deviceId = deviceId;
        this.applicationId = applicationId;
        if (Vp4Interpreter.containTable(tableName)) {
            this.tableId = Vp4Interpreter.getTableId(tableName);
            this.builder = flowRuleBuilder();
        } else {
            this.tableId = 0;
            throw new RuntimeException("Table name error : " + tableName);
        }
    }

    /**
     * Create a flow rule builder with the application id, device id and table id.
     *
     * @param applicationId application id
     * @param deviceId      device id
     * @param tableId       target table id
     */
    public FlowRuleHelper(ApplicationId applicationId, DeviceId deviceId, int tableId) {
        this.applicationId = applicationId;
        this.deviceId = deviceId;
        this.tableId = tableId;
        this.builder = flowRuleBuilder();
    }

    public FlowRule build() {
        return this.builder.build();
    }

    /**
     * Set trafficselector.
     *
     * @param trafficSelectorHelper traffic selector helper
     * @return this
     */
    public FlowRuleHelper withSelector(TrafficSelectorHelper trafficSelectorHelper) {
        this.builder.withSelector(DefaultTrafficSelector
                .builder()
                .extension(trafficSelectorHelper.build(), deviceId)
                .build());
        return this;
    }

    /**
     * Set up traffic treatment.
     *
     * @param trafficTreatmentHelper traffic treatment helper
     * @return this
     */
    public FlowRuleHelper withTreatment(TrafficTreatmentHelper trafficTreatmentHelper) {
        this.builder.withTreatment(DefaultTrafficTreatment
                .builder()
                .extension(trafficTreatmentHelper.build(), this.deviceId)
                .build());
        return this;
    }

    /**
     * Set up the priority.
     *
     * @param i priority
     * @return this
     */
    public FlowRuleHelper withPriority(int i) {
        this.builder.withPriority(i);
        return this;
    }


    /**
     * Initialize flow rule builder.
     *
     * @return flow rule builder
     */
    private FlowRule.Builder flowRuleBuilder() {
        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .forTable(tableId)
                .fromApp(applicationId)
                .withPriority(FLOW_PRIORITY)
                .makePermanent();
    }

}
