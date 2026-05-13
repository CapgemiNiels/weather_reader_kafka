package nh.weather_reader_kafka.kafka;

import nh.weather_reader_kafka.model.ProcessedWeatherData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka producer that publishes {@link ProcessedWeatherData} records as JSON
 * to the {@code processed-weather-data} topic.
 *
 * <p>Each message key follows the pattern {@code PROCESSED_WEATHER_DATA-<epochMillis>},
 * mirroring the convention used on the upstream producer side and guaranteeing
 * practical uniqueness across messages.
 */
@Component
public class WeatherKafkaProducer {

    private static final Logger logger =
            LoggerFactory.getLogger(WeatherKafkaProducer.class);

    /** Prefix used when building the per-message key. */
    private static final String KEY_PREFIX = "processed-weather-data";

    private final KafkaTemplate<String, ProcessedWeatherData> kafkaTemplate;
    private final String processedTopicName;

    /**
     * @param kafkaTemplate      template configured with {@link org.springframework.kafka.support.serializer.JsonSerializer}
     * @param processedTopicName target topic name, bound from {@code kafka.topic.processed-name}
     */
    public WeatherKafkaProducer(
            KafkaTemplate<String, ProcessedWeatherData> kafkaTemplate,
            @Value("${kafka.topic.processed-name}") String processedTopicName) {
        this.kafkaTemplate       = kafkaTemplate;
        this.processedTopicName  = processedTopicName;
    }

    /**
     * Sends {@code data} to the processed-weather-data Kafka topic.
     *
     * <p>The message key is generated at call time as
     * {@code PROCESSED_WEATHER_DATA-<current epoch millis>}.
     *
     * @param data the processed weather record to publish
     */
    public void send(ProcessedWeatherData data) {
        String messageKey = KEY_PREFIX + "-" + Instant.now().toEpochMilli();
        logger.info("Publishing to topic '{}' with key '{}'", processedTopicName, messageKey);
        kafkaTemplate.send(processedTopicName, messageKey, data);
    }
}

