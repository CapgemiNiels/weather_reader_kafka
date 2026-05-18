package nh.weather_reader_kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point for the weather reader Kafka Spring Boot application. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class WeatherReaderKafkaApplication {

    /** Creates the application bootstrap class. */
    public WeatherReaderKafkaApplication() {
    }

    /**
     * Starts the Spring Boot application.
     *
     * @param args startup arguments passed from the command line
     */
    public static void main(String[] args) {
        SpringApplication.run(WeatherReaderKafkaApplication.class, args);
    }

}
