/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018-2023 The OpenNMS Group, Inc.
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
package org.opennms.horizon.flows.classification;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.opennms.horizon.flows.classification.csv.CsvImporter;
import org.opennms.horizon.flows.classification.internal.DefaultClassificationEngine;
import org.opennms.horizon.flows.classification.persistence.api.Rule;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use the Java Microbenchmarking Harness (JMH) to measure classification performance.
 * <p>
 * Rule sets are loaded from csv files and classification is done with randomly generated flows based on the
 * protocols, ports, and addresses found in the loaded rule sets.
 */
public class ClassificationEngineBenchmark {

    // the number of classification request that are processed in a single benchmark method call
    // -> the reported number of operations per second must be multiplied by this number to get
    //    the number of classifications per second
    private static final int BATCH_SIZE = 1000;

    // the benchmark is run for different rule sets
    private static final String EXAMPLE_RULES_RESOURCE = "/example-rules.csv";
    private static final String PRE_DEFINED_RULES_RESOURCE = "/pre-defined-rules.csv";

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    public static List<Rule> getRules(String resource) throws IOException {
        final var rules = CsvImporter.parseCSV(
            ClassificationEngineBenchmark.class.getResourceAsStream(resource),
            true);
        int cnt = 0;
        for (var r: rules) {
            r.setPosition(cnt++);
        }
        return rules;
    }

    @State(Scope.Benchmark)
    public static class BState {

        @Param({"0", "1"})
        public int index;

        @Param({EXAMPLE_RULES_RESOURCE, PRE_DEFINED_RULES_RESOURCE})
        public String ruleSet;

        private ClassificationEngine classificationEngine;
        private List<ClassificationRequest> classificationRequests;

        @Setup
        public void setup() throws InterruptedException, IOException {
            var rules = getRules(ruleSet);
            classificationEngine = new DefaultClassificationEngine(() -> rules, org.mockito.Mockito.mock(FilterService.class));
            classificationRequests = RandomClassificationEngineTest.streamOfclassificationRequests(rules, 123456l).skip(index * BATCH_SIZE).limit(BATCH_SIZE).collect(Collectors.toList());
        }

        public List<ClassificationRequest> requests() {
            return classificationRequests;
        }

        public ClassificationEngine classificationEngine() {
            return classificationEngine;
        }

    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @Measurement(iterations = 2)
    public void classify(BState state, Blackhole blackhole) {
        var classificationEngine = state.classificationEngine();
        for (var cr: state.requests()) {
            var app = classificationEngine.classify(cr);
            blackhole.consume(app);
        }
    }

}
