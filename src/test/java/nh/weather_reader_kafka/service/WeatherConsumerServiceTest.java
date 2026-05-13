package nh.weather_reader_kafka.service;

import nh.weather_reader_kafka.kafka.WeatherKafkaProducer;
import nh.weather_reader_kafka.model.ProcessedWeatherData;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link WeatherConsumerService}.
 *
 * <p>Verifies that the service correctly extracts the five target fields from an
 * Avro {@link GenericRecord}, maps them to camelCase in a {@link ProcessedWeatherData}
 * record, and hands the result off to {@link WeatherKafkaProducer}.
 *
 * <p>No Spring context is started — all collaborators are Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
class WeatherConsumerServiceTest {

    // -----------------------------------------------------------------------
    // Mocks & Subject
    // -----------------------------------------------------------------------

    @Mock
    private WeatherKafkaProducer weatherKafkaProducer;

    @InjectMocks
    private WeatherConsumerService weatherConsumerService;

    // -----------------------------------------------------------------------
    // Test schema  — mirrors the fields in incoming-data-from-kafka.json.
    // All fields are optional (union["null", type]) so null values can be set
    // without Avro validation errors.
    // -----------------------------------------------------------------------

    private static final Schema WEATHER_SCHEMA = SchemaBuilder.record("WeatherData")
            .namespace("test")
            .fields()
            .optionalString("time")
            .optionalDouble("temperature")
            .optionalDouble("windspeed")
            .optionalInt("winddirection")
            .optionalInt("is_day")
            .optionalInt("interval")
            .optionalInt("weathercode")
            .endRecord();

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Builds a {@link GenericRecord} using the test schema with all 7 source fields.
     * Pass {@code null} to simulate a missing/absent value for any field.
     */
    private GenericRecord buildRecord(String time,
                                      Double temperature,
                                      Double windspeed,
                                      Integer winddirection,
                                      Integer isDay,
                                      Integer interval,
                                      Integer weathercode) {
        GenericRecord record = new GenericData.Record(WEATHER_SCHEMA);
        record.put("time",         time);
        record.put("temperature",  temperature);
        record.put("windspeed",    windspeed);
        record.put("winddirection", winddirection);
        record.put("is_day",       isDay);
        record.put("interval",     interval);
        record.put("weathercode",  weathercode);
        return record;
    }

    // -----------------------------------------------------------------------
    // Test 1 — Happy path: all fields present
    // -----------------------------------------------------------------------

    /**
     * Given a fully populated GenericRecord, the service must build a
     * ProcessedWeatherData with all five fields correctly mapped (including
     * the camelCase rename windspeed→windSpeed, winddirection→windDirection,
     * is_day→isDay) and call the producer exactly once.
     */
    @Test
    void processWeather_allFieldsPresent_buildsRecordAndCallsProducer() {
        GenericRecord input = buildRecord(
                "2026-05-13T14:15:00Z", 11.8, 11.5, 248, 1, 900, 61);

        weatherConsumerService.processWeather(input);

        ArgumentCaptor<ProcessedWeatherData> captor =
                ArgumentCaptor.forClass(ProcessedWeatherData.class);
        verify(weatherKafkaProducer).send(captor.capture());

        ProcessedWeatherData result = captor.getValue();
        assertThat(result.time())          .isEqualTo("2026-05-13T14:15:00Z");
        assertThat(result.temperature())   .isEqualTo(11.8);
        assertThat(result.windSpeed())     .isEqualTo(11.5);
        assertThat(result.windDirection()) .isEqualTo(248);
        assertThat(result.isDay())         .isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // Test 2 — Null field: temperature
    // -----------------------------------------------------------------------

    /**
     * When temperature is null in the incoming record the service must still
     * call the producer (publish-with-null policy) and the resulting record's
     * temperature() must be null — not zero or some default.
     */
    @Test
    void processWeather_nullTemperature_publishesWithNullTemperature() {
        GenericRecord input = buildRecord(
                "2026-05-13T14:15:00Z", null, 11.5, 248, 1, 900, 61);

        weatherConsumerService.processWeather(input);

        ArgumentCaptor<ProcessedWeatherData> captor =
                ArgumentCaptor.forClass(ProcessedWeatherData.class);
        verify(weatherKafkaProducer).send(captor.capture());

        assertThat(captor.getValue().temperature()).isNull();
    }

    // -----------------------------------------------------------------------
    // Test 3 — Null field: winddirection
    // -----------------------------------------------------------------------

    /**
     * When winddirection is null the mapped windDirection() must also be null.
     * The producer must be called exactly once regardless, confirming the
     * null-pass-through contract.
     */
    @Test
    void processWeather_nullWindDirection_publishesWithNullWindDirection() {
        GenericRecord input = buildRecord(
                "2026-05-13T14:15:00Z", 11.8, 11.5, null, 1, 900, 61);

        weatherConsumerService.processWeather(input);

        ArgumentCaptor<ProcessedWeatherData> captor =
                ArgumentCaptor.forClass(ProcessedWeatherData.class);
        verify(weatherKafkaProducer).send(captor.capture());

        assertThat(captor.getValue().windDirection()).isNull();
    }

    // -----------------------------------------------------------------------
    // Test 4 — Edge case: all five target fields null
    // -----------------------------------------------------------------------

    /**
     * Extreme edge case: every field the service cares about is null.
     * The service must still call the producer once (never silently drop)
     * and all five fields in the resulting record must be null.
     * The irrelevant fields (interval, weathercode) are populated to confirm
     * they are not accidentally read instead.
     */
    @Test
    void processWeather_allFieldsNull_publishesAllNulls() {
        GenericRecord input = buildRecord(null, null, null, null, null, 900, 61);

        weatherConsumerService.processWeather(input);

        ArgumentCaptor<ProcessedWeatherData> captor =
                ArgumentCaptor.forClass(ProcessedWeatherData.class);
        verify(weatherKafkaProducer).send(captor.capture());

        ProcessedWeatherData result = captor.getValue();
        assertThat(result.time())          .isNull();
        assertThat(result.temperature())   .isNull();
        assertThat(result.windSpeed())     .isNull();
        assertThat(result.windDirection()) .isNull();
        assertThat(result.isDay())         .isNull();
    }

    // -----------------------------------------------------------------------
    // Test 5 — Contract: extra fields (interval, weathercode) must be ignored
    // -----------------------------------------------------------------------

    /**
     * The source record contains 7 fields; the output record must only carry 5.
     * This test asserts the structural contract: ProcessedWeatherData has exactly
     * 5 record components (compile-time guarantee via Java records), and the
     * values of the two extra fields (interval=999, weathercode=99) never leak
     * into the output.
     */
    @Test
    void processWeather_extraFieldsIgnored_onlyFiveFieldsMapped() {
        // Use recognisable sentinel values for the extra fields
        GenericRecord input = buildRecord(
                "2026-05-13T14:15:00Z", 11.8, 11.5, 248, 1,
                /* interval */ 999,
                /* weathercode */ 99);

        weatherConsumerService.processWeather(input);

        ArgumentCaptor<ProcessedWeatherData> captor =
                ArgumentCaptor.forClass(ProcessedWeatherData.class);
        verify(weatherKafkaProducer).send(captor.capture());

        ProcessedWeatherData result = captor.getValue();

        // Structural check: the record type exposes exactly 5 components
        assertThat(ProcessedWeatherData.class.getRecordComponents()).hasSize(5);

        // Value check: the five mapped fields are correct
        assertThat(result.time())          .isEqualTo("2026-05-13T14:15:00Z");
        assertThat(result.temperature())   .isEqualTo(11.8);
        assertThat(result.windSpeed())     .isEqualTo(11.5);
        assertThat(result.windDirection()) .isEqualTo(248);
        assertThat(result.isDay())         .isEqualTo(1);
    }
}

