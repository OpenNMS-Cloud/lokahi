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


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import javax.validation.constraints.NotNull;
import java.net.InetAddress;
import java.time.LocalDateTime;



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
    private InetAddress ipAddress;

    @Column(name = "event_parameters", columnDefinition = "jsonb")
    @JdbcTypeCode( SqlTypes.JSON )
    private EventParameters eventParameters;

    @Column(name = "event_info", columnDefinition = "bytea")
    @Lob
    @JdbcTypeCode(SqlTypes.VARBINARY)
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

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
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
