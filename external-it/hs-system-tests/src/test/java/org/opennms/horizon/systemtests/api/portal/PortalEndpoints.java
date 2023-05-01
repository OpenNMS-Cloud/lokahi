/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.systemtests.api.portal;

import okhttp3.ResponseBody;
import org.opennms.horizon.systemtests.api.portal.models.BtoInstanceRequest;
import org.opennms.horizon.systemtests.api.portal.models.BtoInstancesResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface PortalEndpoints {

    @POST("api/v1/portal/{organization}/bto-instance")
    Call<ResponseBody> createBtoInstance(
        @Path("organization") String organization,
        @Body BtoInstanceRequest body,
        @Header("authorization") String authToken
    );

    @GET("api/v1/portal/{organization}/bto-instance")
    Call<BtoInstancesResponse> getBtoInstances(
        @Path("organization") String organization,
        @Query("search") String searchPattern,
        @Query("searchColumn") String searchColumn,
        @Query("limit") Integer limit,
        @Header("authorization") String authToken
    );

    @DELETE("api/v1/portal/{organization}/bto-instance/{instanceId}")
    Call<Void> deleteInstance(
        @Path("organization") String organization,
        @Path("instanceId") String instanceId,
        @Header("authorization") String authToken
    );
}
