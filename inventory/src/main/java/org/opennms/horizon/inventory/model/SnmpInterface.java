package org.opennms.horizon.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.net.InetAddress;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
public class SnmpInterface {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Column(name = "tenant_id")
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", referencedColumnName = "id")
    private Node node;

    @Column(name = "node_id", insertable = false, updatable = false)
    private long nodeId;

    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

    @NotNull
    @Column(name = "if_index")
    private int ifIndex;

    @Column(name = "if_descr")
    private String ifDescr;
}
