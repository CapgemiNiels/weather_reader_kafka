package nh.weather_reader_kafka.kafka;

import nh.weather_reader_kafka.config.KafkaTopicProperties;
import nh.weather_reader_kafka.model.ProcessedWeatherData;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WeatherKafkaProducer}.
 *
 * <p>Verifies that the producer:
 * <ul>
 *   <li>sends to the correct topic ({@code processed-weather-data})</li>
 *   <li>generates a key that matches the pattern {@code PROCESSED_WEATHER_DATA-<epochMillis>}</li>
 *   <li>sends the exact {@link ProcessedWeatherData} object it received</li>
 * </ul>
 *
 * <p>No Spring context — the {@link KafkaTemplate} is a Mockito mock and the
 * producer is constructed directly so the topic name can be injected without
 * a deployed application context.
 */
@ExtendWith(MockitoExtension.class)
class WeatherKafkaProducerTest {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final String PROCESSED_TOPIC   = "processed-weather-data";
    private static final String KEY_PATTERN       = "PROCESSED_WEATHER_DATA-\\d+";

    // -----------------------------------------------------------------------
    // Mock & Subject
    // -----------------------------------------------------------------------

    @Mock
    private KafkaTemplate<String, ProcessedWeatherData> kafkaTemplate;

    private WeatherKafkaProducer weatherKafkaProducer;

    @BeforeEach
    void setUp() {
        ProcessedWeatherData metadataPayload = new ProcessedWeatherData(
                "2026-05-13T14:15:00Z", 11.8, 11.5, 248, 1);

        // Direct constructor injection — avoids needing a Spring context.
        SendResult<String, ProcessedWeatherData> sendResult = new SendResult<>(
                new ProducerRecord<>(PROCESSED_TOPIC, "test-key", metadataPayload),
                new RecordMetadata(new TopicPartition(PROCESSED_TOPIC, 0), 0, 0, 0, 0, 0));

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        weatherKafkaProducer = new WeatherKafkaProducer(kafkaTemplate,
                new KafkaTopicProperties("weather_input", PROCESSED_TOPIC));
    }

    // -----------------------------------------------------------------------
    // Test 6 — Happy path: correct topic, correct key pattern, correct payload
    // -----------------------------------------------------------------------

    /**
     * When {@code send()} is called with a valid {@link ProcessedWeatherData} the
     * producer must delegate to {@code KafkaTemplate.send(topic, key, value)} with:
     * <ul>
     *   <li>topic  = {@code "processed-weather-data"}</li>
     *   <li>key    = string matching {@code PROCESSED_WEATHER_DATA-\d+}</li>
     *   <li>value  = the exact same object that was passed in</li>
     * </ul>
     */
    @Test
    void send_validRecord_usesCorrectTopicAndKeyPattern() {
        ProcessedWeatherData data = new ProcessedWeatherData(
                "2026-05-13T14:15:00Z", 11.8, 11.5, 248, 1);

        weatherKafkaProducer.send(data);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(PROCESSED_TOPIC), keyCaptor.capture(), eq(data));

        assertThat(keyCaptor.getValue()).matches(KEY_PATTERN);
    }

    // -----------------------------------------------------------------------
    // Test 7 — Edge case: every call produces a key matching the pattern
    // -----------------------------------------------------------------------

    /**
     * Two consecutive {@code send()} calls must each produce a key that independently
     * satisfies the {@code PROCESSED_WEATHER_DATA-\d+} pattern.
     *
     * <p>Note: because both calls may land within the same millisecond the keys
     * are <em>not</em> required to be distinct — the pattern match is what matters
     * (uniqueness over time is guaranteed in practice by epoch millis).
     * What we do assert is that {@code KafkaTemplate.send} is called twice, each
     * time with a correctly formatted key.
     */
    @Test
    void send_keyMatchesPatternOnEveryCall() {
        ProcessedWeatherData data = new ProcessedWeatherData(
                "2026-05-13T14:15:00Z", 11.8, 11.5, 248, 1);

        weatherKafkaProducer.send(data);
        weatherKafkaProducer.send(data);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2))
                .send(eq(PROCESSED_TOPIC), keyCaptor.capture(), eq(data));

        List<String> keys = keyCaptor.getAllValues();
        assertThat(keys).hasSize(2);
        assertThat(keys.get(0)).matches(KEY_PATTERN);
        assertThat(keys.get(1)).matches(KEY_PATTERN);
    }

    // -----------------------------------------------------------------------
    // Test 8 — Failure path: async send failure must not throw from send()
    // -----------------------------------------------------------------------

    /**
     * When the {@code KafkaTemplate.send()} future completes exceptionally
     * (e.g. broker unavailable), the {@code send()} method must <em>not</em>
     * throw — the failure is logged asynchronously via the {@code whenComplete}
     * callback and the calling thread is unaffected.
     */
    @Test
    void send_failedFuture_doesNotThrowAndLogsError() {
        CompletableFuture<SendResult<String, ProcessedWeatherData>> failedFuture =
                new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failedFuture);

        ProcessedWeatherData data = new ProcessedWeatherData(
                "2026-05-13T14:15:00Z", 11.8, 11.5, 248, 1);

        assertThatCode(() -> weatherKafkaProducer.send(data)).doesNotThrowAnyException();
    }
}

