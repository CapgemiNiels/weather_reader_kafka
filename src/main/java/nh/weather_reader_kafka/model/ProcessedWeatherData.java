package nh.weather_reader_kafka.model;

/**
 * Immutable value object carrying the five weather fields extracted from
 * the incoming {@code weather_input} Avro record.
 *
 * <p>Field names follow Java camelCase convention; they are mapped from their
 * snake_case / single-word Avro counterparts in
 * {@link nh.weather_reader_kafka.service.WeatherConsumerService}:
 * <ul>
 *   <li>{@code windspeed}    → {@code windSpeed}</li>
 *   <li>{@code winddirection}→ {@code windDirection}</li>
 *   <li>{@code is_day}       → {@code isDay}</li>
 * </ul>
 *
 * <p>All fields are nullable; a {@code null} value means the field was absent
 * or null in the source record.
 */
public record ProcessedWeatherData(
        String  time,
        Double  temperature,
        Double  windSpeed,
        Integer windDirection,
        Integer isDay
) {}

