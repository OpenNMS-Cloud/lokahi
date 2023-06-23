package org.opennms.horizon.it.gqlmodels.querywrappers;

import java.util.Map;

public class AddDiscoveryResponseData {

    private long id;
    private String name;
    private String[] ipAddresses;
    private String[] readCommunities;
    private String locationId;
    private int[] ports;
    private Map<String, Object> snmpConfig;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(String[] ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public String[] getReadCommunities() {
        return readCommunities;
    }

    public void setReadCommunities(String[] readCommunities) {
        this.readCommunities = readCommunities;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public int[] getPorts() {
        return ports;
    }

    public void setPorts(int[] ports) {
        this.ports = ports;
    }

    public Map<String, Object> getSnmpConfig() {
        return snmpConfig;
    }

    public void setSnmpConfig(Map<String, Object> snmpConfig) {
        this.snmpConfig = snmpConfig;
    }

    // {"data":
    // {"createIcmpActiveDiscovery":
    // {"id":2,"name":"Automation Discovery Tests","ipAddresses":["127.1.0.5"],"location":"Default",
    // "snmpConfig":{"readCommunities":["public"],"ports":[161]}}}}


}
