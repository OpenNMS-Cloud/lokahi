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

package org.opennms.horizon.systemtests.steps.cloud;

import io.cucumber.java.en.Then;
import lombok.SneakyThrows;
import org.opennms.horizon.systemtests.CucumberHooks;
import testcontainers.UpdGenContainer;

import java.util.Arrays;

public class UdpGenSteps {
    @SneakyThrows
    @Then("send {int} packets of {string} traffic to {int} port")
    public void generateNetflow9(Integer number, String flowType, Integer port) {
        if (!Arrays.asList("netflow9", "netflow5", "ipfix").contains(flowType)) {
            throw new RuntimeException("Do not support this type");
        }

        try (
            UpdGenContainer updGenContainer = new UpdGenContainer(
                CucumberHooks.MINIONS.get(0).getUdpPortBinding(port),
                flowType,
                number)
        ) {
            updGenContainer.start();
        }
    }
}
