package org.opennms.horizon.shared.azure.http.dto.subscription;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureSubscription {
    @SerializedName("id")
    private String id;
    @SerializedName("authorizationSource")
    private String authorizationSource;
    @SerializedName("subscriptionId")
    private String subscriptionId;
    @SerializedName("tenantId")
    private String tenantId;
    @SerializedName("displayName")
    private String displayName;
    @SerializedName("state")
    private String state;
}
