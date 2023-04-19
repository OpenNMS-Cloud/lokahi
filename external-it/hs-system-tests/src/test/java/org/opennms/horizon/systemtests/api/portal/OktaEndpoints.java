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
import org.opennms.horizon.systemtests.api.portal.models.AuthnRequest;
import org.opennms.horizon.systemtests.api.portal.models.AuthnResponse;
import org.opennms.horizon.systemtests.api.portal.models.TokenResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface OktaEndpoints {
    @POST("api/v1/authn")
    Call<AuthnResponse> authn(@Body AuthnRequest body);

    @GET("oauth2/{companyId}/v1/authorize")
    Call<ResponseBody> authorize(
        @Path("companyId") String companyId,
        @Query("client_id") String clientId,
        @Query("code_challenge") String codeChallenge,
        @Query("code_challenge_method") String codeChallengeMethod,
        @Query("nonce") String nonce,
        @Query("prompt") String prompt,
        @Query("scope") String scope,
        @Query("redirect_uri") String redirectUri,
        @Query("sessionToken") String sessionToken,
        @Query("state") String state,
        @Query("response_mode") String responseMode,
        @Query("response_type") String responseType
    );

    @FormUrlEncoded
    @POST("oauth2/{companyId}/v1/token")
    Call<TokenResponse> token(
        @Path("companyId") String companyId,
        @Field("client_id") String clientId,
        @Field("redirect_uri") String redirectUri,
        @Field("grant_type") String grantType,
        @Field("code_verifier") String codeVerifier,
        @Field("code") String code
    );
}
