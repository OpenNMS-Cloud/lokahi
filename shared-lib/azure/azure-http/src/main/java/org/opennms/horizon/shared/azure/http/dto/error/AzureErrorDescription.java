package org.opennms.horizon.shared.azure.http.dto.error;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureErrorDescription {

    @SerializedName("code")
    private String code;
    @SerializedName("message")
    private String message;

    @Override
    public String toString() {
        return String.format("%s: %s", code, message);
    }
}
