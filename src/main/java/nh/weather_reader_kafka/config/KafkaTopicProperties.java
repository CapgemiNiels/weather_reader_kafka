package nh.weather_reader_kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the custom {@code kafka.topic.*} properties.
 *
 * <p>Replaces the individual {@code @Value} injections for topic names, giving
 * compile-time safety, IDE completion and validateable configuration.
 *
 * <p>Bound via {@code @ConfigurationPropertiesScan} on
 * {@link nh.weather_reader_kafka.WeatherReaderKafkaApplication}.
 *
 * <pre>
 * # application.yml
 * kafka:
 *   topic:
 *     name: weather_input
 *     processed-name: processed-weather-data
 * </pre>
 *
 * @param name source topic consumed by listeners
 * @param processedName destination topic used for processed messages
 */
@ConfigurationProperties(prefix = "kafka.topic")
public record KafkaTopicProperties(

        /** Source topic consumed by {@link nh.weather_reader_kafka.kafka.WeatherKafkaListener}. */
        String name,

        /**
         * Target topic published to by
         * {@link nh.weather_reader_kafka.kafka.WeatherKafkaProducer}.
         * Bound from the kebab-case property {@code kafka.topic.processed-name}.
         */
        String processedName
) {}
