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
package org.opennms.horizon.flows;

import com.codahale.metrics.MetricRegistry;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.opennms.horizon.flows.classification.ClassificationEngine;
import org.opennms.horizon.flows.classification.ClassificationRuleProvider;
import org.opennms.horizon.flows.classification.FilterService;
import org.opennms.horizon.flows.classification.csv.CsvImporter;
import org.opennms.horizon.flows.classification.internal.DefaultClassificationEngine;
import org.opennms.horizon.flows.grpc.client.IngestorClient;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.flows.integration.FlowRepository;
import org.opennms.horizon.flows.integration.FlowRepositoryImpl;
import org.opennms.horizon.flows.processing.DocumentEnricherImpl;
import org.opennms.horizon.flows.processing.Pipeline;
import org.opennms.horizon.flows.processing.PipelineImpl;
import org.opennms.horizon.flows.processing.impl.FlowDocumentClassificationRequestMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

// TODO: remove grey box tests and this custom test-only application wiring
@TestConfiguration
@EnableKafka
public class FlowProcessorTestConfig {

    @Bean
    public Pipeline createPipeLine(
            MetricRegistry metricRegistry, DocumentEnricherImpl documentEnricher, FlowRepository flowRepository) {
        var pipeLine = new PipelineImpl(metricRegistry, documentEnricher);
        var properties = new HashMap<>();
        properties.put(PipelineImpl.REPOSITORY_ID, "DataPlatform");
        pipeLine.onBind(flowRepository, properties);
        return pipeLine;
    }

    @Bean
    public DocumentEnricherImpl createDocumentEnricher(
            InventoryClient inventoryClient,
            ClassificationEngine classificationEngine,
            FlowDocumentClassificationRequestMapperImpl flowDocumentClassificationRequestMapper) {

        return new DocumentEnricherImpl(
                inventoryClient, classificationEngine, flowDocumentClassificationRequestMapper, 1);
    }

    @Bean
    public FlowDocumentClassificationRequestMapperImpl flowDocumentBuilderClassificationRequestMapper() {
        return new FlowDocumentClassificationRequestMapperImpl();
    }

    @Bean
    public ClassificationEngine createClassificationEngine() throws InterruptedException, IOException {
        final var rules = CsvImporter.parseCSV(FlowProcessor.class.getResourceAsStream("/pre-defined-rules.csv"), true);

        return new DefaultClassificationEngine(ClassificationRuleProvider.forList(rules), FilterService.NOOP);
    }

    @Bean
    public FlowRepository createFlowRepositoryImpl(final IngestorClient ingestorClient) {
        return new FlowRepositoryImpl(ingestorClient);
    }

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Bean
    public ProducerFactory<String, byte[]> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean("kafkaByteArrayProducerTemplate")
    public KafkaTemplate<String, byte[]> kafkaTemplate(@Autowired ProducerFactory<String, byte[]> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, byte[]> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-id");
        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.ByteArrayDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
