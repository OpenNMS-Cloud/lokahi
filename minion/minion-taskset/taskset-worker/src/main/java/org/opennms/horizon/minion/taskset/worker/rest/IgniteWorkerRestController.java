package org.opennms.horizon.minion.taskset.worker.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/ignite-worker")
public interface IgniteWorkerRestController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/service-deployment/metrics")
    Response reportServiceDeploymentMetrics(@QueryParam("verbose") boolean verbose);
}
