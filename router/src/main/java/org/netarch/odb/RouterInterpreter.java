package org.netarch.odb;


import com.google.common.collect.ImmutableBiMap;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.context.Bmv2InterpreterException;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;

public class RouterInterpreter implements Bmv2Interpreter {
    private static final ImmutableBiMap.Builder<Integer, String> tableIdMapBuilder = new ImmutableBiMap.Builder<>();

    static {
        int counter = 0;
        tableIdMapBuilder.put(counter++, "ipv4_nhop");
        tableIdMapBuilder.put(counter++, "forward_table");
        tableIdMapBuilder.put(counter++, "send_frame");
        tableIdMapBuilder.put(counter++, "predication_1");
        tableIdMapBuilder.put(counter++, "predication_2");
        tableIdMapBuilder.put(counter++, "watch_1");
        tableIdMapBuilder.put(counter++, "watch_2");
        tableIdMapBuilder.put(counter++, "watch_3");
        tableIdMapBuilder.put(counter++, "watch_4");
        tableIdMapBuilder.put(counter++, "ipv4_nhop_match");
        tableIdMapBuilder.put(counter++, "ipv4_nhop_action");
        tableIdMapBuilder.put(counter++, "match_1");
        tableIdMapBuilder.put(counter++, "break_1");
        tableIdMapBuilder.put(counter++, "action_1");
    }

    @Override
    public ImmutableBiMap<Integer, String> tableIdMap() {
        return tableIdMapBuilder.build();
    }

    @Override
    public ImmutableBiMap<Criterion.Type, String> criterionTypeMap() {
        return ImmutableBiMap.of();
    }

    @Override
    public Bmv2Action mapTreatment(TrafficTreatment trafficTreatment, Bmv2Configuration bmv2Configuration) throws Bmv2InterpreterException {
        return null;
    }

    public int getTableId(String tableName) {
        return tableIdMapBuilder.build().inverse().get(tableName);
    }
}
