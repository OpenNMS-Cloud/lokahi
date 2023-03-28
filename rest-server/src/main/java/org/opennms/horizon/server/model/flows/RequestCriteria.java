package org.opennms.horizon.server.model.flows;

import io.leangen.graphql.annotations.GraphQLInputField;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RequestCriteria {
    private TimeRange timeRange;
    @GraphQLInputField(description = "For limit how much data to return.", defaultValue = "10")
    private int count = 10;
    @GraphQLInputField(description = "For series query.", defaultValue = "300000")
    private int step;
    @GraphQLInputField(description = "For find exporters use.")
    private List<Exporter> exporter;
    @GraphQLInputField(description = "For find application use.")
    private List<String> applications;
}
