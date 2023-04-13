/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.shared.constants;

import io.grpc.Context;
import io.grpc.Metadata;

//Those constants used in more than one services will be here.
public interface GrpcConstants {
    String TENANT_ID_KEY = "tenant-id";
    String LOCATION_KEY = "location";
    String DEFAULT_TENANT_ID = "opennms-prime";
    String DEFAULT_LOCATION = "Default";

    //gRPC constants
    Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> TENANT_ID_REQUEST_KEY = Metadata.Key.of(TENANT_ID_KEY, Metadata.ASCII_STRING_MARSHALLER);
    Context.Key<String> TENANT_ID_CONTEXT_KEY = Context.key(TENANT_ID_KEY);
    Metadata.Key<String> LOCATION_REQUEST_KEY = Metadata.Key.of(LOCATION_KEY, Metadata.ASCII_STRING_MARSHALLER);
    Context.Key<String> LOCATION_CONTEXT_KEY = Context.key(LOCATION_KEY);

    // TODO: Remove this once we have inter-service authentication in place
    Metadata.Key<String> AUTHORIZATION_BYPASS_KEY = Metadata.Key.of("Bypass-Authorization", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> TENANT_ID_BYPASS_KEY = Metadata.Key.of("Bypass-Tenant-ID", Metadata.ASCII_STRING_MARSHALLER);
}
