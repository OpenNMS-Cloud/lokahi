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
package org.opennms.horizon.dockerit;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.opennms.horizon.dockerit.testcontainers.TestContainerRunnerClassRule;

/**
 * IT runner that kicks off Cucumber.
 *
 * NOTE: glue lists the packages that Cucumber scans for Step Definitions (i.e. the Java classes that define the
 *  code executed for steps in the features file).  The minion-gateway wiremock package is needed to make use of the
 *  steps defined in the package from the minion gateway mock.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        glue = {"org.opennms.horizon.dockerit", "org.opennms.horizon.testtool.miniongateway.wiremock.client"},
        plugin = {"json:target/cucumber-report.json", "html:target/cucumber.html", "pretty"})
public class CucumberRunnerIT {
    // ClassRule must be public
    @ClassRule
    public static TestContainerRunnerClassRule testContainerRunnerClassRule = new TestContainerRunnerClassRule();
}
