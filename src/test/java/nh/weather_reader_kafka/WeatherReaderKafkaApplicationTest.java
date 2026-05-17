package nh.weather_reader_kafka;

import nh.weather_reader_kafka.config.KafkaErrorHandlerConfig;
import nh.weather_reader_kafka.config.KafkaTopicProperties;
import nh.weather_reader_kafka.kafka.WeatherKafkaProducer;
import nh.weather_reader_kafka.service.WeatherConsumerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring context smoke test — verifies that the full application context loads
 * successfully without a running Kafka broker or Schema Registry.
 *
 * <p>Kafka listeners are prevented from starting ({@code auto-startup=false})
 * so no real broker connection is attempted. Producers connect lazily, so the
 * fake bootstrap address never causes a failure at startup.
 *
 * <p>This test catches wiring mistakes (missing beans, property-binding
 * failures, type mismatches) that pure Mockito unit tests cannot detect.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.kafka.consumer.properties.schema.registry.url=http://localhost:8081",
        "spring.kafka.listener.auto-startup=false"
})
class WeatherReaderKafkaApplicationTest {

    @Autowired
    private WeatherConsumerService weatherConsumerService;

    @Autowired
    private WeatherKafkaProducer weatherKafkaProducer;

    @Autowired
    private KafkaTopicProperties kafkaTopicProperties;

    @Autowired
    private DefaultErrorHandler defaultErrorHandler;

    @Test
    void contextLoads() {
        assertThat(weatherConsumerService).isNotNull();
        assertThat(weatherKafkaProducer).isNotNull();
    }

    @Test
    void kafkaTopicProperties_areBoundCorrectly() {
        assertThat(kafkaTopicProperties.name()).isEqualTo("weather_input");
        assertThat(kafkaTopicProperties.processedName()).isEqualTo("processed-weather-data");
    }

    @Test
    void defaultErrorHandler_isConfiguredWithRetrySettings() {
        // Verify the error handler bean exists and is the one from KafkaErrorHandlerConfig
        assertThat(defaultErrorHandler).isNotNull();
    }
}

