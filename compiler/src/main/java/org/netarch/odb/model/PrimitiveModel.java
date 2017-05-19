package org.netarch.odb.model;

import org.netarch.odb.primitive.PrimitiveType;

import java.util.List;

public class PrimitiveModel {
    private final PrimitiveType type;
    private final List<ParameterModel> parameterModels;

    public PrimitiveModel(PrimitiveType type, List<ParameterModel> parameterModels) {
        this.type = type;
        this.parameterModels = parameterModels;
    }

    public PrimitiveType getType() {
        return type;
    }

    public ParameterModel getParameterModel(int index) {
        return parameterModels.get(index);
    }

    public List<ParameterModel> getParameterModels() {
        return parameterModels;
    }
}
