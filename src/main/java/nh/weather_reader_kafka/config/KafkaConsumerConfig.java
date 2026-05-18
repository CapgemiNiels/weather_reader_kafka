package nh.weather_reader_kafka.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Kafka consumer configuration.
 *
 * <p>{@code @EnableKafka} is intentionally absent — Spring Boot's
 * {@code KafkaAutoConfiguration} activates it automatically when
 * {@code spring-kafka} is on the classpath.
 *
 * <p>Auto-commit is disabled ({@code enable.auto.commit=false}) so that offsets
 * are committed by the container only after a record is successfully processed
 * ({@link ContainerProperties.AckMode#RECORD}). Failed records are handed to
 * the {@link DefaultErrorHandler} for retry and dead-letter routing.
 */
@Configuration
public class KafkaConsumerConfig {

    /** Creates a Kafka consumer configuration bean container. */
    public KafkaConsumerConfig() {
    }

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    /**
     * Builds the base Kafka consumer factory properties.
     *
     * @return consumer factory configured for Avro payload deserialization
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,        bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,                 groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url",  schemaRegistryUrl);
        props.put("specific.avro.reader", false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,        autoOffsetReset);

        // Disable Kafka client auto-commit; offsets are committed by the
        // container after each record is processed (AckMode.RECORD below).
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates the listener container factory used by {@code @KafkaListener}.
     *
     * @param errorHandler shared error handler for retries and DLT publishing
     * @return listener container factory configured for record-level acks
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler);

        // Commit offset only after the listener method returns successfully.
        // On failure the error handler takes over (retry → DLT).
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }
}
