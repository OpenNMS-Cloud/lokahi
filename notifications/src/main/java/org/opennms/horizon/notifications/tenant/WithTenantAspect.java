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
import org.opennms.horizon.notifications.grpc.config.TenantLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class WithTenantAspect {
    private static final Logger LOG = LoggerFactory.getLogger(WithTenantAspect.class);

    @Autowired
    private TenantLookup tenantLookup;

    @Around(("@annotation(withTenant)"))
    public Object getTenant(ProceedingJoinPoint joinPoint, WithTenant withTenant) throws Throwable {
        String tenantId = withTenant.tenantId();
        int tenantIdArg = withTenant.tenantIdArg();

        if (tenantIdArg >= 0) {
            Object[] args = joinPoint.getArgs();
            if (args.length <= tenantIdArg) {
                throw new RuntimeException("TenantIdArg position is greater than the number of arguments to the method");
            }
            tenantId = String.valueOf(args[tenantIdArg]);
        }

        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = tenantLookup.lookupTenantId().orElseThrow();
        }
        TenantContext.setTenantId(tenantId);

        Object proceed = joinPoint.proceed();
        TenantContext.clear();
        return proceed;
    }
}
