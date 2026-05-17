package nh.weather_reader_kafka.service;

import nh.weather_reader_kafka.kafka.WeatherKafkaProducer;
import nh.weather_reader_kafka.model.ProcessedWeatherData;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Business-logic service for incoming weather data.
 *
 * <p>Extracts five fields from the raw Avro {@link GenericRecord} received from
 * the {@code weather_input} topic, maps them to a {@link ProcessedWeatherData}
 * record (applying camelCase renames), and hands the result to
 * {@link WeatherKafkaProducer} for forwarding to {@code processed-weather-data}.
 *
 * <p>Null/absent fields are passed through as {@code null} — the downstream
 * consumer decides how to handle them.
 */
@Service
public class WeatherConsumerService {

    private static final Logger logger =
            LoggerFactory.getLogger(WeatherConsumerService.class);

    private final WeatherKafkaProducer weatherKafkaProducer;

    public WeatherConsumerService(WeatherKafkaProducer weatherKafkaProducer) {
        this.weatherKafkaProducer = weatherKafkaProducer;
    }

    /**
     * Processes one weather record from Kafka.
     *
     * <p>Field mapping (Avro source → Java camelCase):
     * <pre>
     *   time          → time          (String)
     *   temperature   → temperature   (Double)
     *   windspeed     → windSpeed     (Double)
     *   winddirection → windDirection (Integer)
     *   is_day        → isDay         (Integer)
     * </pre>
     *
     * @param weatherRecord raw Avro {@link GenericRecord} from {@code weather_input}
     */
    public void processWeather(GenericRecord weatherRecord) {
        if (weatherRecord == null) {
            logger.warn("Received null weather record; skipping processing");
            return;
        }

        logger.info("Processing weather record from schema: {}",
                weatherRecord.getSchema().getName());

        // Extract fields — Avro may return CharSequence (Utf8) for strings;
        // toString() normalises to java.lang.String safely.
        String  time          = extractString(weatherRecord, "time");
        Double  temperature   = extractDouble(weatherRecord, "temperature");
        Double  windSpeed     = extractDouble(weatherRecord, "windspeed");     // windspeed  → windSpeed
        Integer windDirection = extractInt(weatherRecord,    "winddirection"); // winddirection → windDirection
        Integer isDay         = extractInt(weatherRecord,    "is_day");        // is_day → isDay

        ProcessedWeatherData processed =
                new ProcessedWeatherData(time, temperature, windSpeed, windDirection, isDay);

        logger.info("Forwarding processed weather: {}", processed);
        weatherKafkaProducer.send(processed);
    }

    // -----------------------------------------------------------------------
    // Private helpers — null-safe extraction from GenericRecord
    // -----------------------------------------------------------------------

    /**
     * Returns the field value as a {@link String}, or {@code null} if absent.
     * Handles both {@code java.lang.String} and Avro's {@code Utf8}.
     */
    private String extractString(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return value != null ? value.toString() : null;
    }

    /**
     * Returns the field value as a {@link Double}, or {@code null} if absent.
     * Handles any {@link Number} subtype returned by the Avro deserialiser.
     */
    private Double extractDouble(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return value instanceof Number n ? n.doubleValue() : null;
    }

    /**
     * Returns the field value as an {@link Integer}, or {@code null} if absent.
     * Handles any {@link Number} subtype returned by the Avro deserialiser.
     */
    private Integer extractInt(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return value instanceof Number n ? n.intValue() : null;
    }
}
