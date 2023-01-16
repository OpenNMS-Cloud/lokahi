package org.opennms.horizon.it.gqlmodels.querywrappers;

import org.opennms.horizon.it.gqlmodels.ErrorData;

import java.util.List;

public class CreateNodeResult {
    private CreateNodeResponseWrapper data;
    private List<ErrorData> errors;

    public CreateNodeResponseWrapper getData() {
        return data;
    }

    public void setData(CreateNodeResponseWrapper data) {
        this.data = data;
    }

    public List<ErrorData> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorData> errors) {
        this.errors = errors;
    }
}
