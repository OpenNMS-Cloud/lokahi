package org.opennms.horizon.it.gqlmodels.querywrappers;

public class AddDiscoveryResponseWrapper {

    private AddDiscoveryResponseData createIcmpActiveDiscovery;

    public AddDiscoveryResponseData getCreateIcmpActiveDiscovery() {
        return createIcmpActiveDiscovery;
    }

    public void setCreateIcmpActiveDiscovery(AddDiscoveryResponseData createIcmpActiveDiscovery) {
        this.createIcmpActiveDiscovery = createIcmpActiveDiscovery;
    }
}
