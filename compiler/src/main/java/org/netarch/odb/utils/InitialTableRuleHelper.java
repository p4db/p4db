package org.netarch.odb.utils;

import org.netarch.odb.runtime.CompoundAction;
import org.netarch.odb.runtime.Data;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowRule;

import static org.netarch.odb.compiler.Vp4Interpreter.ACTION_SET_INITIAL_CONFIG;
import static org.netarch.odb.compiler.Vp4Interpreter.CONFIG_AT_INITIAL;

public class InitialTableRuleHelper {
    private final FlowRuleHelper ruleHelper;
    private final Bmv2Configuration configuration;
    private Byte progId;
    private Byte stageId;
    private Byte matchBitmap;
    private Short policyId;

    /**
     * a TABLE_CONFIG_AT_INITIAL helpers.
     *
     * @param applicationId applcaition id
     * @param deviceId      device id
     */
    public InitialTableRuleHelper(ApplicationId applicationId, DeviceId deviceId, Bmv2Configuration configuration) {
        ruleHelper = new FlowRuleHelper(applicationId, deviceId, CONFIG_AT_INITIAL);
        this.configuration = configuration;
        progId = null;
        stageId = null;
        matchBitmap = null;
        policyId = null;
    }


    public InitialTableRuleHelper setStageId(Byte stageId) {
        this.stageId = stageId;
        return this;
    }

    public InitialTableRuleHelper setMatchBitmap(Byte matchBitmap) {
        this.matchBitmap = matchBitmap;
        return this;
    }

    public InitialTableRuleHelper setPolicyId(Short policyId) {
        this.policyId = policyId;
        return this;
    }

    public InitialTableRuleHelper setProgId(Byte progId) {
        this.progId = progId;
        return this;
    }

    /**
     * Check whether the helper can be built.
     */
    private void check() {
        if (progId == null) {
            throw new RuntimeException("Program ID in InitialTableRuleHelper is NULL.");
        }

        if (stageId == null) {
            throw new RuntimeException("Stage ID in InitialTableRuleHelper is NULL.");
        }

        if (matchBitmap == null) {
            throw new RuntimeException("Match bitmap in InitialTableRuleHelper is NULL.");
        }

        if (policyId == null) {
            throw new RuntimeException("Policy ID in InitialTableRuleHelper is NULL.");
        }
    }

    public FlowRule build() {
        check();
        TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);
        selectorHelper.withPolicyId(policyId)
                .withProgramId((byte) 0)
                .withStageId((byte) 0);

        TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);
        CompoundAction action = new CompoundAction(ACTION_SET_INITIAL_CONFIG);
        action.addParameter(Data.createByteData("progid", progId))
                .addParameter(Data.createByteData("initstage", stageId))
                .addParameter(Data.createByteData("match_bitmap", matchBitmap));

        treatmentHelper.withCoumpoundAction(action);

        ruleHelper.withSelector(selectorHelper).withTreatment(treatmentHelper).withPriority(1);

        return ruleHelper.build();
    }

}
