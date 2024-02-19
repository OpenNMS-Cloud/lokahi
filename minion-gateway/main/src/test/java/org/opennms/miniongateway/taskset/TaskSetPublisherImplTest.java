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
package org.opennms.miniongateway.taskset;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.miniongateway.grpc.twin.TwinPublisher;
import org.opennms.taskset.contract.TaskSet;

public class TaskSetPublisherImplTest {

    private TaskSetPublisherImpl target;

    private GrpcTwinPublisher mockGrpcTwinPublisher;
    private TwinPublisher.Session<TaskSet> mockSession;

    private TaskSet testTaskSet;

    @Before
    public void setUp() throws Exception {
        mockGrpcTwinPublisher = Mockito.mock(GrpcTwinPublisher.class);
        mockSession = Mockito.mock(TwinPublisher.Session.class);

        Mockito.when(mockGrpcTwinPublisher.register("task-set", TaskSet.class, "x-tenant-id-x", "x-location-x"))
                .thenReturn(mockSession);

        testTaskSet = TaskSet.newBuilder().build();

        target = new TaskSetPublisherImpl(mockGrpcTwinPublisher);
    }

    @Test
    public void testPublishTaskSet() throws IOException {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        target.publishTaskSet("x-tenant-id-x", "x-location-x", testTaskSet);

        //
        // Verify the Results
        //
        Mockito.verify(mockSession).publish(testTaskSet);
    }

    @Test
    public void testExceptionOnSessionPublishTaskSet() throws IOException {
        //
        // Setup Test Data and Interactions
        //
        IOException testException = new IOException("x-test-io-exception-x");
        Mockito.doThrow(testException).when(mockSession).publish(testTaskSet);

        //
        // Execute
        //
        Exception actualException = null;
        try {
            target.publishTaskSet("x-tenant-id-x", "x-location-x", testTaskSet);
            fail("missing expected exception");
        } catch (Exception caughtException) {
            actualException = caughtException;
        }

        //
        // Verify the Results
        //
        assertSame(testException, actualException.getCause());
    }
}
