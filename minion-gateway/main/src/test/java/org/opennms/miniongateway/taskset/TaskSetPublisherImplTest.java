package org.opennms.miniongateway.taskset;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.miniongateway.grpc.twin.TwinPublisher;
import org.opennms.taskset.contract.TaskSet;

import java.io.IOException;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

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

        testTaskSet =
            TaskSet.newBuilder()
                .build();

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
        } catch(Exception caughtException) {
            actualException = caughtException;
        }

        //
        // Verify the Results
        //
        assertSame(testException, actualException.getCause());
    }
}
