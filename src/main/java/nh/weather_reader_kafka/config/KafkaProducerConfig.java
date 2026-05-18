package nh.weather_reader_kafka.config;

import nh.weather_reader_kafka.model.ProcessedWeatherData;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Kafka producer configuration.
 *
 * <p>Produces {@link ProcessedWeatherData} records serialised as JSON to the
 * {@code processed-weather-data} topic. The key is a plain {@link String};
 * the value is serialized by {@link ProcessedWeatherDataSerializer}, a custom
 * Jackson-based {@link org.apache.kafka.common.serialization.Serializer} that
 * carries no deprecated API.
 */
@Configuration
public class KafkaProducerConfig {

    /** Creates a Kafka producer configuration bean container. */
    public KafkaProducerConfig() {
    }

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
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,      bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ProcessedWeatherDataSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Creates the {@link KafkaTemplate} used by
     * {@link nh.weather_reader_kafka.kafka.WeatherKafkaProducer}.
     *
     * <p>Marked {@code @Primary} so that Spring resolves this bean when
     * multiple {@link KafkaTemplate} beans are present (e.g. the DLT template
     * in {@link KafkaErrorHandlerConfig}).
     *
     * @return a {@link KafkaTemplate} backed by {@link #producerFactory()}
     */
    @Bean
    @Primary
    public KafkaTemplate<String, ProcessedWeatherData> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
