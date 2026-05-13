package nh.weather_reader_kafka.config;

import nh.weather_reader_kafka.model.ProcessedWeatherData;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Kafka producer configuration.
 *
 * <p>Produces {@link ProcessedWeatherData} records serialised as JSON to the
 * {@code processed-weather-data} topic. The key is always a plain
 * {@link String}; {@link JsonSerializer} is used for the value so that the
 * output is human-readable and Avro-independent.
 *
 * <p>Type-info headers are disabled ({@code ADD_TYPE_INFO_HEADERS = false}) so
 * that the JSON payloads can be consumed by non-Spring listeners without
 * needing to understand Spring's type header convention.
 */
@Configuration
@SuppressWarnings("deprecation") // JsonSerializer — will migrate when Spring Kafka ships a stable replacement
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Creates the producer factory wired to the shared Kafka broker.
     *
     * @return a {@link DefaultKafkaProducerFactory} for String keys and
     *         {@link ProcessedWeatherData} JSON values
     */
    @Bean
    public ProducerFactory<String, ProcessedWeatherData> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Omit Spring type headers so the JSON payload is clean for any consumer
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Creates the {@link KafkaTemplate} used by
     * {@link nh.weather_reader_kafka.kafka.WeatherKafkaProducer}.
     *
     * @return a {@link KafkaTemplate} backed by {@link #producerFactory()}
     */
    @Bean
    public KafkaTemplate<String, ProcessedWeatherData> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}


