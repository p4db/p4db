package org.netarch.odb.runtime;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.netarch.odb.compiler.Vp4Interpreter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class InstanceRule {
    private int stageId;
    private List<FlowKey> keys;
    private Map<String, FlowKey> headerKeys;
    private Map<String, FlowKey> metadataKeys;
    private Map<String, FlowKey> stdMetadataKeys;
    private CompoundAction action;

    /**
     * Create a Instance rule with specific stage id.
     *
     * @param stageId
     */
    public InstanceRule(int stageId) {
        this.stageId = stageId;
        this.keys = Lists.newArrayList();
        this.headerKeys = Maps.newHashMap();
        this.metadataKeys = Maps.newHashMap();
        this.stdMetadataKeys = Maps.newHashMap();
        this.action = null;
    }

    /**
     * Get stage id.
     *
     * @return stage id
     */
    public int getStageId() {
        return stageId;
    }

    /**
     * Get the compound action of the instance rule.
     *
     * @return compound action
     */
    public CompoundAction getAction() {
        return action;
    }

    /**
     * Set compound of the flow rules.
     *
     * @param action compound action
     * @return this
     */
    public InstanceRule setAction(CompoundAction action) {
        this.action = action;
        return this;
    }

    /**
     * Get flow keys.
     *
     * @return key list
     */
    public List<FlowKey> getKeys() {
        return keys;
    }

    /**
     * Add a flow key into the InstanceRule.
     *
     * @param key a flow key with the field name
     * @return this
     */
    public InstanceRule addKey(FlowKey key) {
        this.keys.add(key);
        if (key.getType() == FlowKeyType.METADATA_TYPE) {
            if (key.getHeaderName().equals(Vp4Interpreter.STD_META)) {
                this.stdMetadataKeys.put(key.getKeyName(), key);
            } else {
                this.metadataKeys.put(key.getKeyName(), key);
            }
        } else {
            this.headerKeys.put(key.getKeyName(), key);
        }
        return this;
    }

    /**
     * Get key values.
     *
     * @param keyName      key name
     * @param defaultValue default value.
     * @return key value if the key exists, otherwise default value
     */
    public Short getOrDefault(String keyName, Short defaultValue) {
        FlowKey key = null;
        key = headerKeys.getOrDefault(keyName, null);
        if (key != null) {
            return key.getValue().getShort();
        }

        key = metadataKeys.getOrDefault(keyName, null);
        if (key != null) {
            return key.getValue().getShort();
        }

        key = stdMetadataKeys.getOrDefault(keyName, null);
        if (key != null) {
            return key.getValue().getShort();
        }

        return defaultValue;
    }

    /**
     * Get key values.
     *
     * @param keyName      key name
     * @param defaultValue default value.
     * @return key value if the key exists, otherwise default value
     */
    public Integer getOrDefault(String keyName, Integer defaultValue) {
        FlowKey key = headerKeys.getOrDefault(keyName, null);
        if (key != null) {
            return key.getValue().getInt();
        }

        key = metadataKeys.getOrDefault(keyName, null);
        if (key != null) {
            return key.getValue().getInt();
        }

        key = stdMetadataKeys.getOrDefault(keyName, null);
        if (key != null) {
            return key.getValue().getInt();
        }

        return defaultValue;
    }


    /**
     * Get key values.
     *
     * @param keyName      key name
     * @param defaultValue default value.
     * @return key value if the key exists, otherwise default value
     */
    public Long getOrDefault(String keyName, Long defaultValue) {
        FlowKey key = null;
        key = headerKeys.getOrDefault(keyName, null);
        if (key != null) {
            return key.getValue().getLong();
        }

        key = metadataKeys.getOrDefault(keyName, null);
        if (key != null) {
            return key.getValue().getLong();
        }

        key = stdMetadataKeys.getOrDefault(keyName, null);
        if (key != null) {
            return key.getValue().getLong();
        }

        return defaultValue;
    }


    /**
     * Get header match keys.
     *
     * @return header match keys
     */
    public Collection<FlowKey> getHeaderKeys() {
        return this.headerKeys.values();
    }

    /**
     * Get metadata match keys.
     *
     * @return metadata match keys
     */
    public Collection<FlowKey> getMetadataKeys() {
        return this.metadataKeys.values();
    }

    /**
     * Get std metadata keys.
     *
     * @return std metadata keys
     */
    public Collection<FlowKey> getStdMetadataKeys() {
        return this.stdMetadataKeys.values();
    }
}
