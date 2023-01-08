package org.opennms.horizon.shared.azure.http.dto.subscription;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureSubscription {
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("authorizationSource")
    @Expose
    private String authorizationSource;
    @SerializedName("subscriptionId")
    @Expose
    private String subscriptionId;
    @SerializedName("tenantId")
    @Expose
    private String tenantId;
    @SerializedName("displayName")
    @Expose
    private String displayName;
    @SerializedName("state")
    @Expose
    private String state;
}
