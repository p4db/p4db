package org.netarch.odb.compiler;

import com.google.common.collect.ImmutableBiMap;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.context.Bmv2InterpreterException;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;


public class Vp4Interpreter implements Bmv2Interpreter {


    /* Standard metadata fields. */
    public static final String STD_META = "standard_metadata";
    public static final String INGRESS_PORT = "ingress_port";
    public static final String PACKET_LENGTH = "packet_length";
    public static final String EGRESS_SPEC = "eress_spec";
    public static final String EGRESS_PORT = "egress_port";
    public static final String EGRESS_INSTANCE = "egress_instance";
    public static final String INSTANCE_TYPE = "instance_type";
    public static final String CLONE_SPEC = "clonc_spec";

    public static final String INGRESS_PORT_KEY = STD_META + "." + INGRESS_PORT;
    public static final String PACKET_LENGTH_KEY = STD_META + "." + PACKET_LENGTH;
    public static final String EGRESS_SPEC_KEY = STD_META + "." + EGRESS_SPEC;
    public static final String EGRESS_PORT_KEY = STD_META + "." + EGRESS_PORT;
    public static final String EGRESS_INSTANCE_KEY = STD_META + "." + EGRESS_INSTANCE;
    public static final String INSTANCE_TYPE_KEY = STD_META + "." + INSTANCE_TYPE;
    public static final String CLONE_SPEC_KEY = STD_META + "." + CLONE_SPEC;

    /* Actions */
    public static final String ACTION_SET_INITIAL_CONFIG = "action_set_initial_config";
    public static final String ACTION_SET_ACTION_ID = "action_set_action_id";
    public static final String ACTION_SET_STAGE_AND_BITMAP = "action_set_stage_and_bitmap";
    public static final String ACTION_SET_MATCH_RESULT = "action_set_match_result";


    public static final String ACTION_MOD_HEADER_WITH_CONST = "action_mod_header_with_const";
    public static final String ACTION_MOD_HEADER_WITH_HEADER = "action_mod_header_with_header_1";
    public static final String ACTION_MOD_HEADER_WITH_META = "action_mod_header_with_meta_1";

    public static final String ACTION_MOD_META_WITH_CONST = "action_mod_meta_with_const";
    public static final String ACTION_MOD_META_WITH_HEADER = "action_mod_meta_with_header_1";
    public static final String ACTION_MOD_META_WITH_META = "action_mod_meta_with_meta_1";


    public static final String CONFIG_AT_INITIAL = "table_config_at_initial";
    public static final String HEADER_MATCH = "table_header_match";
    public static final String META_MATCH = "table_user_meta";
    public static final String STD_META_MATCH = "table_std_meta";
    public static final String TABLE_MATCH_RESULT = "table_match_result";
    public static final String TABLE_MOD_HEADER_WITH_CONST = "table_mod_header_with_const";
    public static final String TABLE_MOD_HEADER_WITH_HEADER = "table_mod_header_with_header";
    public static final String TABLE_MOD_HEADER_WITH_META = "table_mod_header_with_meta";

    public static final String TABLE_MOD_META_WITH_CONST = "table_mod_meta_with_const";
    public static final String TABLE_MOD_META_WITH_HEADER = "table_mod_meta_with_header";
    public static final String TABLE_MOD_META_WITH_META = "table_mod_meta_with_meta";

    public static final byte HEADER_MATCH_BIT = 4;
    public static final byte USER_META_MATCH_BIT = 2;
    public static final byte STD_META_MATCH_BIT = 1;

    public static final long ACTION_COUNTER_BIT  = 1L << 21;
    public static final long ACTION_DROP_BIT     = 1L << 17;
    public static final long ACTION_MOD_STD_META = 1L << 37;
    public static final long ACTION_MOD_META_WITH_CONST_STAGE_BIT = 1L << 34;
    public static final long ACTION_SUBTRACT_BIT = 1L << 7;
    public static final long ACTION_GENERATE_DEGIST = 1L << 25;

    public static final byte OP_EQUAL = 0;
    public static final byte OP_LARGER = 1;
    public static final byte OP_SMALLER = 2;


    private static final ImmutableBiMap.Builder<Integer, String> tableIdMapBuilder = new ImmutableBiMap.Builder<>();

    static {
        int counter = 0;
        tableIdMapBuilder.put(counter++, "table_config_at_initial");
        for (int i = 0; i < Compiler.STAGE_NUM; i++) {
            tableIdMapBuilder.put(counter++, "table_get_expression_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_branch_1_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_branch_2_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_branch_3_" + "stage" + i);

            tableIdMapBuilder.put(counter++, "table_header_match_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_std_meta_match_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_user_meta_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_match_result_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_mod_header_with_meta_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_mod_meta_with_meta_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_mod_header_with_header_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_mod_meta_with_header_" + "stage" + i);

            tableIdMapBuilder.put(counter++, "table_mod_header_with_const_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_mod_meta_with_const_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_add_header_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_remove_header_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_mod_std_meta_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_generate_digest_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_add_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_subtract_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_register_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_counter_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_hash_" + "stage" + i);
            tableIdMapBuilder.put(counter++, "table_action_profile_" + "stage" + i);
        }
        tableIdMapBuilder.put(counter++, "table_config_at_end");
        tableIdMapBuilder.put(counter++, "table_config_at_egress");
        tableIdMapBuilder.put(counter++, "table_checksum");
        tableIdMapBuilder.put(counter, "dh_deparse");
    }

    /**
     * Whether the interpreter contains the table.
     *
     * @param tableName table name.
     * @return true if the interpreter contains the table, otherwise false
     */
    public static boolean containTable(String tableName) {
        return tableIdMapBuilder.build().inverse().containsKey(tableName);
    }

    /**
     * Whether the interpreter contains the table.
     *
     * @param tableName table name
     * @param stageName stage name
     * @return true if the interpreter contains the table, otherwise false
     */
    public static boolean containTable(String tableName, String stageName) {
        return tableIdMapBuilder.build().inverse().containsKey(tableName + "_" + stageName);
    }

    /**
     * Get table id from the table name.
     *
     * @param tableName table name
     * @return table id
     */
    public static int getTableId(String tableName) {
        return tableIdMapBuilder.build().inverse().get(tableName);
    }

    /**
     * Get table id from the table name and stage name.
     *
     * @param tableName table name
     * @param stageName stage name
     * @return this
     */
    public static int getTableId(String tableName, String stageName) {
        return tableIdMapBuilder.build().inverse().get(tableName + "_" + stageName);
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


    public static byte getNextStage(byte stageId) {
        return stageId;
    }

    public static byte getCurrentStage(byte stageId) {
        return (byte) (stageId - 1);
    }

    public static byte getNextConditionalStage(byte stageId) {
        return (byte) (stageId + 8);
    }

    public static byte getCurrentConditionalStage(byte stageId) {
        return (byte) (stageId + 7);
    }

}
