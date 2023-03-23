package org.opennms.horizon.server.model.flows;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RequestCriteria {
    private TimeRange timeRange;
    private int count = 10;
    private List<Exporter> exporter;
    private List<String> applications;
}
