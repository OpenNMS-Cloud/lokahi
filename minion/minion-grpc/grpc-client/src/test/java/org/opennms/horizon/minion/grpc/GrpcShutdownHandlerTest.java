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

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.karaf.system.SystemService;
import org.junit.jupiter.api.Test;

class GrpcShutdownHandlerTest {

    private final SystemService mockSystemService = mock(SystemService.class);
    private final GrpcShutdownHandler target = new GrpcShutdownHandler(mockSystemService);

    @Test
    void testShutdownWithMessage() throws Exception {
        target.shutdown("message");
        verify(mockSystemService, times(1)).halt("+0");
    }

    @Test
    void testShutdownWithThrowable() throws Exception {
        RuntimeException ex = new RuntimeException("exception");
        target.shutdown(ex);
        verify(mockSystemService, times(1)).halt("+0");
    }

    @Test
    void testShutdownException() throws Exception {
        doThrow(new RuntimeException()).when(mockSystemService).halt("+0");
        int statusCode = catchSystemExit(() -> target.shutdown("message"));
        verify(mockSystemService, times(1)).halt("+0");
        assertEquals(-1, statusCode);
    }
}
