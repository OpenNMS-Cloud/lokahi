package org.opennms.horizon.it.gqlmodels.tsdata;

import java.util.List;
import java.util.Map;

public class TSResult {
    private Map<String, String> metric;
    private List<Double> value;
    private List<List<Double>> values;

    public Map<String, String> getMetric() {
        return metric;
    }

    public void setMetric(Map<String, String> metric) {
        this.metric = metric;
    }

    public List<Double> getValue() {
        return value;
    }

    public void setValue(List<Double> value) {
        this.value = value;
    }

    public List<List<Double>> getValues() {
        return values;
    }

    public void setValues(List<List<Double>> values) {
        this.values = values;
    }
}
