package nh.weather_reader_kafka.kafka;

import nh.weather_reader_kafka.config.KafkaTopicProperties;
import nh.weather_reader_kafka.model.ProcessedWeatherData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka producer that publishes {@link ProcessedWeatherData} records as JSON
 * to the {@code processed-weather-data} topic.
 *
 * <p>Each message key follows the pattern {@code PROCESSED_WEATHER_DATA-<epochMillis>}.
 * This key provides <em>partitioning context</em> (all processed records share
 * the same prefix) but does <strong>not</strong> guarantee uniqueness — two
 * calls within the same millisecond will produce identical keys. If true
 * uniqueness is required, replace the key with {@code UUID.randomUUID().toString()}.
 */
@Component
public class WeatherKafkaProducer {

    private static final Logger logger =
            LoggerFactory.getLogger(WeatherKafkaProducer.class);

    /** Prefix used when building the per-message key. */
    private static final String KEY_PREFIX = "PROCESSED_WEATHER_DATA";

    private final KafkaTemplate<String, ProcessedWeatherData> kafkaTemplate;
    private final String processedTopicName;

    /**
     * @param kafkaTemplate   template configured to serialize values as JSON
     * @param topicProperties typed configuration carrying the target topic name
     */
    public WeatherKafkaProducer(
            KafkaTemplate<String, ProcessedWeatherData> kafkaTemplate,
            KafkaTopicProperties topicProperties) {
        this.kafkaTemplate       = kafkaTemplate;
        this.processedTopicName  = topicProperties.processedName();
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
        kafkaTemplate.send(processedTopicName, messageKey, data)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to publish weather message with key '{}'", messageKey, ex);
                        return;
                    }

                    logger.debug("Published weather message to topic '{}' partition {} offset {}",
                            processedTopicName,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}
