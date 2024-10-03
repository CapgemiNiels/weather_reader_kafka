package nh.weather_reader_kafka.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class CustomKafkaErrorHandler extends DefaultErrorHandler {


    @Override
    public boolean handleOne(@NonNull Exception thrownException, @NonNull ConsumerRecord<?, ?> record, @NonNull Consumer<?, ?> consumer, @NonNull MessageListenerContainer container) {
        // Log the error or perform any custom error handling logic
        System.err.println("Error in process with Exception {} and the record is {} " + thrownException + record);
        return super.handleOne(thrownException, record, consumer, container);
    }
}