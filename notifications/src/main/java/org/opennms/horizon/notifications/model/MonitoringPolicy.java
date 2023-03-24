package org.opennms.horizon.notifications.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
public class MonitoringPolicy extends TenantAwareEntity {
    @Id
    private long id;

    @Column(name = "notify_email")
    private boolean notifyByEmail;

    @Column(name = "notify_pagerduty")
    private boolean notifyByPagerDuty;

    @Column(name = "notify_webhooks")
    private boolean notifyByWebhooks;
}
