package nh.weather_reader_kafka.kafka;

import nh.weather_reader_kafka.service.WeatherConsumerService;
import org.apache.avro.generic.GenericRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka listener for the weather topic.
 *
 * <p>Receives messages from the {@code weather_input} topic and delegates to
 * {@link WeatherConsumerService} for business logic processing.
 *
 * <p>Exceptions are intentionally <strong>not</strong> swallowed here. Any
 * uncaught exception propagates to the container's {@link
 * org.springframework.kafka.listener.DefaultErrorHandler}, which will retry
 * the record (with back-off) and route it to the dead-letter topic after
 * exhausting retries.
 */
@Component
public class WeatherKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(WeatherKafkaListener.class);

    private final WeatherConsumerService weatherConsumerService;

    /**
     * Creates the listener with its delegated processing service.
     *
     * @param weatherConsumerService service that processes incoming messages
     */
    public WeatherKafkaListener(WeatherConsumerService weatherConsumerService) {
        this.weatherConsumerService = weatherConsumerService;
    }

    /**
     * Listens to the weather_input Kafka topic.
     *
     * <p>The schema is automatically fetched from the Schema Registry based on
     * the message's embedded schema ID. Delegates to the service layer for
     * business logic processing.
     *
     * @param weatherRecord GenericRecord containing the Avro message from the topic
     */
    @KafkaListener(topics = "${kafka.topic.name}", groupId = "${spring.kafka.consumer.group-id}")
    public void onWeatherMessage(GenericRecord weatherRecord) {
        if (weatherRecord == null) {
            logger.warn("Received null weather message; skipping processing");
            return;
        }

        logger.info("Received weather message with schema: {}", weatherRecord.getSchema().getName());

        // No try/catch — failures propagate to the container DefaultErrorHandler
        // for retry and dead-letter routing (see KafkaErrorHandlerConfig).
        weatherConsumerService.processWeather(weatherRecord);
    }
}
