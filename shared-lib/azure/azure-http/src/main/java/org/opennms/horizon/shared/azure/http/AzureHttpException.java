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
package org.opennms.horizon.shared.azure.http;

import lombok.Getter;
import org.opennms.horizon.shared.azure.http.dto.error.AzureHttpError;

@Getter
public class AzureHttpException extends Exception {
    private final transient AzureHttpError httpError;
    private final transient int httpStatusCode;

    public AzureHttpException(AzureHttpError httpError, int httpStatusCode) {
        super(httpError.getErrorDescription() != null ? httpError.getErrorDescription() : httpError.getMessage());
        this.httpError = httpError;
        this.httpStatusCode = httpStatusCode;
    }

    public AzureHttpException(String message) {
        super(message);
        this.httpError = null;
        this.httpStatusCode = 0;
    }

    public AzureHttpException(String message, Throwable t) {
        super(message, t);
        this.httpError = null;
        this.httpStatusCode = 0;
    }

    public boolean hasHttpError() {
        return this.httpError != null;
    }
}
