/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.netarch.odb;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.netarch.odb.runtime.CompoundAction;
import org.netarch.odb.runtime.Data;
import org.netarch.odb.runtime.MatchResult;
import org.netarch.odb.runtime.Value;
import org.netarch.odb.service.CompilerService;
import org.netarch.odb.utils.FlowRuleHelper;
import org.netarch.odb.utils.InitialTableRuleHelper;
import org.netarch.odb.utils.TrafficSelectorHelper;
import org.netarch.odb.utils.TrafficTreatmentHelper;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.io.FileWriter;
import static org.netarch.odb.compiler.Vp4Interpreter.*;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TestType TEST_TYPE = TestType.ROUTER_ACTION;
    private final short DAMPER_INDEX = 50;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CompilerService compilerService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;
    private ApplicationId applicationId;

    @Activate
    protected void activate() {
        log.info("ODB switch.");

        applicationId = coreService.registerApplication("odb-app-switch");

        List<DeviceId> deviceIdList = compilerService.getDevices();

        // long[] data = new long[1000];
        String name = "";
        // for (int i=0; i< 1000; i++) {
        // data[i] = System.nanoTime();
        for (DeviceId deviceId : deviceIdList) {
            switch (TEST_TYPE) {
                case ROUTER:
                    name = "router";
                    installRouter(deviceId);
                    break;
                case ROUTER_WATCH:
                    name = "watch";
                    installRouterWatch(deviceId);
                    break;
                case ROUTER_BREAK:
                    name = "break";
                    installRouterBreak(deviceId);
                    break;
                case ROUTER_ACTION:
                    name = "action";
                    installRouterAction(deviceId);
                    break;
                case ROUTER_MATCH:
                    name = "match";
                    installRouterMatch(deviceId);
                    break;
                case ROUTER_PREDICATION:
                    name = "predication";
                    installRouterPredication(deviceId);
                    break;
                case ROUTER_DAMPER:
                    name = "damper";
                    installRouterWithDamper(deviceId);
                    break;
                case ROUTER_WATCH1:
                    name = "watch1";
                    installRouterWatch1(deviceId);
                    break;
                case ROUTER_WATCH2:
                    name = "watch2";
                    installRouterWatch2(deviceId);
                    break;
                case ROUTER_WATCH3:
                    name = "watch3";
                    installRouterWatch3(deviceId);
                    break;
                case L2_SWITCH_PREDICATION:
                case L2_SWITCH_ACTION:
                case L2_SWITCH_BREAK:
                case L2_SWITCH_MATCH:
                case L2_SWITCH_WATCH:
                case L2_SWITTCH:
                    installSwitch(deviceId);
                    break;
                default:
                    break;
            }
        }
        /*data[i] = System.nanoTime() - data[i];
        }
        try {

            FileWriter file = new FileWriter("/home/netarchlab/odb/test_results/" + name);

            for (long datum : data) {
                file.write("" + datum);
                file.write("\n");
            }

            file.flush();
            file.close();
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        */
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    /**
     * Install a router into the data plane.
     *
     * @param deviceId device id
     */
    private void installRouter(DeviceId deviceId) {
        Bmv2Configuration configuration = compilerService.getConfiguration();
        int currentStage = 1;
        /* Table config at start. */
        try {
            InitialTableRuleHelper ruleHelper = new InitialTableRuleHelper(applicationId, deviceId, configuration);

            ruleHelper.setPolicyId((short) 1)
                    .setProgId((byte) 1)
                    .setMatchBitmap(HEADER_MATCH_BIT)
                    .setStageId((byte) 0);

            compilerService.installRule(ruleHelper.build());

        } catch (Exception e) {
            String result = "table_config_at_initial : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* STAGE 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x08000000000000000000000000000000000000000000",
                    100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000000000000000000000000000",
                    100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) (currentStage + 1)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------
        currentStage++;

        /* CONDITIONAL STAGE 2 */
        /* table_get_expression_stage2 */
        try {
            String tableName = "table_get_expression_stage" + currentStage;
            String actionName = "action_set_expr_header_op_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("l_expr_offset", (short) 104))
                    .addParameter(Data.createIntData("l_expr_mask", 0xFFFF))
                    .addParameter(Data.createByteData("op", (byte) 0))
                    .addParameter(Data.createByteData("r_expr", (byte) 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_get_expression_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch1_stage2 */
        try {
            String tableName = "table_branch_1_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_1_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch2_stage2 */
        try {
            String tableName = "table_branch_2_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage + 8))
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createByteData("action_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 1))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_2_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch3_stage2 */
        try {
            String tableName = "table_branch_3_stage2";
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_3_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage2";
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 1)
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x100000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 2))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage2";
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000002L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage2";
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage2 */
        try {
            String tableName = "table_header_match_stage2";
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 1)
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x200000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 2))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage2";
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000102L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage2";
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* STAGE 3 */
        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage3";
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 2)
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x10000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage3";
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x10000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 1))
                    .addParameter(Data.createByteData("next_stage", (byte) 3))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage3";
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 2));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage3";
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000000000000000",
                                    100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage3";
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 2)
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x20000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

                /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage3";
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x20000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 1))
                    .addParameter(Data.createByteData("next_stage", (byte) 3))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage3";
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage3";
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* STAGE 4 */
        /* table_std_meta_match_stage4 */
        try {
            String tableName = "table_std_meta_match_stage4";
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 3)
                    .withStandardMetadata((short) 1, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_std_meta_match_stage4";
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 3)
                    .withStandardMetadata((short) 2, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 2))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage4";
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage4";
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x2L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000", 100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 " + e.toString() + " : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
    }

    /**
     * Install a router into the data plane.
     *
     * @param deviceId device id
     */
    private void installRouterWatch(DeviceId deviceId) {
        Bmv2Configuration configuration = compilerService.getConfiguration();
        byte currentStage = 1;

        /* Table config at start. */
        try {
            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, CONFIG_AT_INITIAL);

            TrafficSelectorHelper trafficSelectorHelper = new TrafficSelectorHelper(configuration);
            trafficSelectorHelper.withPolicyId((short) 1)
                    .withProgramId((byte) 0)
                    .withStageId((byte) 0);

            flowRuleHelper.withSelector(trafficSelectorHelper);

            TrafficTreatmentHelper trafficTreatmentHelper = new TrafficTreatmentHelper(
                    compilerService.getConfiguration());

            CompoundAction action = new CompoundAction(ACTION_SET_INITIAL_CONFIG);
            action.addParameter(Data.createByteData("progid", (byte) 1))
                    .addParameter(Data.createByteData("initstage", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4));

            trafficTreatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(trafficTreatmentHelper);
            compilerService.installRule(flowRuleHelper.build());

        } catch (Exception e) {
            String result = "table_config_at_initial : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------------
        /* WATCH 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createIntData("receiver", 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //-------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x08000000000000000000000000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000000000000000000000000000", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 10))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //----------------------------------------------------------------------------------
        /* CONDITIONAL STAGE 2 */
        /* table_get_expression_stage2 */
        try {
            String tableName = "table_get_expression_stage" + currentStage;
            String actionName = "action_set_expr_header_op_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("l_expr_offset", (short) 104))
                    .addParameter(Data.createIntData("l_expr_mask", 0xFFFF))
                    .addParameter(Data.createByteData("op", (byte) 0))
                    .addParameter(Data.createByteData("r_expr", (byte) 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_get_expression_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch1_stage2 */
        try {
            String tableName = "table_branch_1_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10)
                    .withOperation((byte) 0);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_1_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch2_stage2 */
        try {
            String tableName = "table_branch_2_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) (currentStage - 1)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_2_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch3_stage2 */
        try {
            String tableName = "table_branch_3_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_3_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------------
        /* WATCH 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------
        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("receiver", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_generate_digest_stage : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //-----------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x100000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000002L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x200000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000102L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------------
        currentStage++;

        //---------------------------------------------------------------------------------------
        /* WATCH 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------
        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("receiver", (byte) 2));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_generate_digest_stage : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;
        /* STAGE 3 */
        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x10000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x10000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", HEADER_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 2));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000000000000000",
                                    100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x20000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x20000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", HEADER_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------


        currentStage++;

        //---------------------------------------------------------------------------------------
        /* WATCH 3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------
        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("receiver", (byte) 3));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_generate_digest_stage : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //------------------------------------------------------------------------------------
        /* STAGE 4 */
        /* table_std_meta_match_stage4 */
        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 1, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 2, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 2))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x2L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000", 100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 " + e.toString() + " : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
    }

    /**
     * Install router with break point snippets.
     *
     * @param deviceId device id
     */
    private void installRouterBreak(DeviceId deviceId) {
        Bmv2Configuration configuration = compilerService.getConfiguration();
        byte currentStage = 1;
        /* Table config at start. */
        try {
            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, CONFIG_AT_INITIAL);

            TrafficSelectorHelper trafficSelectorHelper = new TrafficSelectorHelper(configuration);
            trafficSelectorHelper.withPolicyId((short) 1)
                    .withProgramId((byte) 0)
                    .withStageId((byte) 0);

            flowRuleHelper.withSelector(trafficSelectorHelper);

            TrafficTreatmentHelper trafficTreatmentHelper = new TrafficTreatmentHelper(
                    configuration);

            CompoundAction action = new CompoundAction(ACTION_SET_INITIAL_CONFIG);
            action.addParameter(Data.createByteData("progid", (byte) 1))
                    .addParameter(Data.createByteData("initstage", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4));

            trafficTreatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(trafficTreatmentHelper);
            compilerService.installRule(flowRuleHelper.build());

        } catch (Exception e) {
            String result = "table_config_at_initial : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------------
        /* WATCH 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createIntData("receiver", 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //-------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x08000000000000000000000000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000000000000000000000000000", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 10))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //----------------------------------------------------------------------------------
        /* CONDITIONAL STAGE 2 */
        /* table_get_expression_stage2 */
        try {
            String tableName = "table_get_expression_stage" + currentStage;
            String actionName = "action_set_expr_header_op_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("l_expr_offset", (short) 104))
                    .addParameter(Data.createIntData("l_expr_mask", 0xFFFF))
                    .addParameter(Data.createByteData("op", (byte) 0))
                    .addParameter(Data.createByteData("r_expr", (byte) 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_get_expression_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch1_stage2 */
        try {
            String tableName = "table_branch_1_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10)
                    .withOperation((byte) 0);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_1_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch2_stage2 */
        try {
            String tableName = "table_branch_2_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) (currentStage - 1)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_2_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch3_stage2 */
        try {
            String tableName = "table_branch_3_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_3_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //-----------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x100000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000002L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x200000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000102L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------------
        currentStage++;

        /* STAGE 3 */
        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x10000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x10000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 1))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 2));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000000000000000",
                                    100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x20000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x20000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 1))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------
        currentStage++;

        //------------------------------------------------------------------------------------
        /* STAGE 4 */
        /* table_std_meta_match_stage4 */
        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 1, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 2, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 2))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x2L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000", 100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 " + e.toString() + " : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
    }

    /**
     * Install router with prediction snippets.
     *
     * @param deviceId device id
     */
    private void installRouterPredication(DeviceId deviceId) {
        Bmv2Configuration configuration = compilerService.getConfiguration();
        byte currentStage = 1;
        /* Table config at start. */
        try {
            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, CONFIG_AT_INITIAL);

            TrafficSelectorHelper trafficSelectorHelper = new TrafficSelectorHelper(configuration);
            trafficSelectorHelper.withPolicyId((short) 1)
                    .withProgramId((byte) 0)
                    .withStageId((byte) 0);

            flowRuleHelper.withSelector(trafficSelectorHelper);

            TrafficTreatmentHelper trafficTreatmentHelper = new TrafficTreatmentHelper(
                    configuration);

            CompoundAction action = new CompoundAction(ACTION_SET_INITIAL_CONFIG);
            action.addParameter(Data.createByteData("progid", (byte) 1))
                    .addParameter(Data.createByteData("initstage", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4));

            trafficTreatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(trafficTreatmentHelper);
            compilerService.installRule(flowRuleHelper.build());

        } catch (Exception e) {
            String result = "table_config_at_initial : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //-------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x08000000000000000000000000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000000000000000000000000000", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 9))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //----------------------------------------------------------------------------------
        /* CONDITIONAL STAGE 2 */
        /* table_get_expression_stage2 */
        try {
            String tableName = "table_get_expression_stage" + currentStage;
            String actionName = "action_set_expr_header_op_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("l_expr_offset", (short) 104))
                    .addParameter(Data.createIntData("l_expr_mask", 0xFFFF))
                    .addParameter(Data.createByteData("op", (byte) 0))
                    .addParameter(Data.createByteData("r_expr", (byte) 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_get_expression_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch1_stage2 */
        try {
            String tableName = "table_branch_1_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_1_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch2_stage2 */
        try {
            String tableName = "table_branch_2_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) (currentStage - 1)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_2_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch3_stage2 */
        try {
            String tableName = "table_branch_3_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_3_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------------
        /* WATCH 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createIntData("receiver", 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //-----------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x100000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000002L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x200000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000102L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------------
        currentStage++;

        /* STAGE 3 */
        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x10000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x10000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 1))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 2));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000000000000000",
                                    100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x20000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x20000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 1))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------
        currentStage++;

        //------------------------------------------------------------------------------------
        /* STAGE 4 */
        /* table_std_meta_match_stage4 */
        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 1, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 2, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 2))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x2L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000", 100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 " + e.toString() + " : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
    }

    /**
     * Install router with match snippets.
     *
     * @param deviceId device id
     */
    private void installRouterMatch(DeviceId deviceId) {
        Bmv2Configuration configuration = compilerService.getConfiguration();
        byte currentStage = 1;
        /* Table config at start. */
        try {
            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, CONFIG_AT_INITIAL);

            TrafficSelectorHelper trafficSelectorHelper = new TrafficSelectorHelper(configuration);
            trafficSelectorHelper.withPolicyId((short) 1)
                    .withProgramId((byte) 0)
                    .withStageId((byte) 0);

            flowRuleHelper.withSelector(trafficSelectorHelper);

            TrafficTreatmentHelper trafficTreatmentHelper = new TrafficTreatmentHelper(
                    configuration);

            CompoundAction action = new CompoundAction(ACTION_SET_INITIAL_CONFIG);
            action.addParameter(Data.createByteData("progid", (byte) 1))
                    .addParameter(Data.createByteData("initstage", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4));

            trafficTreatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(trafficTreatmentHelper);
            compilerService.installRule(flowRuleHelper.build());

        } catch (Exception e) {
            String result = "table_config_at_initial : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //-------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x08000000000000000000000000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000000000000000000000000000", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 9))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //----------------------------------------------------------------------------------
        /* CONDITIONAL STAGE 2 */
        /* table_get_expression_stage2 */
        try {
            String tableName = "table_get_expression_stage" + currentStage;
            String actionName = "action_set_expr_header_op_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("l_expr_offset", (short) 104))
                    .addParameter(Data.createIntData("l_expr_mask", 0xFFFF))
                    .addParameter(Data.createByteData("op", (byte) 0))
                    .addParameter(Data.createByteData("r_expr", (byte) 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_get_expression_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch1_stage2 */
        try {
            String tableName = "table_branch_1_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_1_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch2_stage2 */
        try {
            String tableName = "table_branch_2_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) (currentStage - 1)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_2_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch3_stage2 */
        try {
            String tableName = "table_branch_3_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_3_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //-----------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x100000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000002L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x200000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000102L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------------
        currentStage++;

        /* STAGE 3 */
        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result_with_next_stage";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x10000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result_with_next_stage";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);

            action.addParameter(Data.createLongData("match_result", 0x20000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        //---------------------------------------------------------------------------------------
        currentStage++;

        //---------------------------------------------------------------------------------------
        /* Match 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id_direct";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_id", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createIntData("receiver", 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;



        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x10000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 1))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x20000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 1))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 2));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000000000000000",
                                    100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------
        currentStage++;

        //------------------------------------------------------------------------------------
        /* STAGE 4 */
        /* table_std_meta_match_stage4 */
        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 1, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 2, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 2))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x2L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000", 100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 " + e.toString() + " : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
    }

    /**
     * Install router with predication snippets.
     *
     * @param deviceId device id
     */
    private void installRouterAction(DeviceId deviceId) {
        Bmv2Configuration configuration = compilerService.getConfiguration();
        byte currentStage = 1;
        /* Table config at start. */
        try {
            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, CONFIG_AT_INITIAL);

            TrafficSelectorHelper trafficSelectorHelper = new TrafficSelectorHelper(configuration);
            trafficSelectorHelper.withPolicyId((short) 1)
                    .withProgramId((byte) 0)
                    .withStageId((byte) 0);

            flowRuleHelper.withSelector(trafficSelectorHelper);

            TrafficTreatmentHelper trafficTreatmentHelper = new TrafficTreatmentHelper(
                    configuration);

            CompoundAction action = new CompoundAction(ACTION_SET_INITIAL_CONFIG);
            action.addParameter(Data.createByteData("progid", (byte) 1))
                    .addParameter(Data.createByteData("initstage", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4));

            trafficTreatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(trafficTreatmentHelper);
            compilerService.installRule(flowRuleHelper.build());

        } catch (Exception e) {
            String result = "table_config_at_initial : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //-------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x08000000000000000000000000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000000000000000000000000000", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 9))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //----------------------------------------------------------------------------------
        /* CONDITIONAL STAGE 2 */
        /* table_get_expression_stage2 */
        try {
            String tableName = "table_get_expression_stage" + currentStage;
            String actionName = "action_set_expr_header_op_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("l_expr_offset", (short) 104))
                    .addParameter(Data.createIntData("l_expr_mask", 0xFFFF))
                    .addParameter(Data.createByteData("op", (byte) 0))
                    .addParameter(Data.createByteData("r_expr", (byte) 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_get_expression_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch1_stage2 */
        try {
            String tableName = "table_branch_1_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_1_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch2_stage2 */
        try {
            String tableName = "table_branch_2_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) (currentStage - 1)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_2_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch3_stage2 */
        try {
            String tableName = "table_branch_3_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_3_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //-----------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x100000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000002L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x200000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000102L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------------
        currentStage++;

        /* STAGE 3 */
        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x10000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x10000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", HEADER_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 2));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000000000000000",
                                    100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x20000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x20000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", HEADER_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------
        currentStage++;

        //---------------------------------------------------------------------------------------
        /* WATCH 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createIntData("receiver", 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //------------------------------------------------------------------------------------
        /* STAGE 4 */
        /* table_std_meta_match_stage4 */
        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 1, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 2, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 2))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x2L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000", 100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 " + e.toString() + " : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
    }


    /**
     * Install router with predication snippets.
     *
     * @param deviceId device id
     */
    private void installRouterWithDamper(DeviceId deviceId) {
        Bmv2Configuration configuration = compilerService.getConfiguration();
        byte currentStage = 1;
        /* Table config at start. */
        try {
            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, CONFIG_AT_INITIAL);

            TrafficSelectorHelper trafficSelectorHelper = new TrafficSelectorHelper(configuration);
            trafficSelectorHelper.withPolicyId((short) 1)
                    .withProgramId((byte) 0)
                    .withStageId((byte) 0);

            flowRuleHelper.withSelector(trafficSelectorHelper);

            TrafficTreatmentHelper trafficTreatmentHelper = new TrafficTreatmentHelper(
                    configuration);

            CompoundAction action = new CompoundAction(ACTION_SET_INITIAL_CONFIG);
            action.addParameter(Data.createByteData("progid", (byte) 1))
                    .addParameter(Data.createByteData("initstage", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4));

            trafficTreatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(trafficTreatmentHelper);
            compilerService.installRule(flowRuleHelper.build());

        } catch (Exception e) {
            String result = "table_config_at_initial : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //-------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x08000000000000000000000000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000000000000000000000000000", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 9))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //----------------------------------------------------------------------------------
        /* CONDITIONAL STAGE 2 */
        /* table_get_expression_stage2 */
        try {
            String tableName = "table_get_expression_stage" + currentStage;
            String actionName = "action_set_expr_header_op_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("l_expr_offset", (short) 104))
                    .addParameter(Data.createIntData("l_expr_mask", 0xFFFF))
                    .addParameter(Data.createByteData("op", (byte) 0))
                    .addParameter(Data.createByteData("r_expr", (byte) 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_get_expression_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch1_stage2 */
        try {
            String tableName = "table_branch_1_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_1_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch2_stage2 */
        try {
            String tableName = "table_branch_2_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) (currentStage - 1)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_2_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch3_stage2 */
        try {
            String tableName = "table_branch_3_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_3_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //-----------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x100000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", getNextStage(currentStage)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000002L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x200000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000102L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------------
        currentStage++;

        /* STAGE 3 */
        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x10000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x10000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", HEADER_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", getNextStage(currentStage)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 2));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000000000000000",
                                    100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x20000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x20000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", HEADER_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------
        currentStage++;

        //---------------------------------------------------------------------------------------
        /* Break With Damper 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId(getCurrentStage(currentStage))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", ACTION_COUNTER_BIT))
                    .addParameter(Data.createByteData("match_bitmap", HEADER_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", getNextConditionalStage(currentStage)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_counter_stage" + currentStage;
            String actionName = "packet_count";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);

            action.addParameter(Data.createIntData("index", 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_counter_stage : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        currentStage++;

       /* Packet counter branch */
        try {
            String tableName = "table_get_expression_stage" + currentStage;
            String actionName = "action_set_expr_counter_op_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId(getCurrentConditionalStage(currentStage));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("op", OP_EQUAL))
                    .addParameter(Data.createShortData("r_expr", DAMPER_INDEX));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_get_expression_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch1_stage2 */
        try {
            String tableName = "table_branch_1_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId(getCurrentConditionalStage(currentStage))
                    .withOperation(OP_EQUAL);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", STD_META_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", getNextStage(currentStage)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_1_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch2_stage2 */
        try {
            String tableName = "table_branch_2_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId(getCurrentConditionalStage(currentStage))
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", HEADER_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", getCurrentStage(currentStage)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_2_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch3_stage2 */
        try {
            String tableName = "table_branch_3_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId(getCurrentConditionalStage(currentStage))
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", HEADER_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", getCurrentStage(currentStage)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_3_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", ACTION_COUNTER_BIT | ACTION_GENERATE_DEGIST))
                    .addParameter(Data.createByteData("match_bitmap", HEADER_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createIntData("receiver", 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        try {
            String tableName = "table_counter_stage" + currentStage;
            String actionName = "packet_count_clear";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);

            action.addParameter(Data.createIntData("index", 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_counter_stage : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //------------------------------------------------------------------------------------
        /* STAGE 4 */
        /* table_std_meta_match_stage4 */
        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 1, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 2, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 2))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x2L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000", 100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 " + e.toString() + " : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
    }


    /**
     * Install switch.
     *
     * @param deviceId device id
     */
    private void installSwitch(DeviceId deviceId) {
        Bmv2Configuration configuration = compilerService.getConfiguration();
        byte currentStage = 1;
        try {
            InitialTableRuleHelper ruleHelper = new InitialTableRuleHelper(applicationId, deviceId, configuration);

            ruleHelper.setPolicyId((short) 1)
                    .setProgId((byte) 1)
                    .setMatchBitmap(HEADER_MATCH_BIT)
                    .setStageId((byte) 0);

            compilerService.installRule(ruleHelper.build());

        } catch (Exception e) {
            String result = "table_config_at_initial : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

      /* STAGE 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x00020000000000000000",
                    100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000",
                    100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x200000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------
        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 2));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

              /* STAGE 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x00010000000000000000",
                    100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000",
                    100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x100000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------
        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
    }

        /**
     * Install a router into the data plane.
     *
     * @param deviceId device id
     */
    private void installRouterWatch1(DeviceId deviceId) {
        Bmv2Configuration configuration = compilerService.getConfiguration();
        byte currentStage = 1;

        /* Table config at start. */
        try {
            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, CONFIG_AT_INITIAL);

            TrafficSelectorHelper trafficSelectorHelper = new TrafficSelectorHelper(configuration);
            trafficSelectorHelper.withPolicyId((short) 1)
                    .withProgramId((byte) 0)
                    .withStageId((byte) 0);

            flowRuleHelper.withSelector(trafficSelectorHelper);

            TrafficTreatmentHelper trafficTreatmentHelper = new TrafficTreatmentHelper(
                    compilerService.getConfiguration());

            CompoundAction action = new CompoundAction(ACTION_SET_INITIAL_CONFIG);
            action.addParameter(Data.createByteData("progid", (byte) 1))
                    .addParameter(Data.createByteData("initstage", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4));

            trafficTreatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(trafficTreatmentHelper);
            compilerService.installRule(flowRuleHelper.build());

        } catch (Exception e) {
            String result = "table_config_at_initial : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------------
        /* WATCH 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createIntData("receiver", 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //-------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x08000000000000000000000000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000000000000000000000000000", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 10))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //----------------------------------------------------------------------------------
        /* CONDITIONAL STAGE 2 */
        /* table_get_expression_stage2 */
        try {
            String tableName = "table_get_expression_stage" + currentStage;
            String actionName = "action_set_expr_header_op_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("l_expr_offset", (short) 104))
                    .addParameter(Data.createIntData("l_expr_mask", 0xFFFF))
                    .addParameter(Data.createByteData("op", (byte) 0))
                    .addParameter(Data.createByteData("r_expr", (byte) 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_get_expression_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch1_stage2 */
        try {
            String tableName = "table_branch_1_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10)
                    .withOperation((byte) 0);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_1_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch2_stage2 */
        try {
            String tableName = "table_branch_2_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", HEADER_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", (byte) (currentStage - 1)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_2_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch3_stage2 */
        try {
            String tableName = "table_branch_3_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_3_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //-----------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x100000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000002L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x200000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000102L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------------
        currentStage++;

        /* STAGE 3 */
        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x10000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x10000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", STD_META_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 2));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000000000000000",
                                    100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x20000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x20000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", STD_META_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------
        currentStage++;

        //------------------------------------------------------------------------------------
        /* STAGE 4 */
        /* table_std_meta_match_stage4 */
        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 1, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 2, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 2))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x2L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000", 100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 " + e.toString() + " : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
    }


    /**
     * Install a router into the data plane.
     *
     * @param deviceId device id
     */
    private void installRouterWatch2(DeviceId deviceId) {
        Bmv2Configuration configuration = compilerService.getConfiguration();
        byte currentStage = 1;

        /* Table config at start. */
        try {
            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, CONFIG_AT_INITIAL);

            TrafficSelectorHelper trafficSelectorHelper = new TrafficSelectorHelper(configuration);
            trafficSelectorHelper.withPolicyId((short) 1)
                    .withProgramId((byte) 0)
                    .withStageId((byte) 0);

            flowRuleHelper.withSelector(trafficSelectorHelper);

            TrafficTreatmentHelper trafficTreatmentHelper = new TrafficTreatmentHelper(
                    compilerService.getConfiguration());

            CompoundAction action = new CompoundAction(ACTION_SET_INITIAL_CONFIG);
            action.addParameter(Data.createByteData("progid", (byte) 1))
                    .addParameter(Data.createByteData("initstage", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4));

            trafficTreatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(trafficTreatmentHelper);
            compilerService.installRule(flowRuleHelper.build());

        } catch (Exception e) {
            String result = "table_config_at_initial : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------------
        /* WATCH 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createIntData("receiver", 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //-------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x08000000000000000000000000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000000000000000000000000000", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 10))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //----------------------------------------------------------------------------------
        /* CONDITIONAL STAGE 2 */
        /* table_get_expression_stage2 */
        try {
            String tableName = "table_get_expression_stage" + currentStage;
            String actionName = "action_set_expr_header_op_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("l_expr_offset", (short) 104))
                    .addParameter(Data.createIntData("l_expr_mask", 0xFFFF))
                    .addParameter(Data.createByteData("op", (byte) 0))
                    .addParameter(Data.createByteData("r_expr", (byte) 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_get_expression_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch1_stage2 */
        try {
            String tableName = "table_branch_1_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10)
                    .withOperation((byte) 0);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_1_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch2_stage2 */
        try {
            String tableName = "table_branch_2_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) (currentStage - 1)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_2_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch3_stage2 */
        try {
            String tableName = "table_branch_3_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_3_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------------
        /* WATCH 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------
        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("receiver", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_generate_digest_stage : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //-----------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x100000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000002L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x200000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000102L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------------
        currentStage++;


        /* STAGE 3 */
        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x10000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x10000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", STD_META_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 2));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000000000000000",
                                    100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x20000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x20000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", STD_META_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------


        currentStage++;


        //------------------------------------------------------------------------------------
        /* STAGE 4 */
        /* table_std_meta_match_stage4 */
        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 1, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 2, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 2))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x2L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000", 100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 " + e.toString() + " : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
    }

    /**
     * Install a router into the data plane.
     *
     * @param deviceId device id
     */
    private void installRouterWatch3(DeviceId deviceId) {
        Bmv2Configuration configuration = compilerService.getConfiguration();
        byte currentStage = 1;

        /* Table config at start. */
        try {
            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, CONFIG_AT_INITIAL);

            TrafficSelectorHelper trafficSelectorHelper = new TrafficSelectorHelper(configuration);
            trafficSelectorHelper.withPolicyId((short) 1)
                    .withProgramId((byte) 0)
                    .withStageId((byte) 0);

            flowRuleHelper.withSelector(trafficSelectorHelper);

            TrafficTreatmentHelper trafficTreatmentHelper = new TrafficTreatmentHelper(
                    compilerService.getConfiguration());

            CompoundAction action = new CompoundAction(ACTION_SET_INITIAL_CONFIG);
            action.addParameter(Data.createByteData("progid", (byte) 1))
                    .addParameter(Data.createByteData("initstage", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4));

            trafficTreatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withTreatment(trafficTreatmentHelper);
            compilerService.installRule(flowRuleHelper.build());

        } catch (Exception e) {
            String result = "table_config_at_initial : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------------
        /* WATCH 1 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createIntData("receiver", 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //-------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix(
                    "0x08000000000000000000000000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix(
                    "0xFFFF0000000000000000000000000000000000000000", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) 10))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //----------------------------------------------------------------------------------
        /* CONDITIONAL STAGE 2 */
        /* table_get_expression_stage2 */
        try {
            String tableName = "table_get_expression_stage" + currentStage;
            String actionName = "action_set_expr_header_op_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("l_expr_offset", (short) 104))
                    .addParameter(Data.createIntData("l_expr_mask", 0xFFFF))
                    .addParameter(Data.createByteData("op", (byte) 0))
                    .addParameter(Data.createByteData("r_expr", (byte) 0));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_get_expression_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch1_stage2 */
        try {
            String tableName = "table_branch_1_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10)
                    .withOperation((byte) 0);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_1_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch2_stage2 */
        try {
            String tableName = "table_branch_2_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 10)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0))
                    .addParameter(Data.createLongData("action_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", (byte) (currentStage - 1)))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_2_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_branch3_stage2 */
        try {
            String tableName = "table_branch_3_stage" + currentStage;
            String actionName = "action_end";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) 9)
                    .withOperation((byte) 0);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_branch_3_stage2 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------------
        /* WATCH 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------
        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("receiver", (byte) 1));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_generate_digest_stage : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;

        //-----------------------------------------------------------------------------------
        /* STAGE 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x100000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000002L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x100000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);


            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x200000000L))
                    .addParameter(Data.createLongData("action_bitmap", 0x100000080L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_meta_with_const_stage2 */
        try {
            String tableName = "table_mod_meta_with_const_stage" + currentStage;
            String actionName = "action_mod_meta_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("value", 0x0a000102L))
                    .addParameter(Data.createLongData("mask1", 0xFFFFFFFFL));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_meta_with_const_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_substract_stage2 */
        try {
            String tableName = "table_subtract_stage" + currentStage;
            String actionName = "action_subtract_const_from_header";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x200000000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);


            Value value = Value.createValueFromHexWithPrefix("0x010000000000000000000000", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFF0000000000000000000000", 100);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createData("value1", value))
                    .addParameter(Data.createData("mask1", mask));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);
            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_subtract_stage2 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------------
        currentStage++;

        //---------------------------------------------------------------------------------------
        /* WATCH 2 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x00", 100);
            Value mask = Value.createValueFromHexWithPrefix("0x00", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1L))
                    .addParameter(Data.createLongData("action_bitmap", 0x2000000))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 4))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage1 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        //---------------------------------------------------------------------------------
        try {
            String tableName = "table_generate_digest_stage" + currentStage;
            String actionName = "action_gen_watch_digest";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createByteData("receiver", (byte) 2));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_generate_digest_stage : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //------------------------------------------------------------------------------
        currentStage++;
        /* STAGE 3 */
        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000002", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x10000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x10000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", STD_META_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 2));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x10000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000000000000000",
                                    100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_header_match_stage3 */
        try {
            String tableName = "table_header_match_stage" + currentStage;
            String actionName = "action_set_match_result";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            Value header = Value.createValueFromHexWithPrefix("0x0a000001", 100);
            Value mask = Value.createValueFromHexWithPrefix("0xFFFFFFFF", 100);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withHeaderMatch(header, mask);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 0x20000L));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_match_result_stage3 */
        try {
            String tableName = "table_match_result_stage" + currentStage;
            String actionName = "action_set_stage_and_bitmap";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withMatchResult(new MatchResult(0x20000L));


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("action_bitmap", 0x2080000000L))
                    .addParameter(Data.createByteData("match_bitmap", STD_META_MATCH_BIT))
                    .addParameter(Data.createByteData("next_stage", currentStage))
                    .addParameter(Data.createByteData("next_prog", (byte) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_header_match_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }


        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_std_meta_stage" + currentStage;
            String actionName = "action_forward";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createShortData("port", (short) 1));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_std_meta_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        /* table_mod_std_meta_stage3. */
        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x20000L);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage3 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
        //---------------------------------------------------------------------------------


        currentStage++;


        //------------------------------------------------------------------------------------
        /* STAGE 4 */
        /* table_std_meta_match_stage4 */
        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 1, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);


            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 1))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_std_meta_match_stage" + currentStage;
            String actionName = "action_set_action_id";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withProgramId((byte) 1)
                    .withStageId((byte) (currentStage - 1))
                    .withStandardMetadata((short) 2, 0, (short) 0, (short) 0, 0, 0, 0, 0x01);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(Data.createLongData("match_result", 2))
                    .addParameter(Data.createLongData("action_bitmap", 0x0000002000000000L))
                    .addParameter(Data.createByteData("match_bitmap", (byte) 0))
                    .addParameter(Data.createByteData("next_stage", (byte) 0))
                    .addParameter(Data.createByteData("next_prog", (byte) 0xFF));

            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_std_meta_match_stage4 : \n ";

            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x1L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010001000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000",
                                            100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }

        try {
            String tableName = "table_mod_header_with_const_stage" + currentStage;
            String actionName = "action_mod_header_with_const";

            FlowRuleHelper flowRuleHelper = new FlowRuleHelper(applicationId, deviceId, tableName);

            TrafficSelectorHelper selectorHelper = new TrafficSelectorHelper(configuration);

            selectorHelper.withActionId(0x2L);

            TrafficTreatmentHelper treatmentHelper = new TrafficTreatmentHelper(configuration);

            CompoundAction action = new CompoundAction(actionName);
            action.addParameter(
                    Data.createData("value",
                            Value.createValueFromHexWithPrefix(
                                    "0x082200010002000000000000000000000000000000000000000000", 100)))
                    .addParameter(
                            Data.createData("mask1",
                                    Value.createValueFromHexWithPrefix(
                                            "0xFFFFFFFFFFFF000000000000000000000000000000000000000000", 100)));
            treatmentHelper.withCoumpoundAction(action);

            flowRuleHelper.withSelector(selectorHelper)
                    .withTreatment(treatmentHelper)
                    .withPriority(100);

            compilerService.installRule(flowRuleHelper.build());
        } catch (Exception e) {
            String result = "table_mod_header_with_const_stage4 " + e.toString() + " : \n ";
            for (StackTraceElement element : e.getStackTrace()) {
                result += element.toString() + "\n";
            }
            log.error(result);
        }
    }
    

    enum TestType {
        ROUTER,
        ROUTER_WATCH,
        ROUTER_BREAK,
        ROUTER_PREDICATION,
        ROUTER_ACTION,
        ROUTER_MATCH,
        ROUTER_WATCH1,
        ROUTER_WATCH2,
        ROUTER_WATCH3,
        ROUTER_DAMPER,
        L2_SWITTCH,
        L2_SWITCH_WATCH,
        L2_SWITCH_BREAK,
        L2_SWITCH_PREDICATION,
        L2_SWITCH_ACTION,
        L2_SWITCH_MATCH

    }

}
