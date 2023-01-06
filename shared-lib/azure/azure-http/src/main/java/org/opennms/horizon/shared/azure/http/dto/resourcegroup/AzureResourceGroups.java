package org.opennms.horizon.shared.azure.http.dto.resourcegroup;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AzureResourceGroups {
    @SerializedName("value")
    @Expose
    private List<AzureValue> value = new ArrayList<>();
}
