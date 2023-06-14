package org.opennms.horizon.it.gqlmodels.querywrappers;

import org.opennms.horizon.it.gqlmodels.MinionData;

import java.util.List;

public class FindAllMinionsData {
    private List<MinionData> findAllMinions;

    public List<MinionData> getFindAllMinions() {
        return findAllMinions;
    }

    public void setFindAllMinions(List<MinionData> findAllMinions) {
        this.findAllMinions = findAllMinions;
    }
}
