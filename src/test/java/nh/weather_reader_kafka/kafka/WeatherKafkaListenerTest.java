package nh.weather_reader_kafka.kafka;

import nh.weather_reader_kafka.service.WeatherConsumerService;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class WeatherKafkaListenerTest {

    @Mock
    private WeatherConsumerService weatherConsumerService;

    @Mock
    private GenericRecord weatherRecord;

    @Mock
    private Schema schema;

    @Test
    void onWeatherMessage_validRecord_delegatesToService() {
        when(weatherRecord.getSchema()).thenReturn(schema);
        when(schema.getName()).thenReturn("WeatherData");

        WeatherKafkaListener listener = new WeatherKafkaListener(weatherConsumerService);
        listener.onWeatherMessage(weatherRecord);

        verify(weatherConsumerService).processWeather(weatherRecord);
    }

    @Test
    void onWeatherMessage_serviceThrows_propagatesExceptionToContainer() {
        // The listener no longer swallows exceptions — it lets them propagate so
        // that the container's DefaultErrorHandler can drive retry and DLT logic.
        when(weatherRecord.getSchema()).thenReturn(schema);
        when(schema.getName()).thenReturn("WeatherData");
        doThrow(new IllegalStateException("boom")).when(weatherConsumerService).processWeather(weatherRecord);

        WeatherKafkaListener listener = new WeatherKafkaListener(weatherConsumerService);

        assertThatThrownBy(() -> listener.onWeatherMessage(weatherRecord))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        verify(weatherConsumerService).processWeather(weatherRecord);
    }

    @Test
    void onWeatherMessage_nullRecord_skipsProcessing() {
        WeatherKafkaListener listener = new WeatherKafkaListener(weatherConsumerService);

        listener.onWeatherMessage(null);

        verifyNoInteractions(weatherConsumerService);
    }
}

