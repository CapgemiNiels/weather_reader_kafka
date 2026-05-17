package nh.weather_reader_kafka.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configures production-grade Kafka consumer error handling.
 *
 * <h3>Strategy</h3>
 * <ol>
 *   <li>On listener failure, Spring Kafka retries the record up to
 *       {@value #MAX_RETRY_ATTEMPTS} times with a {@value #RETRY_INTERVAL_MS} ms
 *       pause between each attempt.</li>
 *   <li>After exhausting retries the raw {@code ConsumerRecord} bytes are
 *       forwarded to a dead-letter topic ({@code <source-topic>.DLT}) via a
 *       dedicated byte-array producer — preserving the original payload for
 *       later inspection or replay.</li>
 *   <li>{@link SerializationException}s (e.g. schema mismatch) are classified
 *       as non-retryable and are routed directly to the DLT without wasting
 *       retry budget.</li>
 * </ol>
 *
 * <p>The DLT producer uses {@link ByteArraySerializer} for both key and value so
 * that the original wire-format bytes are written verbatim, regardless of which
 * serializer the consuming side uses.
 *
 * <p><strong>Important:</strong> ensure the DLT topics ({@code *.DLT}) exist in
 * your Kafka cluster, or that the broker is configured with
 * {@code auto.create.topics.enable=true}.
 */
@Configuration
public class KafkaErrorHandlerConfig {

    /** Number of retry attempts after the initial failure (total attempts = retries + 1). */
    static final long MAX_RETRY_ATTEMPTS = 3L;

    /** Pause between retry attempts, in milliseconds. */
    static final long RETRY_INTERVAL_MS = 1_000L;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * A minimal {@link KafkaTemplate} backed by {@link ByteArraySerializer} for
     * both key and value. Used exclusively by {@link DeadLetterPublishingRecoverer}
     * to write failed records verbatim to the DLT topic.
     *
     * <p>This bean is intentionally <em>not</em> {@code @Primary} — the main
     * {@link KafkaTemplate} (typed to {@link nh.weather_reader_kafka.model.ProcessedWeatherData})
     * in {@link KafkaProducerConfig} carries that role.
     */
    @Bean
    public KafkaTemplate<Object, Object> dltKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   ByteArraySerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    /**
     * The shared {@link DefaultErrorHandler} wired into the
     * {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory}.
     *
     * <p>Retries are not attempted for {@link SerializationException} — those
     * failures are permanent and should go straight to the DLT.
     */
    @Bean
    public DefaultErrorHandler defaultErrorHandler(KafkaTemplate<Object, Object> dltKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(dltKafkaTemplate);

        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer, new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_ATTEMPTS));

        // Deserialization failures are not transient — skip retries
        handler.addNotRetryableExceptions(SerializationException.class);

        return handler;
    }
}

