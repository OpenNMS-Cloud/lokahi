package org.opennms.horizon.shared.azure.http.dto.error;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class AzureHttpError {
    @SerializedName("timestamp")
    private Date timestamp;

    @SerializedName("error")
    private String error;

    @SerializedName("error_description")
    private String errorDescription;

    @SerializedName("error_uri")
    private String errorUri;

    @SerializedName("trace_id")
    private String traceId;

    @SerializedName("correlation_id")
    private String correlationId;

    @SerializedName("code")
    private String code;

    @SerializedName("message")
    private String message;
}
