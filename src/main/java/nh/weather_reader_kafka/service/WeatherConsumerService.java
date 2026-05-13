package nh.weather_reader_kafka.service;

import org.apache.avro.generic.GenericRecord;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Business logic service for weather data processing.
 * Handles the processing and logic for weather messages received from Kafka.
 */
@Service
public class WeatherConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherConsumerService.class);

    /**
     * Processes weather data from the Kafka message.
     * This method contains the business logic for handling weather records.
     *
     * @param weatherRecord GenericRecord containing the weather data
     */
    public void processWeather(GenericRecord weatherRecord) {
        logger.info("Processing weather data");

        // Extract and log all fields from the Avro record
        weatherRecord.getSchema().getFields().forEach(field -> {
            Object value = weatherRecord.get(field.name());
            logger.debug("Field: {} = {}", field.name(), value);
        });

        // Add your business logic here
        // Example use cases:
        // - Save to database
        // - Perform calculations and analytics
        // - Trigger alerts
        // - Validate data
        // - Transform and publish to other systems

        executeBusinessLogic(weatherRecord);
    }

    private void executeBusinessLogic(GenericRecord weatherRecord) {
        // TODO: Implement business logic for weather data
        logger.debug("Executing business logic for weather record: {}", weatherRecord);
    }
}

