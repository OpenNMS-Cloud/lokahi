/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013-2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
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

package org.opennms.horizon.alarmservice.api;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.opennms.horizon.alarmservice.rest.support.MultivaluedMapImpl;

@Path("/alarms")
public interface AlarmRestService {

    @GET
    @Path("list") // curl -X GET http://localhost:8181/cxf/alarmservice/alarms/list
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "admin"})
    Response getAlarms(@Context final SecurityContext securityContext, final UriInfo uriInfo);


    @PUT
    @Path("{id}/memo") // curl -X PUT http://localhost:8181/cxf/alarmservice/alarms/1/memo -d 'user=mark&body=not null'
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed({"user", "admin"})
    Response updateMemo(@Context final SecurityContext securityContext, @PathParam("id") final Long alarmId, final MultivaluedMapImpl params);


    @PUT
    @Path("{id}/journal") // curl -X PUT http://localhost:8181/cxf/alarmservice/alarms/1/journal -d 'user=mark&body=not null'
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed({"user", "admin"})
    Response updateJournal(@Context final SecurityContext securityContext, @PathParam("id") final Long alarmId, final MultivaluedMapImpl params);

    @DELETE
    @Path("{id}/memo") // curl -X DELETE http://localhost:8181/cxf/alarmservice/alarms/1/memo -d 'user=mark&body=not null'
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed({"admin"})
    Response removeMemo(@Context final SecurityContext securityContext, @PathParam("id") final Long alarmId);

}
