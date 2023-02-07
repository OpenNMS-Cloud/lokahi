/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.horizon.inventory.service.trapconfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "snmpTrapAddress",
    "snmpTrapPort",
    "newSuspectOnTrap",
    "snmpV3Users",
    "includeRawMessage",
    "batchSize",
    "queueSize",
    "numThreads",
    "batchIntervalMs"
})
@Generated("jsonschema2pojo")
public class TrapConfigBean {

    @JsonProperty("snmpTrapAddress")
    private String snmpTrapAddress;
    @JsonProperty("snmpTrapPort")
    private Integer snmpTrapPort;
    @JsonProperty("newSuspectOnTrap")
    private Boolean newSuspectOnTrap;
    @JsonProperty("snmpV3Users")
    private List<SnmpV3User> snmpV3Users = new ArrayList<>();
    @JsonProperty("includeRawMessage")
    private Boolean includeRawMessage;
    @JsonProperty("batchSize")
    private Integer batchSize;
    @JsonProperty("queueSize")
    private Integer queueSize;
    @JsonProperty("numThreads")
    private Integer numThreads;
    @JsonProperty("batchIntervalMs")
    private Integer batchIntervalMs;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("snmpTrapAddress")
    public String getSnmpTrapAddress() {
        return snmpTrapAddress;
    }

    @JsonProperty("snmpTrapAddress")
    public void setSnmpTrapAddress(String snmpTrapAddress) {
        this.snmpTrapAddress = snmpTrapAddress;
    }

    @JsonProperty("snmpTrapPort")
    public Integer getSnmpTrapPort() {
        return snmpTrapPort;
    }

    @JsonProperty("snmpTrapPort")
    public void setSnmpTrapPort(Integer snmpTrapPort) {
        this.snmpTrapPort = snmpTrapPort;
    }

    @JsonProperty("newSuspectOnTrap")
    public Boolean getNewSuspectOnTrap() {
        return newSuspectOnTrap;
    }

    @JsonProperty("newSuspectOnTrap")
    public void setNewSuspectOnTrap(Boolean newSuspectOnTrap) {
        this.newSuspectOnTrap = newSuspectOnTrap;
    }

    @JsonProperty("snmpV3Users")
    public List<SnmpV3User> getSnmpV3Users() {
        return snmpV3Users;
    }

    @JsonProperty("snmpV3Users")
    public void setSnmpV3Users(List<SnmpV3User> snmpV3Users) {
        this.snmpV3Users = snmpV3Users;
    }

    @JsonProperty("includeRawMessage")
    public Boolean isIncludeRawMessage() {
        return includeRawMessage;
    }

    @JsonProperty("includeRawMessage")
    public void setIncludeRawMessage(Boolean includeRawMessage) {
        this.includeRawMessage = includeRawMessage;
    }

    @JsonProperty("batchSize")
    public Integer getBatchSize() {
        return batchSize;
    }

    @JsonProperty("batchSize")
    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    @JsonProperty("queueSize")
    public Integer getQueueSize() {
        return queueSize;
    }

    @JsonProperty("queueSize")
    public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
    }

    @JsonProperty("numThreads")
    public Integer getNumThreads() {
        return numThreads;
    }

    @JsonProperty("numThreads")
    public void setNumThreads(Integer numThreads) {
        this.numThreads = numThreads;
    }

    @JsonProperty("batchIntervalMs")
    public Integer getBatchIntervalMs() {
        return batchIntervalMs;
    }

    @JsonProperty("batchIntervalMs")
    public void setBatchIntervalMs(Integer batchIntervalMs) {
        this.batchIntervalMs = batchIntervalMs;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
