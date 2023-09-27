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
@Table(name="default_policy_tag")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@IdClass(DefaultPolicyTag.RelationshipId.class)
public class DefaultPolicyTag {
    @Id
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Id
    @OneToOne
    @JoinColumn(name = "tag_id", referencedColumnName = "id")
    private Tag tag;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class RelationshipId implements Serializable {
        private String tenantId;
        private Tag tag;
    }
}
