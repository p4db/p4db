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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.runtime.Bmv2Device;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.api.service.Bmv2DeviceContextService;
import org.onosproject.bmv2.api.service.Bmv2PacketListener;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private static final String JSON_PATH = "/router.json";
    private static final String CONFIGURATION_NAME = "ROUTER";
    private static final Bmv2Configuration CONFIGURATION = loadConfiguration();
    private static final RouterInterpreter INTERPRETER = new RouterInterpreter();
    private static final Bmv2DeviceContext DEVICE_CONTEXT = new Bmv2DeviceContext(CONFIGURATION, INTERPRETER);
    private static final String APP_NAME = "ODB Router";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ExecutorService executorService = Executors
            .newFixedThreadPool(8, groupedThreads("onos/odb", "bmv2-app-task", log));


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Bmv2Controller bmv2Controller;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ApplicationAdminService appService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Bmv2DeviceContextService bmv2ContextService;

    private ConcurrentMap<DeviceId, Lock> deviceLocks = Maps.newConcurrentMap();

    private List<DeviceId> deviceIdList;
    private ApplicationId applicationId;
    private Compiler compiler;


    public AppComponent() {
        deviceIdList = Lists.newArrayList();
    }

    /**
     * Load configuration from the json file.
     *
     * @return BMv2 configuration
     */
    private static Bmv2Configuration loadConfiguration() {
        try {
            JsonObject json = Json.parse(new BufferedReader(new InputStreamReader(AppComponent.class.getResourceAsStream(JSON_PATH)))).asObject();
            return Bmv2DefaultConfiguration.parse(json);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration");
        }
    }

    /**
     * Activate the ODB compiler.
     */
    @Activate
    protected void activate() {
        topologyService.addListener(new InternalTopologyListener());
        deviceService.addListener(new InternalDeviceListener());
        bmv2Controller.addPacketListener(new InternalPakcetListener());

        applicationId = coreService.registerApplication(APP_NAME);

        bmv2ContextService.registerInterpreterClassLoader(INTERPRETER.getClass(), this.getClass().getClassLoader());

        //spawnTask(() -> deviceService.getAvailableDevices().forEach(device -> {
        //    deviceIdList.add(device.id());
        //    deployDevice(device);
        //}));

        bmv2Controller.addPacketListener(new InternalPakcetListener());
    }

    public void installRule(FlowRule rule) {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        ops.add(rule);
        flowRuleService.apply(ops.build());
    }

    /**
     * Deploy device, and called when finding new device.
     *
     * @param device device object
     */
    private void deployDevice(Device device) {
        DeviceId deviceId = device.id();
        deviceIdList.add(deviceId);

        bmv2ContextService.getContext(deviceId);



        try {
            log.info("Setting device context to {} for {} ...", CONFIGURATION_NAME, deviceId);
            bmv2ContextService.setContext(deviceId, DEVICE_CONTEXT);
        } finally {
            log.info("Device {} deployment done.", deviceId);
        }

        /*Break_1*/
        try {
            FlowRule.Builder builder =
                    DefaultFlowRule.builder()
                            .forDevice(deviceId)
                            .forTable(INTERPRETER.getTableId("break_1"))
                            .fromApp(this.applicationId)
                            .withPriority(DefaultFlowRule.MAX_PRIORITY).makePermanent();

            Bmv2ExtensionSelector.Builder selectorBuilder = Bmv2ExtensionSelector.builder();
            selectorBuilder.matchLpm("ipv4", "dstAddr", 0x0a000001, 32);
            builder.withSelector(DefaultTrafficSelector.builder()
                    .extension(selectorBuilder.build(), deviceId).build());

            Bmv2ExtensionTreatment.Builder treatmentBuilder = Bmv2ExtensionTreatment.builder();
            treatmentBuilder.setActionName("gen_break")
                    .addParameter("id", 1);
            builder.withTreatment(DefaultTrafficTreatment.builder()
                    .extension(treatmentBuilder.build(), deviceId)
                    .build());
            installRule(builder.build());
        } catch (Exception e) {
            log.error(e.toString());
        }

        try {
            FlowRule.Builder builder =
                    DefaultFlowRule.builder()
                            .forDevice(deviceId)
                            .forTable(INTERPRETER.getTableId("break_1"))
                            .fromApp(this.applicationId)
                            .withPriority(DefaultFlowRule.MAX_PRIORITY).makePermanent();

            Bmv2ExtensionSelector.Builder selectorBuilder = Bmv2ExtensionSelector.builder();
            selectorBuilder.matchLpm("ipv4", "dstAddr", 0x0a000002, 32);
            builder.withSelector(DefaultTrafficSelector.builder()
                    .extension(selectorBuilder.build(), deviceId).build());

            Bmv2ExtensionTreatment.Builder treatmentBuilder = Bmv2ExtensionTreatment.builder();
            treatmentBuilder.setActionName("gen_break")
                    .addParameter("id", 1);
            builder.withTreatment(DefaultTrafficTreatment.builder()
                    .extension(treatmentBuilder.build(), deviceId)
                    .build());
            installRule(builder.build());
        } catch (Exception e) {
            log.error(e.toString());
        }

    /* ipv4_nhop */
    try {
        FlowRule.Builder builder =
                DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .forTable(INTERPRETER.getTableId("ipv4_nhop"))
                        .fromApp(this.applicationId)
                        .withPriority(DefaultFlowRule.MAX_PRIORITY).makePermanent();

        Bmv2ExtensionSelector.Builder selectorBuilder = Bmv2ExtensionSelector.builder();
        selectorBuilder.matchLpm("ipv4", "dstAddr", 0x0a000001, 32);
        builder.withSelector(DefaultTrafficSelector.builder()
                .extension(selectorBuilder.build(), deviceId).build());

        Bmv2ExtensionTreatment.Builder treatmentBuilder = Bmv2ExtensionTreatment.builder();
        treatmentBuilder.setActionName("set_nhop")
                .addParameter("nhop_ipv4", 0x0a000001);
        builder.withTreatment(DefaultTrafficTreatment.builder()
                .extension(treatmentBuilder.build(), deviceId)
                .build());
        installRule(builder.build());
    } catch (Exception e) {
        log.error(e.toString());
    }

    try {
        FlowRule.Builder builder =
                DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .forTable(INTERPRETER.getTableId("ipv4_nhop"))
                        .fromApp(this.applicationId)
                        .withPriority(DefaultFlowRule.MAX_PRIORITY).makePermanent();

        Bmv2ExtensionSelector.Builder selectorBuilder = Bmv2ExtensionSelector.builder();
        selectorBuilder.matchLpm("ipv4", "dstAddr", 0x0a000002, 32);
        builder.withSelector(DefaultTrafficSelector.builder()
                .extension(selectorBuilder.build(), deviceId).build());

        Bmv2ExtensionTreatment.Builder treatmentBuilder = Bmv2ExtensionTreatment.builder();
        treatmentBuilder.setActionName("set_nhop")
                .addParameter("nhop_ipv4", 0x0a000002);
        builder.withTreatment(DefaultTrafficTreatment.builder()
                .extension(treatmentBuilder.build(), deviceId)
                .build());
        installRule(builder.build());
    } catch (Exception e) {
        log.error(e.toString());
    }

    /* forward_table */
    try {
        FlowRule.Builder builder =
                DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .forTable(INTERPRETER.getTableId("forward_table"))
                        .fromApp(this.applicationId)
                        .withPriority(DefaultFlowRule.MAX_PRIORITY).makePermanent();

        Bmv2ExtensionSelector.Builder selectorBuilder = Bmv2ExtensionSelector.builder();
        selectorBuilder.matchExact("routing_metadata", "nhop_ipv4", 0x0a000001);

        builder.withSelector(DefaultTrafficSelector.builder()
                .extension(selectorBuilder.build(), deviceId).build());

        Bmv2ExtensionTreatment.Builder treatmentBuilder = Bmv2ExtensionTreatment.builder();
        treatmentBuilder.setActionName("set_dmac")
                .addParameter("dmac", 0x802200010001L)
                .addParameter("port", (short) 1);
        builder.withTreatment(DefaultTrafficTreatment.builder()
                .extension(treatmentBuilder.build(), deviceId)
                .build());
        installRule(builder.build());
    } catch (Exception e) {
        log.error(e.toString());
    }

    /* forward_table */
    try {
        FlowRule.Builder builder =
                DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .forTable(INTERPRETER.getTableId("forward_table"))
                        .fromApp(this.applicationId)
                        .withPriority(DefaultFlowRule.MAX_PRIORITY).makePermanent();

        Bmv2ExtensionSelector.Builder selectorBuilder = Bmv2ExtensionSelector.builder();
        selectorBuilder.matchExact("routing_metadata", "nhop_ipv4", 0x0a000002);

        builder.withSelector(DefaultTrafficSelector.builder()
                .extension(selectorBuilder.build(), deviceId).build());

        Bmv2ExtensionTreatment.Builder treatmentBuilder = Bmv2ExtensionTreatment.builder();
        treatmentBuilder.setActionName("set_dmac")
                .addParameter("dmac", 0x802200010002L)
                .addParameter("port", (short) 2);
        builder.withTreatment(DefaultTrafficTreatment.builder()
                .extension(treatmentBuilder.build(), deviceId)
                .build());
        installRule(builder.build());
    } catch (Exception e) {
        log.error(e.toString());
    }

    /* forward_table */
    try {
        FlowRule.Builder builder =
                DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .forTable(INTERPRETER.getTableId("send_frame"))
                        .fromApp(this.applicationId)
                        .withPriority(DefaultFlowRule.MAX_PRIORITY).makePermanent();

        Bmv2ExtensionSelector.Builder selectorBuilder = Bmv2ExtensionSelector.builder();
        selectorBuilder.matchExact("standard_metadata", "egress_port", 1);

        builder.withSelector(DefaultTrafficSelector.builder()
                .extension(selectorBuilder.build(), deviceId).build());

        Bmv2ExtensionTreatment.Builder treatmentBuilder = Bmv2ExtensionTreatment.builder();
        treatmentBuilder.setActionName("set_smac")
                .addParameter("smac", 0x802200010002L);
        builder.withTreatment(DefaultTrafficTreatment.builder()
                .extension(treatmentBuilder.build(), deviceId)
                .build());
        installRule(builder.build());
    } catch (Exception e) {
        log.error(e.toString());
    }
    /* forward_table */
    try {
        FlowRule.Builder builder =
                DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .forTable(INTERPRETER.getTableId("send_frame"))
                        .fromApp(this.applicationId)
                        .withPriority(DefaultFlowRule.MAX_PRIORITY).makePermanent();

        Bmv2ExtensionSelector.Builder selectorBuilder = Bmv2ExtensionSelector.builder();
        selectorBuilder.matchExact("standard_metadata", "egress_port", 2);

        builder.withSelector(DefaultTrafficSelector.builder()
                .extension(selectorBuilder.build(), deviceId).build());

        Bmv2ExtensionTreatment.Builder treatmentBuilder = Bmv2ExtensionTreatment.builder();
        treatmentBuilder.setActionName("set_smac")
                .addParameter("smac", 0x802200010001L);
        builder.withTreatment(DefaultTrafficTreatment.builder()
                .extension(treatmentBuilder.build(), deviceId)
                .build());
        installRule(builder.build());
    } catch (Exception e) {
        log.error(e.toString());
    }
}

    /**
     * The device is removed.
     *
     * @param device device that is going to be removed
     */
    private void removeDevice(Device device) {
        deviceIdList.remove(deviceIdList);
    }

    private void processPacketIn(Bmv2Device bmv2Device, int i, ImmutableByteSequence immutableByteSequence) {
        log.info("New packet from [{}] {}", bmv2Device.thriftServerHost(), immutableByteSequence.toString());
    }


    /**
     * Run tasks in the thread group.
     *
     * @param task runnable task
     */
    private void spawnTask(Runnable task) {
        executorService.execute(task);
    }

    private class InternalTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent topologyEvent) {
            log.info("Topology event : {} {}", topologyEvent.type(), topologyEvent.reasons());
        }

        @Override
        public boolean isRelevant(TopologyEvent event) {
            return event.type() == TopologyEvent.Type.TOPOLOGY_CHANGED;
        }
    }


    /**
     * Internal device event listener.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent deviceEvent) {
            switch (deviceEvent.type()) {
                case DEVICE_ADDED:
                    //spawnTask(() -> deployDevice(deviceEvent.subject()));
                    break;
                case DEVICE_REMOVED:
                    //spawnTask(() -> removeDevice(deviceEvent.subject()));
                    break;
                default:
                    log.info("Unconcerning device event {} {}", deviceEvent.type().toString(),
                            deviceEvent.subject().id());
            }

        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.type() == DeviceEvent.Type.DEVICE_ADDED
                    || event.type() == DeviceEvent.Type.DEVICE_REMOVED;
        }
    }

    private class InternalPakcetListener implements Bmv2PacketListener {
        @Override
        public void handlePacketIn(Bmv2Device bmv2Device, int i, ImmutableByteSequence immutableByteSequence) {
            spawnTask(() -> processPacketIn(bmv2Device, i, immutableByteSequence));
        }
    }
}
