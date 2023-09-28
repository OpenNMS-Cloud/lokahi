package org.opennms.horizon.alertservice.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Entity
@Table(name="system_policy_tag")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@IdClass(SystemPolicyTag.RelationshipId.class)
public class SystemPolicyTag {
    @Id
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Id
    @Column(name = "policy_id", nullable = false)
    private long policyId;

    @Id
    @OneToOne
    @JoinColumn(name = "tag_id", referencedColumnName = "id", nullable = false)
    private Tag tag;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class RelationshipId implements Serializable {
        private String tenantId;
        private long policyId;
        private Tag tag;
    }
}
