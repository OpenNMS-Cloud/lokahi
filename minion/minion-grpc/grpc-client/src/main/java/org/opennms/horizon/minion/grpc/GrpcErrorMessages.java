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
package org.opennms.horizon.minion.grpc;

public class GrpcErrorMessages {
    private GrpcErrorMessages() {
        throw new IllegalStateException("Constant class");
    }

    public static final String UNAUTHENTICATED =
            "Certificate is not accepted by the server. Please download the client certificate again. Going to shut down now.";
    public static final String INVALID_CLIENT_STORE =
            "Client keystore file is invalid. Please make sure it exists and is a file. Going to shut down now.";
    public static final String FAIL_LOADING_CLIENT_KEYSTORE =
            "Client keystore file failed to load. Please check keystore and password. Going to shut down now.";
    public static final String INVALID_TRUST_STORE =
            "Trust keystore file is invalid. Please make sure it exists and is a file. Going to shut down now.";
    public static final String FAIL_LOADING_TRUST_KEYSTORE =
            "Trust keystore file failed to load. Please check keystore and password. Going to shut down now.";
}
