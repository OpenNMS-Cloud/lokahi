package org.opennms.horizon.shared.azure.http.dto.login;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureOAuthToken {
    @SerializedName("token_type")
    @Expose
    private String tokenType;
    @SerializedName("expires_in")
    @Expose
    private String expiresIn;
    @SerializedName("ext_expires_in")
    @Expose
    private String extExpiresIn;
    @SerializedName("expires_on")
    @Expose
    private String expiresOn;
    @SerializedName("not_before")
    @Expose
    private String notBefore;
    @SerializedName("resource")
    @Expose
    private String resource;
    @SerializedName("access_token")
    @Expose
    private String accessToken;
}
