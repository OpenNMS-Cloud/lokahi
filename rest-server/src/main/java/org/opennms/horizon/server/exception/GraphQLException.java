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
package org.opennms.horizon.server.exception;

import java.io.Serial;

/**
 * An exception that represents an error that occurred whose message can be
 * displayed to the user, ie bad request, id not found, etc.
 */
public class GraphQLException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -4543741752174882855L;

    /**
     * @param message A message about the failure that will be displayed in the response.
     */
    public GraphQLException(String message) {
        super(message);
    }

    /**
     * @param message A message about the failure that will be displayed in the response.
     * @param cause   The cause of the exception. Will not be exposed in the response.
     */
    public GraphQLException(String message, Throwable cause) {
        super(message, cause);
    }
}
