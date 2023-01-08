package org.opennms.horizon.shared.azure.http.dto.login;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureOAuthToken {
    @SerializedName("token_type")
    private String tokenType;
    @SerializedName("expires_in")
    private String expiresIn;
    @SerializedName("ext_expires_in")
    private String extExpiresIn;
    @SerializedName("expires_on")
    private String expiresOn;
    @SerializedName("not_before")
    private String notBefore;
    @SerializedName("resource")
    private String resource;
    @SerializedName("access_token")
    private String accessToken;
}
