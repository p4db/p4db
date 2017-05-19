package org.netarch.odb.runtime;

import org.netarch.odb.compiler.Compiler;
import org.netarch.odb.compiler.Vp4Interpreter;
import org.netarch.odb.model.ActionModel;
import org.netarch.odb.model.FieldModel;
import org.netarch.odb.model.MatchKeyModel;
import org.netarch.odb.model.ParameterModel;
import org.netarch.odb.model.PrimitiveModel;
import org.netarch.odb.model.ProgramModel;
import org.netarch.odb.utils.DualKeyMap;
import org.netarch.odb.utils.FlowRuleHelper;
import org.netarch.odb.utils.TrafficSelectorHelper;
import org.netarch.odb.utils.TrafficTreatmentHelper;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.util.List;

import static org.netarch.odb.compiler.Vp4Interpreter.*;

public class Instance {
    private final String name;
    private final ApplicationId applicationId;
    private final ProgramModel program;
    private final DeviceId deviceId;
    private final int instanceId;
    private final int poliyId;
    private final Bmv2Configuration configuration;
    private Compiler compiler;
    private DualKeyMap<Stage> stageMap;

    /**
     * Create an instance.
     *
     * @param policyId      policy id
     * @param instanceId    instance id
     * @param program       program model
     * @param compiler      flow rule
     * @param deviceId      binding deivce id
     * @param applicationId application id
     */
    public Instance(int policyId,
                    int instanceId,
                    ProgramModel program,
                    Compiler compiler,
                    DeviceId deviceId,
                    ApplicationId applicationId) {

        this.instanceId = instanceId;
        this.program = program;
        this.compiler = compiler;
        this.deviceId = deviceId;
        this.poliyId = policyId;
        this.applicationId = applicationId;
        this.stageMap = new DualKeyMap<>();
        this.configuration = compiler.getConfiguration();
        this.name = program.getName() + "-" + instanceId;

        program.tables().forEach(table -> {
            Stage stage = new Stage(table.getId(), table);
            stageMap.put(stage.getName(), stage.getId(), stage);
        });

    }

    /**
     * Initialize config tables in data plane.
     */
    private void initConfig() {
        Bmv2Configuration configuration = this.compiler.getConfiguration();

        FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, "table_config_at_initial");

        TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

        /* Traffic selector */
        selectorHelper.withPolicyId((short) this.poliyId)
                .withStageId((byte) 0)
                .withProgramId((byte) 0);

        flowRuleHelper.withSelector(selectorHelper);

        TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

        byte matchType = 0;
        Stage stage = stageMap.getWithLeastKey();

        if (stage.isMatchHeader()) {
            matchType |= 4;
        }

        if (stage.isMatchMetadata()) {
            matchType |= 2;
        }

        if (stage.isMatchStdMetadata()) {
            matchType |= 1;
        }

        CompoundAction action = new CompoundAction(CONFIG_AT_INITIAL);
        action.addParameter(Data.createByteData("progid", (byte) this.instanceId))
                .addParameter(Data.createByteData("initstage", (byte) (int) stageMap.getLeastKey()))
                .addParameter(Data.createByteData("match_bitmap", matchType));

        treatmentHelper.withCoumpoundAction(action);

        flowRuleHelper.withTreatment(treatmentHelper);

        compiler.installRule(flowRuleHelper.build());
    }

    public void initialize() {
        initConfig();

    }

    /**
     * Install a rule into the instance.
     *
     * @param rule instace rule
     * @return this
     */
    public Instance addRule(InstanceRule rule) {
        byte stageId = (byte) rule.getStageId();
        Stage stage = stageMap.get(stageId);

        if (stage == null) {
            throw new RuntimeException("Cannot find the stage.");
        }

        Stage nextStage = stageMap.get(stage.getNextStage());
        MatchResult matchResult = stage.computeMatchResult(rule.getKeys());

        ActionModel actionModel = program.getActionModel(rule.getAction().getName());

        if (actionModel == null) {
            throw new RuntimeException("Cannot find the action model");
        }

        /* Match header */
        if (stage.isMatchHeader()) {
            Value headerValue = Value.createValue(stage.getMaxHeaderBitLength());
            Value maskValue = Value.createValue(stage.getMaxHeaderBitLength());

            rule.getHeaderKeys().forEach(flowKey -> {
                MatchKeyModel matchKeyModel = stage.getMatchKey(flowKey.getKeyName());
                if (matchKeyModel != null) {
                    int offset = matchKeyModel.field().getBitOffset() +
                            matchKeyModel.field().getHeader().getBitOffset();
                    headerValue.mod(flowKey.getValue(), offset);
                    maskValue.mod(matchKeyModel.getMaskValue(), offset);
                } else {
                    throw new RuntimeException("Can't find the match key model.");
                }
            });

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(this.applicationId,
                    this.deviceId,
                    Vp4Interpreter.getTableId(Vp4Interpreter.HEADER_MATCH,
                            "stage" + stage.getId()));
            TrafficSelectorHelper trafficSelectorHelper =
                    new TrafficSelectorHelper(compiler.getBmv2Configuration());


            trafficSelectorHelper.withProgramId((byte) this.instanceId)
                    .withStageId(stage.getId())
                    .withHeaderMatch(headerValue, maskValue);

            flowRuleHelper.withSelector(trafficSelectorHelper);

            TrafficTreatmentHelper trafficTreatmentHelper =
                    new TrafficTreatmentHelper(compiler.getBmv2Configuration());

            CompoundAction action = new CompoundAction(ACTION_SET_MATCH_RESULT);
            action.addParameter(Data.createLongData("match_result", matchResult.getHeaderMatchResult()));

            trafficTreatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(trafficTreatmentHelper);

            compiler.installRule(flowRuleHelper.build());
        }

        /* Match metadata */
        if (stage.isMatchMetadata()) {
            Value headerValue = Value.createValue(stage.getMaxMetadataBitLength());
            Value maskValue = Value.createValue(stage.getMaxMetadataBitLength());

            rule.getMetadataKeys().forEach(flowKey -> {
                MatchKeyModel matchKeyModel = stage.getMatchKey(flowKey.getKeyName());
                if (matchKeyModel != null) {
                    int offset = matchKeyModel.field().getBitOffset() + matchKeyModel.field().getHeader().getBitOffset();
                    headerValue.mod(flowKey.getValue(), offset);
                    maskValue.mod(matchKeyModel.getMaskValue(), offset);
                } else {
                    throw new RuntimeException("Can't find the match key model.");
                }
            });

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(this.applicationId,
                    this.deviceId,
                    Vp4Interpreter.getTableId(Vp4Interpreter.META_MATCH,
                            "stage" + stage.getId()));
            TrafficSelectorHelper trafficSelectorHelper =
                    new TrafficSelectorHelper(compiler.getBmv2Configuration());

            trafficSelectorHelper.withProgramId((byte) this.instanceId)
                    .withStageId(stage.getId())
                    .withHeaderMatch(headerValue, maskValue);

            flowRuleHelper.withSelector(trafficSelectorHelper);

            TrafficTreatmentHelper treatmentHelper =
                    new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(ACTION_SET_MATCH_RESULT);
            action.addParameter(Data.createLongData("match_result", matchResult.getMetadataMatchResult()));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(treatmentHelper);

            compiler.installRule(flowRuleHelper.build());
        }

        if (stage.isMatchStdMetadata()) {
            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(this.applicationId,
                    this.deviceId,
                    Vp4Interpreter.getTableId(Vp4Interpreter.STD_META_MATCH,
                            "stage" + stage.getId()));
            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            int matchBitMap = 0;
            Short ingressPort = rule.getOrDefault(INGRESS_PORT_KEY, (short) -1);

            if (ingressPort >= 0) {
                matchBitMap |= 1;
            }

            Integer packetLength = rule.getOrDefault(PACKET_LENGTH_KEY, -1);

            matchBitMap <<= 1;
            if (packetLength >= 0) {
                matchBitMap |= 1;
            }

            Short egressSpec = rule.getOrDefault(EGRESS_SPEC_KEY, (short) -1);


            matchBitMap <<= 1;
            if (egressSpec >= 0) {
                matchBitMap |= 1;
            }

            Short egressPort = rule.getOrDefault(EGRESS_PORT_KEY, (short) -1);


            matchBitMap <<= 1;
            if (egressPort >= 0) {
                matchBitMap |= 1;
            }

            Integer egressInstance = rule.getOrDefault(EGRESS_INSTANCE_KEY, -1);

            matchBitMap <<= 1;
            if (egressInstance >= 0) {
                matchBitMap |= 1;
            }

            Integer instanceType = rule.getOrDefault(INSTANCE_TYPE_KEY, -1);

            matchBitMap <<= 1;
            if (instanceType >= 0) {
                matchBitMap |= 1;
            }

            Integer cloneSpec = rule.getOrDefault(CLONE_SPEC_KEY, -1);

            matchBitMap <<= 1;
            if (cloneSpec >= 0) {
                matchBitMap |= 1;
            }

            selectorHelper.withProgramId(getInstanceId())
                    .withStageId(stageId)
                    .withStandardMetadata(ingressPort,
                            packetLength,
                            egressSpec,
                            egressPort,
                            egressInstance,
                            instanceType,
                            cloneSpec,
                            matchBitMap);
            flowRuleHelper.withSelector(selectorHelper);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(ACTION_SET_MATCH_RESULT);
            action.addParameter(Data.createLongData("match_result", matchResult.getStdMetadataMatchResult()));
            treatmentHelper.withCoumpoundAction(action);

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(treatmentHelper);

            compiler.installRule(flowRuleHelper.build());
        }

        /* Match result table. */
        try {
            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);
            selectorHelper.withMatchResult(matchResult);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);
            CompoundAction action = new CompoundAction(ACTION_SET_STAGE_AND_BITMAP);
            action.addParameter(Data.createLongData("action_bitmap", actionModel.getActionBitMap()))
                    .addParameter(Data.createByteData("macth_bitmap", nextStage.getMatchBitmap()))
                    .addParameter(Data.createByteData("next_satge", nextStage.getId()))
                    .addParameter(Data.createByteData("next_prog", getInstanceId()));
            treatmentHelper.withCoumpoundAction(action);

            FlowRuleHelper ruleHelper = new FlowRuleHelper(this.applicationId, this.deviceId,
                    Vp4Interpreter.getTableId(TABLE_MATCH_RESULT, "stage" + stage.getId()));

            compiler.installRule(ruleHelper.build());
        } finally {
            // TODO
        }

        /* Actions. */
        actionModel.getPrimitiveModels().forEach(primitive -> {
            switch (primitive.getType()) {
                case MODIFY_FIELD:
                    installModifyFieldPrimitive(stage, primitive, matchResult, rule);
                    break;
                case DROP:
                case NO_OP:
                    break;

            }
        });

        return this;
    }

    public void delRule(InstanceRule rule) {
        // TODO
    }

    /**
     * Get name of the instance.
     *
     * @return instance name
     */
    public String getName() {
        return name;
    }

    /**
     * Get instance id.
     *
     * @return instance id
     */
    public byte getInstanceId() {
        return (byte) instanceId;
    }

    /**
     * Install modify_field primitive action.
     *
     * @param stage     current stage.
     * @param primitive the primitive
     * @param rule      runtime rule
     */
    private void installModifyFieldPrimitive(Stage stage, PrimitiveModel primitive, MatchResult matchResult, InstanceRule rule) {
        List<ParameterModel> parameters = primitive.getParameterModels();

        if (parameters.size() != 2) {
            throw new RuntimeException("Cannot install modify field primitive due to the wrong " +
                    "number of the parameters " + parameters.size());
        }

        ParameterModel firstParam = parameters.get(0);
        ParameterModel secondParam = parameters.get(1);

        String tableName = "table_mod_";
        String stageName = "stage" + stage.getId();

        if (firstParam.getType() == ParameterModel.ParameterModelType.METADATA_FIELD) {
            tableName += "meta_with_";
        } else if (firstParam.getType() == ParameterModel.ParameterModelType.PACKET_FIELD) {
            tableName += "header_with_";
        } else {
            throw new RuntimeException("Wrong modify_field fisrt parameter.");
        }

        switch (secondParam.getType()) {
            case CONST:
                tableName += "const";
                break;
            case METADATA_FIELD:
                tableName += "meta";
                break;
            case PACKET_FIELD:
                tableName += "header";
                break;
            default:
                throw new RuntimeException("Wrong modify_field second parameter.");
        }

        FlowRuleHelper flowRuleHelper = new FlowRuleHelper(this.applicationId, this.deviceId, Vp4Interpreter.getTableId(tableName, stageName));

        TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(this.configuration);
        selectorHelper.withMatchResult(matchResult);

        TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(this.configuration);


        switch (tableName) {
            case TABLE_MOD_HEADER_WITH_CONST: {
                CompoundAction action = new CompoundAction(ACTION_MOD_HEADER_WITH_CONST);
                FieldModel fieldModel = firstParam.getFieldModel();
                Value headerValue = Value.createValue(stage.getMaxHeaderBitLength());
                Value maskValue = Value.createValue(stage.getMaxHeaderBitLength());

                byte[] data = new byte[(fieldModel.getLength() + 7) / 8];

                Value value = Value.createArrayValue(data);
                // TODO

                break;
            }
            case TABLE_MOD_HEADER_WITH_HEADER: {
                CompoundAction action = new CompoundAction(ACTION_MOD_HEADER_WITH_HEADER);
                break;
            }
            case TABLE_MOD_HEADER_WITH_META: {
                CompoundAction action = new CompoundAction(ACTION_MOD_HEADER_WITH_META);
                break;
            }
            case TABLE_MOD_META_WITH_CONST: {
                CompoundAction action = new CompoundAction(ACTION_MOD_META_WITH_CONST);
                break;
            }
            case TABLE_MOD_META_WITH_HEADER: {
                CompoundAction action = new CompoundAction(ACTION_MOD_META_WITH_HEADER);
                break;
            }
            case TABLE_MOD_META_WITH_META: {
                CompoundAction action = new CompoundAction(ACTION_MOD_META_WITH_META);
                break;
            }
            default:
                throw new RuntimeException("can not find the table with the name : " + tableName);
        }
    }

}


