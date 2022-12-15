package org.opennms.horizon.events;

import io.grpc.Context;
import io.grpc.Metadata;

public interface Constants {

    String TENANT_ID_KEY = "tenant-id";
    Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    Context.Key<String> TENANT_ID_CONTEXT_KEY = Context.key(TENANT_ID_KEY);

/*    INDETERMINATE(1, "Indeterminate", "lightblue"),
    CLEARED(2, "Cleared", "white"),
    NORMAL(3, "Normal", "green"),
    WARNING(4, "Warning", "cyan"),
    MINOR(5, "Minor", "yellow"),
    MAJOR(6, "Major", "orange"),
    CRITICAL(7, "Critical", "red");*/

}
