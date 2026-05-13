package nh.weather_reader_kafka.kafka;

import nh.weather_reader_kafka.service.WeatherConsumerService;
import org.apache.avro.generic.GenericRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka listener for the weather topic.
 * Responsible for receiving messages from Kafka and delegating to the business logic service.
 */
@Component
public class WeatherKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(WeatherKafkaListener.class);

    private final WeatherConsumerService weatherConsumerService;

    public WeatherKafkaListener(WeatherConsumerService weatherConsumerService) {
        this.weatherConsumerService = weatherConsumerService;
    }

    /**
     * Listens to the weather_input Kafka topic.
     * The schema is automatically fetched from the Schema Registry based on the message's schema ID.
     * Delegates to the service layer for business logic processing.
     *
     * @param weatherRecord GenericRecord containing the Avro message from the topic
     */
    @KafkaListener(topics = "${kafka.topic.name}", groupId = "weather-reader-group")
    public void onWeatherMessage(GenericRecord weatherRecord) {
        logger.info("Received weather message with schema: {}", weatherRecord.getSchema().getName());

        try {
            // Delegate to business logic service
            weatherConsumerService.processWeather(weatherRecord);
        } catch (Exception e) {
            logger.error("Error handling weather message", e);
        }
    }
}

