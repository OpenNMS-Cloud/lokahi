package org.opennms.horizon.inventory.model.discovery.active;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.opennms.horizon.inventory.model.Tag;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity(name = "active_discovery")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ActiveDiscovery {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private long id;

    @NotNull
    @Column(name = "tenant_id")
    private String tenantId;

    @NotNull
    @Column(name = "name")
    private String name;

    @NotNull
    @Column(name = "location")
    private String location;

    @NotNull
    @Column(name = "create_time", columnDefinition = "TIMESTAMP")
    private LocalDateTime createTime;

    @ManyToMany(mappedBy = "activeDiscoveries")
    private List<Tag> tags = new ArrayList<>();
}
