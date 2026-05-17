package nh.weather_reader_kafka.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nh.weather_reader_kafka.model.ProcessedWeatherData;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Custom Kafka value serializer for {@link ProcessedWeatherData}.
 *
 * <p>Replaces Spring Kafka's deprecated {@code JsonSerializer} so that the
 * producer configuration carries no deprecated API and the global
 * {@code -Xlint:-deprecation} compiler flag can be removed.
 *
 * <p>Jackson's {@link ObjectMapper} is thread-safe; a single instance is
 * shared across all serialization calls on this instance.
 *
 * <p>Kafka instantiates this class via reflection (no-arg constructor) when it
 * is registered as {@code VALUE_SERIALIZER_CLASS_CONFIG}. No Spring context
 * is required.
 */
public class ProcessedWeatherDataSerializer implements Serializer<ProcessedWeatherData> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Serializes {@code data} to its JSON byte representation.
     *
     * @param topic the target topic (unused but required by the interface)
     * @param data  the record to serialize; {@code null} produces {@code null}
     * @return UTF-8 encoded JSON bytes, or {@code null} if {@code data} is {@code null}
     * @throws SerializationException if Jackson cannot serialize the object
     */
    @Override
    public byte[] serialize(String topic, ProcessedWeatherData data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException(
                    "Failed to serialize ProcessedWeatherData to JSON", e);
        }
    }
}

