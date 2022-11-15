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

package org.opennms.horizon.events.persistence.model;


import com.vladmihalcea.hibernate.type.basic.Inet;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLInetType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;



@TypeDef(
    name = "ipv4",
    typeClass = PostgreSQLInetType.class,
    defaultForType = Inet.class
)
@Entity
@Table(name = "event")
public class Event {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "tenant_id")
    private String tenantId;

    @NotNull
    @Column(name = "event_uei")
    private String eventUei;

    @NotNull
    @Column(name = "produced_time", columnDefinition = "TIMESTAMP")
    private LocalDateTime producedTime;

    @Column(name = "monitoring_location_id")
    private Long monitoringLocationId;

    @Column(name = "node_id")
    private Long nodeId;

    @Column(name = "ip_address", columnDefinition = "inet")
    @Type(type = "com.vladmihalcea.hibernate.type.basic.PostgreSQLInetType")
    private Inet ipAddress;

    @Column(name = "event_parameters", columnDefinition = "jsonb")
    @Type(type = "com.vladmihalcea.hibernate.type.json.JsonBinaryType")
    private EventParameters eventParameters;

    @Column(name = "event_info")
    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] eventInfo;


    public EventParameters getEventParameters() {
        return eventParameters;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEventUei() {
        return eventUei;
    }

    public void setEventUei(String eventUei) {
        this.eventUei = eventUei;
    }

    public LocalDateTime getProducedTime() {
        return producedTime;
    }

    public void setProducedTime(LocalDateTime producedTime) {
        this.producedTime = producedTime;
    }

    public Long getMonitoringLocationId() {
        return monitoringLocationId;
    }

    public void setMonitoringLocationId(Long monitoringLocationId) {
        this.monitoringLocationId = monitoringLocationId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public Inet getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(Inet ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setEventParameters(EventParameters eventParameters) {
        this.eventParameters = eventParameters;
    }

    public byte[] getEventInfo() {
        return eventInfo;
    }

    public void setEventInfo(byte[] eventInfo) {
        this.eventInfo = eventInfo;
    }
}
