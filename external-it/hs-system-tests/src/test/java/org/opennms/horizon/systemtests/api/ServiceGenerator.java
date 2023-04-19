/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.systemtests.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;


public class ServiceGenerator {
    private static final int DEFAULT_TIMEOUT = 30;
    private static final OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    private static final HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE);

    public static <S> S createService(String endpoint, Class<S> service) {
        Retrofit.Builder builder = new Retrofit.Builder()
            .baseUrl(endpoint + "/")
            .addConverterFactory(JacksonConverterFactory.create());

        httpClient.retryOnConnectionFailure(true);
        httpClient.writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        httpClient.readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        httpClient.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        httpClient.addInterceptor(logging);
        httpClient.addInterceptor(chain -> {
            Request request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();
            return chain.proceed(request);
        });
        httpClient.followRedirects(false);

        Retrofit retrofit = builder.client(httpClient.build()).build();
        return retrofit.create(service);
    }
}
