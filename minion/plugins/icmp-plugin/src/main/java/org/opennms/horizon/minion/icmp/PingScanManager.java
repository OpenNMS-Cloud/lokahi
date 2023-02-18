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

package org.opennms.horizon.minion.icmp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.minion.plugin.api.ScannerManager;
import org.opennms.horizon.shared.icmp.PingerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class PingScanManager implements ScannerManager {


    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat("ping-sweep-%d")
        .build();

    private final ExecutorService executor = Executors.newCachedThreadPool(threadFactory);
    private final PingerFactory pingerFactory;

    public PingScanManager(PingerFactory pingerFactory) {
        this.pingerFactory = pingerFactory;
    }

    @Override
    public Scanner create() {
        return new PingScan(pingerFactory, executor);
    }
}
