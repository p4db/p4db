package org.netarch.odb.model;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.netarch.odb.primitive.PrimitiveType;
import org.netarch.odb.utils.DualKeyMap;

import java.util.List;
import java.util.Set;

public class ProgramModel {
    private final String name;
    private final int id;
    private final JsonObject json;
    private final DualKeyMap<HeaderTypeModel> headerTypes = new DualKeyMap<>();
    private final DualKeyMap<HeaderModel> headers = new DualKeyMap<>();
    private final DualKeyMap<ActionModel> actions = new DualKeyMap<>();
    private final DualKeyMap<TableModel> tables = new DualKeyMap<>();
    private final DualKeyMap<ParserStateModel> parsers = new DualKeyMap<>();

    public ProgramModel(String name, int id, JsonObject json) {
        this.name = name;
        this.id = id;
        this.json = json;
    }

    public HeaderTypeModel headerType(int id) {
        return headerTypes.get(id);
    }

    public HeaderTypeModel headerType(String name) {
        return headerTypes.get(name);
    }

    public List<HeaderTypeModel> headerTypes() {
        return ImmutableList.copyOf(headerTypes.sortedMap().values());
    }

    public HeaderModel getHeader(int id) {
        return headers.get(id);
    }

    public HeaderModel getHeader(String name) {
        return headers.get(name);
    }

    public List<HeaderModel> headers() {
        return ImmutableList.copyOf(headers.sortedMap().values());
    }

    public ActionModel action(int id) {
        return actions.get(id);
    }

    public ActionModel action(String name) {
        return actions.get(name);
    }

    public List<ActionModel> actions() {
        return ImmutableList.copyOf(actions.sortedMap().values());
    }

    public TableModel table(int id) {
        return tables.get(id);
    }

    public TableModel table(String name) {
        return tables.get(name);
    }

    public List<TableModel> tables() {
        return ImmutableList.copyOf(tables.sortedMap().values());
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public JsonObject json() {
        return this.json;
    }


    public ActionModel getActionModel(String actionName) {
        return actions.get(actionName);
    }

    /**
     * Generates a hash code for this BMv2 configuration. The hash function is based solely on the JSON backing this
     * configuration.
     */
    @Override
    public int hashCode() {
        return json.hashCode();
    }

    /**
     * Parse the JSON object and build the corresponding objects.
     */
    public void doParse() {
        // parse header types
        json.get("header_types").asArray().forEach(val -> {

            JsonObject jHeaderType = val.asObject();

            // populate fields list
            List<FieldTypeModel> fieldTypes = Lists.newArrayList();

            jHeaderType.get("fields").asArray().forEach(x -> fieldTypes.add(
                    new FieldTypeModel(
                            x.asArray().get(0).asString(),
                            x.asArray().get(1).asInt())));

            // add header type instance
            String name = jHeaderType.get("name").asString();
            int id = jHeaderType.get("id").asInt();

            HeaderTypeModel headerType = new HeaderTypeModel(name, id, fieldTypes);

            headerTypes.put(name, id, headerType);
        });

        // parse headers
        json.get("headers").asArray().forEach(val -> {

            JsonObject jHeader = val.asObject();

            String name = jHeader.get("name").asString();
            int id = jHeader.get("id").asInt();
            String typeName = jHeader.get("header_type").asString();

            HeaderModel header = new HeaderModel(name,
                    id,
                    headerTypes.get(typeName),
                    jHeader.get("metadata").asBoolean());

            // add instance
            headers.put(name, id, header);
        });

        // Parsers
        json.get("parsers").asArray().forEach(val -> {
            JsonObject jParser = val.asObject();
            jParser.get("parse_states").asArray().forEach(stateVal -> {
                JsonObject jState = stateVal.asObject();
                String name = jState.get("name").asString();
                int id = jState.getInt("id", 0);
                ParserStateModel stateModel = new ParserStateModel(name, id);

                /* Parser operations */
                jState.get("parser_ops").asArray().forEach(opsVal -> {
                    JsonObject jOps = opsVal.asObject();
                    String op = jOps.get("op").asString();
                    String headerName = jOps.get("parameters")
                            .asArray()
                            .get(1)
                            .asObject()
                            .get("value")
                            .asString();
                    if (op.equals("extract")) {
                        HeaderModel headerModel = headers.get(headerName);
                        stateModel.setHeader(headerModel);
                    }
                });

                /* Transitions */
                jState.get("transitions").asArray().forEach(tranVal -> {
                    JsonObject jTransition = tranVal.asObject();
                    if (!jTransition.get("next_state").isNull()) {
                        String nextStage = jTransition.getString("next_state", null);
                        stateModel.addNextState(nextStage);
                    }
                });
                this.parsers.put(stateModel.getName(), stateModel.getId(), stateModel);
            });
        });

        this.parsers.values().forEach(parserStateModel -> {
            HeaderModel preHeader = parserStateModel.getHeader();
            parserStateModel.getNextStates().forEach(state -> {
                HeaderModel headerModel = this.parsers.get(state).getHeader();
                if (headerModel != null) {
                    headerModel.setPreHeader(preHeader);
                }
            });
        });


        // parse actions
        json.get("actions").asArray().forEach(val -> {

            JsonObject jAction = val.asObject();

            // populate runtime data list
            List<DataModel> runtimeDatas = Lists.newArrayList();

            jAction.get("runtime_data").asArray().forEach(jData -> runtimeDatas.add(
                    new DataModel(
                            jData.asObject().get("name").asString(),
                            jData.asObject().get("bitwidth").asInt()
                    )));

            // add action instance
            String name = jAction.get("name").asString();
            int id = jAction.get("id").asInt();

            List<PrimitiveModel> primitiveModels = Lists.newArrayList();

            jAction.get("primitives").asArray().forEach(primitive -> {
                JsonObject jPrimitive = primitive.asObject();
                List<ParameterModel> parameterModels = Lists.newArrayList();
                jPrimitive.get("parameters").asArray().forEach(parameter -> {
                    JsonObject jParameter = parameter.asObject();
                    String type = jParameter.get("type").asString();
                    JsonValue value = jParameter.get("value");
                    if (type.equals("fields")) {
                        String headerName = value.asArray().get(0).asString();
                        String fieldName = value.asArray().get(1).asString();

                        FieldModel fieldModel = headers.get(headerName).getField(fieldName);
                        parameterModels.add(new ParameterModel(fieldModel));
                    } else if (type.equals("hexstr")) {
                        parameterModels.add(new ParameterModel(value.asInt()));
                    } else if (type.equals("header")) {
                        HeaderModel headerModel = headers.get(value.asString());
                        parameterModels.add(new ParameterModel(headerModel));
                    }
                });
                String op = jPrimitive.get("op").asString();
                PrimitiveModel primitiveModel = new PrimitiveModel(PrimitiveType.getPrimitiveType(op), parameterModels);
                primitiveModels.add(primitiveModel);
            });

            ActionModel action = new ActionModel(name,
                    id,
                    runtimeDatas,
                    primitiveModels);

            actions.put(name, id, action);
        });

        // parse tables
        json.get("pipelines").asArray().forEach(pipeline -> {

            pipeline.asObject().get("tables").asArray().forEach(val -> {

                JsonObject jTable = val.asObject();

                // populate keys
                List<MatchKeyModel> keys = Lists.newArrayList();

                jTable.get("key").asArray().forEach(jKey -> {
                    JsonArray target = jKey.asObject().get("target").asArray();

                    HeaderModel header = getHeader(target.get(0).asString());
                    String typeName = target.get(1).asString();

                    FieldModel field = new FieldModel(header,
                            header.getType().getField(typeName), 0);

                    String matchTypeStr = jKey.asObject().get("match_type").asString();

                    MatchKeyModel.MatchKeyType matchKeyType =
                            MatchKeyModel.MatchKeyType.getType(matchTypeStr);

                    if (matchKeyType == MatchKeyModel.MatchKeyType.UNDEFINED) {
                        throw new RuntimeException(
                                "Unable to parse match type: " + matchTypeStr);
                    }
                    keys.add(new MatchKeyModel(matchKeyType, field));
                });

                // populate actions set
                Set<ActionModel> actionzz = Sets.newHashSet();
                jTable.get("actions").asArray().forEach(
                        jAction -> actionzz.add(action(jAction.asString())));

                // add table instance
                String name = jTable.get("name").asString();
                int id = jTable.get("id").asInt();

                TableModel table = new TableModel(name,
                        id,
                        jTable.get("match_type").asString(),
                        jTable.get("type").asString(),
                        jTable.get("max_size").asInt(),
                        jTable.get("with_counters").asBoolean(),
                        jTable.get("support_timeout").asBoolean(),
                        keys,
                        actionzz,
                        jTable.get("base_default_next").isNull() ? null : jTable.get("base_default_next").asString());

                tables.put(name, id, table);
            });
        });
    }

}
