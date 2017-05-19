package org.netarch.odb.compiler;

import com.eclipsesource.json.JsonObject;
import org.netarch.odb.model.ProgramModel;
import org.netarch.odb.runtime.Instance;
import org.netarch.odb.service.CompilerService;
import org.netarch.odb.utils.DualKeyMap;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;

import java.util.List;

public class Compiler implements CompilerService {

    protected static final int STAGE_NUM = 10;
    private int instanceCounter;
    private int programCounter;
    private ApplicationId applicationId;
    private DualKeyMap<ProgramModel> progModelMap;
    private DualKeyMap<Instance> instanceMap;
    private FlowRuleService flowRuleService;
    private Bmv2Configuration configuration;

    public Compiler(FlowRuleService flowRuleService, ApplicationId applicationId, Bmv2Configuration configuration) {
        this.flowRuleService = flowRuleService;
        this.progModelMap = new DualKeyMap<>();
        this.instanceMap = new DualKeyMap<>();
        this.applicationId = applicationId;
        this.configuration = configuration;
        this.instanceCounter = 0;
        this.programCounter = 0;
    }

    @Override
    public ProgramModel compile(JsonObject json, String progName) {
        ProgramModel progModel = new ProgramModel(progName, newProgramId(), json);
        progModel.doParse();
        this.progModelMap.put(progName, progModel.getId(), progModel);
        return progModel;
    }

    @Override
    public Instance run(ProgramModel program, DeviceId deviceId, int policyId) {
        Instance instance = new Instance(policyId,
                newInstanceId(),
                program, this, deviceId, applicationId);
        instance.initialize();
        this.instanceMap.put(instance.getName(), instance.getInstanceId(), instance);
        return instance;
    }

    @Override
    public void stop(Instance instance) {
        this.instanceMap.remove(instance.getName());
    }

    public Bmv2Configuration getBmv2Configuration() {
        return this.configuration;
    }

    public ProgramModel getProgramModel(String progName) {
        return progModelMap.get(progName);
    }

    public ProgramModel getProgramModel(int programId) {
        return progModelMap.get(programId);
    }

    /**
     * Install a flow rule into the device.
     *
     * @param rule flow rule
     */
    @Override
    public CompilerService installRule(FlowRule rule) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        builder.add(rule);
        flowRuleService.apply(builder.build());
        return this;
    }

    /**
     * Remove a flow rule from the device.
     *
     * @param rule flow rule
     */
    @Override
    public CompilerService removeRule(FlowRule rule) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        builder.remove(rule);
        flowRuleService.apply(builder.build());
        return this;
    }

    private int newProgramId() {
        return programCounter++;
    }

    private int newInstanceId() {
        return instanceCounter++;
    }

    @Override
    public List<DeviceId> getDevices() {
        return null;
    }

    /**
     * Get current configuration.
     *
     * @return bmv2 configuration
     */
    @Override
    public Bmv2Configuration getConfiguration() {
        return configuration;
    }
}
