/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
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
 *******************************************************************************/

package org.opennms.horizon.server.service;

import java.util.List;
import java.util.Map;

import org.opennms.horizon.server.model.TimeSeriesQueryResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GraphQLApi
@Service
public class PrometheusTSDBServiceImpl implements TSDBService {
    @Value("${tsdb.url}")
    private String tsdbURL;
    private static final String QUERY_TEMPLATE = "query=%s{%s}";
    private static final String QUERY_TEMPLATE_WITHOUT_LABELS = "query=%s";
    private static final String LABEL_INSTANCE = "instance";
    private final RestTemplate restTemplate = new RestTemplate();

    @GraphQLQuery
    @Override
    public TimeSeriesQueryResult getMetric(String name, Map<String, String> labels) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        String urlEncodedQuery = generatePayloadString(name, labels);
        ResponseEntity<TimeSeriesQueryResult> response = restTemplate.exchange(tsdbURL, HttpMethod.POST,
            new HttpEntity<>(urlEncodedQuery, headers), TimeSeriesQueryResult.class);
        return response.getBody();
    }

    private String generatePayloadString(String name, Map<String, String> labels) {
        StringBuilder filterStr = new StringBuilder();
        if (labels != null && labels.size() > 0) {
            String filterTmp = "%s=\"%s\"";
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                if (filterStr.length() > 0) {
                    filterStr.append(",");
                }
                filterStr.append(String.format(filterTmp, entry.getKey(), entry.getValue()));
            }
        } else {
            return String.format(QUERY_TEMPLATE_WITHOUT_LABELS, name);
        }
        return String.format(QUERY_TEMPLATE, name, filterStr);
    }
}
