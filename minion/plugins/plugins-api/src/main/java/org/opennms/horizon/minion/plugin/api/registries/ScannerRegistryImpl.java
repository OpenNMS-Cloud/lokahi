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

package org.opennms.horizon.minion.plugin.api.registries;

import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.minion.plugin.api.ScannerManager;
import org.opennms.horizon.minion.plugin.api.RegistrationService;
import org.osgi.framework.BundleContext;

import java.util.Map;

@Slf4j
public class ScannerRegistryImpl extends AlertingPluginRegistry<String, ScannerManager> implements ScannerRegistry {

    public static final String PLUGIN_IDENTIFIER = "scanner.name";

    public ScannerRegistryImpl(BundleContext bundleContext, RegistrationService registrationService) {
        super(bundleContext, ScannerManager.class, PLUGIN_IDENTIFIER, registrationService);
    }

    @Override
    public Map<String, ScannerManager> getServices() {
        return super.asMap();
    }
}
