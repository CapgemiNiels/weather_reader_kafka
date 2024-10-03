package nh.weather_reader_kafka.service;

import nh.weather_reader_kafka.model.CurrentWeather;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class WeatherListenerService {

    @KafkaListener(topics = "${kafka.topic.name}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(CurrentWeather currentWeather) {
        System.out.println("Received currentWeather: " + currentWeather);
    }
}
