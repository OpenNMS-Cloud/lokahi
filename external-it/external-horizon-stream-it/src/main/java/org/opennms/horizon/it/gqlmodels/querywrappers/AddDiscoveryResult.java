package org.opennms.horizon.it.gqlmodels.querywrappers;

import org.opennms.horizon.it.gqlmodels.ErrorData;

import java.util.List;

public class AddDiscoveryResult {

    private AddDiscoveryResponseWrapper data;
    private List<ErrorData> errors;

    public AddDiscoveryResponseWrapper getData() {
        return data;
    }

    public void setData(AddDiscoveryResponseWrapper data) {
        this.data = data;
    }

    public List<ErrorData> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorData> errors) {
        this.errors = errors;
    }
}
