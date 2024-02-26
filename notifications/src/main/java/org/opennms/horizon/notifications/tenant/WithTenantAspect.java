/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.notifications.tenant;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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
        String tenantIdArgInternalMethod = withTenant.tenantIdArgInternalMethod();
        String tenantIdArgInternalClass = withTenant.tenantIdArgInternalClass();

        if (tenantIdArg >= 0) {
            Object[] args = joinPoint.getArgs();
            if (args.length <= tenantIdArg) {
                throw new RuntimeException(
                        "TenantIdArg position is greater than the number of arguments to the method");
            }
            if (tenantIdArgInternalMethod == null
                    || tenantIdArgInternalMethod.isEmpty()
                    || tenantIdArgInternalClass == null
                    || tenantIdArgInternalClass.isEmpty()) {
                tenantId = String.valueOf(args[tenantIdArg]);
            } else {
                Object tenantObj = args[tenantIdArg];
                Class clazz = Class.forName(tenantIdArgInternalClass);
                Method method = clazz.getMethod(tenantIdArgInternalMethod);
                Object tenant = method.invoke(tenantObj);
                tenantId = String.valueOf(tenant);
            }
        }

        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = tenantLookup.lookupTenantId().orElseThrow();
        }

        try {
            TenantContext.setTenantId(tenantId);
            Object proceed = joinPoint.proceed();
            return proceed;
        } finally {
            TenantContext.clear();
        }
    }
}
