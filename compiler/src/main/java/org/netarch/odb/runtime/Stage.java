package org.netarch.odb.runtime;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.netarch.odb.compiler.Vp4Interpreter;
import org.netarch.odb.model.FieldModel;
import org.netarch.odb.model.MatchKeyModel;
import org.netarch.odb.model.TableModel;

import java.util.List;
import java.util.Map;

public class Stage {
    private static Short stdMatchResultCounter = 1;
    private static Short metadataMatchResultCounter = 1;
    private static Short headerMatchResultCounter = 1;
    private int stageId;
    private TableModel model;
    private int maxHeaderBitLength;
    private int maxMetadataBitLength;
    private String nextStage;
    private Map<String, MatchKeyModel> matchKeys;
    private List<MatchKeyModel> headerMatchKeys;
    private List<MatchKeyModel> metadataMatchKeys;
    private List<MatchKeyModel> stdMetatdataKeys;

    /**
     * Create a stage with stage id and logical table model.
     *
     * @param stageId    stage id
     * @param tableModel table model
     */
    public Stage(int stageId, TableModel tableModel) {
        this.stageId = stageId;
        this.model = tableModel;
        this.maxHeaderBitLength = 0;
        this.maxMetadataBitLength = 0;
        this.nextStage = null;
        this.matchKeys = Maps.newHashMap();
        this.headerMatchKeys = Lists.newArrayList();
        this.metadataMatchKeys = Lists.newArrayList();
        this.stdMetatdataKeys = Lists.newArrayList();

        tableModel.getKeys().forEach(key -> {
            matchKeys.put(key.getName(), key);
            FieldModel field = key.field();
            if (field.isMetadata()) {
                if (field.getHeaderName().equals(Vp4Interpreter.STD_META)) {
                    this.stdMetatdataKeys.add(key);
                } else {
                    if (field.getBitOffset() + field.getHeader().getBitOffset() > this.maxMetadataBitLength) {
                        this.maxMetadataBitLength = field.getBitOffset() + field.getHeader().getBitOffset();
                    }
                    metadataMatchKeys.add(key);
                }
            } else {
                if (field.getBitOffset() + field.getHeader().getBitOffset() > this.maxMetadataBitLength) {
                    this.maxHeaderBitLength = field.getBitOffset() + field.getHeader().getBitOffset();
                }
                headerMatchKeys.add(key);
            }
        });


        this.nextStage = tableModel.getNext();

    }

    /**
     * Get the stage id.
     *
     * @return stage id
     */
    public byte getId() {
        return (byte) stageId;
    }

    /**
     * Get the table model of the stage
     *
     * @return table model
     */
    public TableModel getModel() {
        return model;
    }

    /**
     * Get name of the stage
     *
     * @return stage name
     */
    public String getName() {
        return model.getName();
    }

    /**
     * Judge whether a key belong to the stage.
     *
     * @param key flow key
     * @return true if it belongs to the stage otherwise false
     */
    public boolean isTableKey(FlowKey key) {
        boolean isTbalekey = false;
        for (String name : matchKeys.keySet()) {
            if (name.equals(key.getKeyName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get match key model from the stage.
     *
     * @param keyName match key name
     * @return match key model
     */
    MatchKeyModel getMatchKey(String keyName) {
        return matchKeys.get(keyName);
    }

    /**
     * Is the stage match metadata fields.
     *
     * @return true if matching metadata fields, otherwise false
     */
    boolean isMatchMetadata() {
        return !metadataMatchKeys.isEmpty();
    }

    /**
     * Get metadata key list.
     *
     * @return metadata key list
     */
    public List<MatchKeyModel> getMetadataMatchKeys() {
        return metadataMatchKeys;
    }

    /**
     * Is the stage match header fields.
     *
     * @return true if matching header fields, otherwise false
     */
    boolean isMatchHeader() {
        return !headerMatchKeys.isEmpty();
    }

    /**
     * Is the stage match standard metadata fields.
     *
     * @return true if matching header fields, otherwise false
     */
    boolean isMatchStdMetadata() {
        return !stdMetatdataKeys.isEmpty();
    }

    /**
     * Get standard metadata keys.
     *
     * @return standard metadata keys
     */
    public List<MatchKeyModel> getStdMetatdataKeys() {
        return stdMetatdataKeys;
    }

    /**
     * Get header key list.
     *
     * @return header match key list
     */
    public List<MatchKeyModel> getHeaderMatchKeys() {
        return headerMatchKeys;
    }

    /**
     * Get max header bit length.
     *
     * @return get max header bit length
     */
    int getMaxHeaderBitLength() {
        return maxHeaderBitLength;
    }

    /**
     * Get max metadata bit length.
     *
     * @return metadata bit length
     */
    int getMaxMetadataBitLength() {
        return maxMetadataBitLength;
    }

    /**
     * Get next stage.
     *
     * @return next stage
     */
    public String getNextStage() {
        return nextStage;
    }

    /**
     * Set next stage.
     *
     * @param next next stage
     * @return this
     */
    public Stage setNextStage(String next) {
        this.nextStage = nextStage;
        return this;
    }

    /**
     * Compute the match result from the stage key.
     *
     * @return match result
     */
    public MatchResult computeMatchResult(List<FlowKey> flowKeys) {
        return new MatchResult(createStdMetadataMatchResult(),
                createMetadataMatchResult(),
                createHeaderMatchResult());
    }

    /**
     * Get match bitmap.
     *
     * @return match bitmap
     */
    public Byte getMatchBitmap() {
        byte matchType = 0;

        if (isMatchHeader()) {
            matchType |= 4;
        }

        if (isMatchMetadata()) {
            matchType |= 2;
        }

        if (isMatchStdMetadata()) {
            matchType |= 1;
        }
        return matchType;
    }

    /**
     * Allocate a new standard metadata match result
     *
     * @return new std if match standard metadata
     */
    private Short createStdMetadataMatchResult() {
        if (isMatchStdMetadata()) {
            Short ret = stdMatchResultCounter;
            stdMatchResultCounter = (short) ((ret + 1) % 20000);
            return ret;
        } else {
            return 0;
        }
    }

    /**
     * Allocate a new metadata match result.
     *
     * @return new metadata match result
     */
    private Short createMetadataMatchResult() {
        if (isMatchMetadata()) {
            Short ret = metadataMatchResultCounter;
            metadataMatchResultCounter = (short) ((ret + 1) % 20000);
            return ret;
        } else {
            return 0;
        }
    }

    /**
     * Allocate a new header match hresult.
     *
     * @return new header match result
     */
    private Short createHeaderMatchResult() {
        if (isMatchHeader()) {
            Short ret = headerMatchResultCounter;
            headerMatchResultCounter = (short) ((ret + 1) % 20000);
            return ret;
        } else {
            return 0;
        }
    }
}
