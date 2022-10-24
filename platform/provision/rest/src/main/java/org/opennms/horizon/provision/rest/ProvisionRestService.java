/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012-2022 The OpenNMS Group, Inc.
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
 ******************************************************************************/

package org.opennms.horizon.provision.rest;

import java.text.ParseException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/provision")
public interface ProvisionRestService {

    @POST
    @Path("publish")
    @Consumes({MediaType.APPLICATION_JSON})
//    @RolesAllowed({"admin"})
    Response publishRequisition(final String requisition);

    @GET
    @Path("read/{id}")
    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed({"admin"})
    Response getRequisition(@PathParam("id") final String requisitionName);

    @PUT
    @Path("update")
    @Consumes({MediaType.APPLICATION_JSON})
//    @RolesAllowed({"admin"})
    Response updateRequisition(final String requisition);

    @DELETE
    @Path("delete/{id}")
    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed({"admin"})
    Response deleteRequisition(@PathParam("id") final String requisitionName);

    @POST
    @Path("scanNodes")
    Response scanNodes();

    @POST
    @Path("publish/foreignsource")
    @Consumes({MediaType.APPLICATION_JSON})
//    @RolesAllowed({"admin"})
    Response publishForeignSource(final String foreignSource);

    @POST
    @Path("run/igniteTest")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed({"admin"})
    void runIgniteTest(@Suspended final AsyncResponse callback);
}
