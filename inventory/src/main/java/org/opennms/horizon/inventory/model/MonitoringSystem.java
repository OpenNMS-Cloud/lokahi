package org.opennms.horizon.inventory.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Entity
@Getter
@Setter
public class MonitoringSystem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Column(name = "tenant_id")
    private String tenantId;

    @NotNull
    @Column(name = "system_id")
    private String systemId;

    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_location_id", referencedColumnName = "id")
    private MonitoringLocation monitoringLocation;

    @Column(name = "monitoring_location_id", insertable = false, updatable = false)
    private long monitoringLocationId;

    @NotNull
    @Column(name = "last_checked_in", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastCheckedIn;
}
