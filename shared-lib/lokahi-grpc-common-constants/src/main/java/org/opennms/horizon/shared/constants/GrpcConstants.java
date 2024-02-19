/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.shared.constants;

import io.grpc.Context;
import io.grpc.Metadata;

// Those constants used in more than one services will be here.
public interface GrpcConstants {
    String TENANT_ID_KEY = "tenant-id";
    String LOCATION_ID_KEY = "location-id";

    // gRPC constants
    Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> TENANT_ID_REQUEST_KEY = Metadata.Key.of(TENANT_ID_KEY, Metadata.ASCII_STRING_MARSHALLER);
    Context.Key<String> TENANT_ID_CONTEXT_KEY = Context.key(TENANT_ID_KEY);
    Metadata.Key<String> LOCATION_ID_REQUEST_KEY = Metadata.Key.of(LOCATION_ID_KEY, Metadata.ASCII_STRING_MARSHALLER);
    Context.Key<String> LOCATION_ID_CONTEXT_KEY = Context.key(LOCATION_ID_KEY);

    // TODO: Remove this once we have inter-service authentication in place
    Metadata.Key<String> AUTHORIZATION_BYPASS_KEY =
            Metadata.Key.of("Bypass-Authorization", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> TENANT_ID_BYPASS_KEY = Metadata.Key.of("Bypass-Tenant-ID", Metadata.ASCII_STRING_MARSHALLER);
}
