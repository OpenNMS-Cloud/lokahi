package org.opennms.horizon.shared.azure.http.dto.error;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureHttpError {

    @SerializedName("error")
    private AzureErrorDescription error;
}
