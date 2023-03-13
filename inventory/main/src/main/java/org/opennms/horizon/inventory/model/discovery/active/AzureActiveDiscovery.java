package org.opennms.horizon.inventory.model.discovery.active;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.opennms.horizon.inventory.mapper.EncryptAttributeConverter;

@Getter
@Setter
@Entity(name = "azure_active_discovery")
public class AzureActiveDiscovery extends ActiveDiscovery {

    @NotNull
    @Column(name = "client_id")
    private String clientId;

    @NotNull
    @Convert(converter = EncryptAttributeConverter.class)
    @Column(name = "client_secret")
    private String clientSecret;

    @NotNull
    @Column(name = "subscription_id")
    private String subscriptionId;

    @NotNull
    @Column(name = "directory_id")
    private String directoryId;
}
