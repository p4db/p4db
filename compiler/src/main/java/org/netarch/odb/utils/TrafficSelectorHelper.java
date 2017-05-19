package org.netarch.odb.utils;

import org.netarch.odb.runtime.MatchResult;
import org.netarch.odb.runtime.Value;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class TrafficSelectorHelper {

    /* Headers */
    private static final String PMI = "pmiInstance";
    private static final String UMI = "umiInstance";
    private static final String STD = "standard_metadata";
    /* PMI Fields */
    private static final String PROGRAM_ID = "pmi_program_id";
    private static final String STAGE_ID = "pmi_stage_id";
    private static final String POLICY_ID = "pmi_policy_id";
    private static final String MATCH_RESULT = "pmi_match_chain_result";
    private static final String ACTION_ID = "pmi_action_chain_id";
    private static final String OP = "pmi_op";
    /* UMI Fields */
    private static final String USER_META = "umi_user_metadata";
    private static final String LOAD_HEADER = "umi_load_header";
    /* STD Fields */
    private static final String INGRESS_PORT = "ingress_port";
    private static final String PACKET_LENGTH = "packet_length";
    private static final String EGRESS_SPEC = "egress_spec";
    private static final String EGRESS_PORT = "egress_port";
    private static final String EGRESS_INSTANCE = "egress_instance";
    private static final String INSTANCE_TYPE = "instance_type";
    private static final String CLONE_SPEC = "clone_spec";
    private final Logger logger = LoggerFactory.getLogger(TrafficSelectorHelper.class);
    private Bmv2ExtensionSelector.Builder builder;

    /**
     * Create a new traffic selector builder.
     *
     * @param configuration bmv2 configuration
     */
    public TrafficSelectorHelper(Bmv2Configuration configuration) {
        this.builder = Bmv2ExtensionSelector
                .builder()
                .forConfiguration(configuration);
    }

    /**
     * Add policy id into the traffic selector.
     *
     * @param policyId policy id
     * @return this
     */
    public TrafficSelectorHelper withPolicyId(short policyId) {
        this.builder.matchExact(PMI, POLICY_ID, policyId);
        return this;
    }

    /**
     * Add program id match into the traffic selector.
     *
     * @param programId program id (instance id)
     * @return this
     */
    public TrafficSelectorHelper withProgramId(byte programId) {
        byte[] data = new byte[1];
        data[0] = programId;
        this.builder.matchExact(PMI, PROGRAM_ID, data);
        return this;
    }

    /**
     * Add stage id key into the traffic selector.
     *
     * @param stageId stage id
     * @return this
     */
    public TrafficSelectorHelper withStageId(byte stageId) {
        byte[] data = new byte[1];
        data[0] = stageId;
        this.builder.matchExact(PMI, STAGE_ID, data);
        return this;
    }

    /**
     * Add header match into the traffic selector.
     *
     * @param value header value
     * @param mask  header mask
     * @return this
     */
    public TrafficSelectorHelper withHeaderMatch(Value value, Value mask) {
        this.builder.matchTernary(UMI, LOAD_HEADER, value.getValue(), mask.getValue());
        return this;
    }

    /**
     * Add user metadata match into the traffic selector.
     *
     * @param value user metadata value
     * @param mask  user metadata mask
     * @return this
     */
    public TrafficSelectorHelper withUserMetadata(Value value, Value mask) {
        this.builder.matchTernary(UMI, USER_META, value.getValue(), mask.getValue());
        return this;
    }

    /**
     * set up operator.
     *
     * @param op operation
     * @return this
     */
    public TrafficSelectorHelper withOperation(byte op) {
        this.builder.matchExact(PMI, OP, op);
        return this;
    }

    /**
     * Add match results into traffic selector.
     *
     * @param matchResult match result.
     * @return this
     */
    public TrafficSelectorHelper withMatchResult(MatchResult matchResult) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[6]);
        byteBuffer.putShort(matchResult.getStdMetadata());
        byteBuffer.putShort(matchResult.getMetadata());
        byteBuffer.putShort(matchResult.getHeader());
        this.builder.matchExact(PMI, MATCH_RESULT, byteBuffer.array());
        return this;
    }


    /**
     * Add action id into traffic selector.
     *
     * @param actionId action id
     * @return this
     */
    public TrafficSelectorHelper withActionId(long actionId) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[6]);
        byteBuffer.putShort((short) (actionId >> 32));
        byteBuffer.putShort((short) (actionId >> 16));
        byteBuffer.putShort((short) (actionId >> 0));
        this.builder.matchExact(PMI, ACTION_ID, byteBuffer.array());
        return this;
    }

    /**
     * Add standard metadata into traffic selector.
     *
     * @param ingressPort    ingress port
     * @param packetLength   packet length
     * @param egressSpec     specify egress port
     * @param egressPort     still unknown
     * @param egressInstance still unknowm
     * @param instanceType   still unknown
     * @param cloneSpec      still unknown
     * @param matchBitMap    match types
     * @return this
     */
    public TrafficSelectorHelper withStandardMetadata(short ingressPort,
                                                      int packetLength,
                                                      short egressSpec,
                                                      short egressPort,
                                                      int egressInstance,
                                                      int instanceType,
                                                      int cloneSpec,
                                                      int matchBitMap) {
        /* Match ingress port */
        this.builder.matchTernary(STD,
                INGRESS_PORT,
                ingressPort,
                (short) ((matchBitMap & 0x1) == 0 ? 0 : 0xFFFF));

        matchBitMap >>= 1;
        /* Match packet lenght */
        this.builder.matchTernary(STD,
                PACKET_LENGTH,
                packetLength,
                ((matchBitMap & 0x1) == 0 ? 0 : 0xFFFFFFFF));

        matchBitMap >>= 1;
        /* Match packet lenght */
        this.builder.matchTernary(STD,
                EGRESS_SPEC,
                egressSpec,
                (short) ((matchBitMap & 0x1) == 0 ? 0 : 0xFFFF));

        matchBitMap >>= 1;
        /* Match packet lenght */
        this.builder.matchTernary(STD,
                EGRESS_PORT,
                egressPort,
                (short) ((matchBitMap & 0x1) == 0 ? 0 : 0xFFFF));

        matchBitMap >>= 1;
        /* Match packet lenght */
        this.builder.matchTernary(STD,
                EGRESS_INSTANCE,
                egressInstance,
                ((matchBitMap & 0x1) == 0 ? 0 : 0xFFFFFFFF));

        matchBitMap >>= 1;
        /* Match packet lenght */
        this.builder.matchTernary(STD,
                INSTANCE_TYPE,
                instanceType,
                ((matchBitMap & 0x1) == 0 ? 0 : 0xFFFFFFFF));

        matchBitMap >>= 1;
        /* Match packet lenght */

        /*
        this.builder.matchTernary(STD,
                CLONE_SPEC,
                cloneSpec,
                ((matchBitMap & 0x1) == 0 ? 0 : 0xFFFFFFFF));
        */
        return this;
    }

    /**
     * Build the traffic selector builder.
     *
     * @return traffic selector.
     */
    public Bmv2ExtensionSelector build() {
        return this.builder.build();
    }
}
