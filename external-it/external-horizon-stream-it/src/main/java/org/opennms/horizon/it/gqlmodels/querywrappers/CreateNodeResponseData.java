package org.opennms.horizon.it.gqlmodels.querywrappers;

public class CreateNodeResponseData {
    private String nodeType;
    private CreateNodeResponseDetails details;

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public CreateNodeResponseDetails getDetails() {
        return details;
    }

    public void setDetails(CreateNodeResponseDetails details) {
        this.details = details;
    }
}
