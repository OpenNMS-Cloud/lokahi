/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.inventory.service.trapconfig;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

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
