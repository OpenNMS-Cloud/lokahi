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

package org.opennms.horizon.notifications.tenant;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.converter.KafkaMessageHeaders;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class TenantAwareKafkaListenerAspect {
    private static final Logger LOG = LoggerFactory.getLogger(TenantAwareKafkaListenerAspect.class);

    @Around(("@annotation(listener)"))
    public Object getTenant(ProceedingJoinPoint joinPoint, TenantAwareKafkaListener listener) throws Throwable {
        boolean foundTenant = false;

        Object[] args = joinPoint.getArgs();
        for (Object arg:args) {
            if (arg instanceof KafkaMessageHeaders) {
                Object tenantId = ((KafkaMessageHeaders)arg).get(GrpcConstants.TENANT_ID_KEY);
                if (tenantId instanceof byte[]) {
                    String strTenantId = new String((byte[]) tenantId);
                    TenantContext.setTenantId(strTenantId);
                    foundTenant = true;
                    break;
                }
            }
        }

        if (foundTenant) {
            try {
                Object proceed = joinPoint.proceed();
                return proceed;
            } finally {
                TenantContext.clear();
            }
        } else {
            LOG.warn("Failed to find tenant id");
            if (listener.skipOnMissing()) {
                // TODO: log args.
                return null;
            } else {
                return joinPoint.proceed();
            }
        }
    }
}
