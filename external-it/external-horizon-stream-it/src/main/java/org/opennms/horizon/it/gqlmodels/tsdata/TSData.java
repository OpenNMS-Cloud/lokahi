package org.opennms.horizon.it.gqlmodels.tsdata;

import java.util.List;

public class TSData {
    private String resultType;
    private List<TSResult> result;

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public List<TSResult> getResult() {
        return result;
    }

    public void setResult(List<TSResult> result) {
        this.result = result;
    }
}
