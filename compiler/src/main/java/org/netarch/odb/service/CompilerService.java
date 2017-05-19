package org.netarch.odb.service;

import com.eclipsesource.json.JsonObject;
import org.netarch.odb.model.ProgramModel;
import org.netarch.odb.runtime.Instance;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowRule;

import java.util.List;

public interface CompilerService {
    /**
     * Compile json to Program.
     *
     * @param json     json object of program
     * @param progName Program name
     * @return Program model
     */
    ProgramModel compile(JsonObject json, String progName);

    /**
     * Run a program on the data plane.
     *
     * @param program program model
     * @return a instance running on thedata plane.
     */
    Instance run(ProgramModel program, DeviceId deviceId, int policyId);

    /**
     * Stop an instance running on the data plane.
     *
     * @param instance running instance
     */
    void stop(Instance instance);

    /**
     * Install a rule into devices.
     *
     * @param rule flow rule (provided by ONOS)
     * @return this
     */
    CompilerService installRule(FlowRule rule);

    /**
     * Remove a rule from the device.
     *
     * @param rule target flow rule
     * @return this
     */
    CompilerService removeRule(FlowRule rule);

    /**
     * Get all devices.
     *
     * @return device lists.
     */
    List<DeviceId> getDevices();

    /**
     * Get BMv2 configuration.
     *
     * @return the device configuration
     */
    Bmv2Configuration getConfiguration();
}
