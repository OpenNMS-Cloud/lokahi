package org.opennms.horizon.it.gqlmodels.querywrappers;

import org.opennms.horizon.it.gqlmodels.tsdata.TimeSeriesQueryResult;

public class MetricQueryData {
    private TimeSeriesQueryResult metric;

    public TimeSeriesQueryResult getMetric() {
        return metric;
    }

    public void setMetric(TimeSeriesQueryResult metric) {
        this.metric = metric;
    }
}
