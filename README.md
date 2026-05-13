# Weather Reader Kafka

weather app based off youtube series on Kafka from the Quix channel
https://www.youtube.com/watch?v=eCsSAzTy5cE

---

## Data flow

```
Kafka topic: weather_input  (Avro / Confluent Schema Registry)
        │
        ▼  WeatherKafkaListener  →  WeatherConsumerService
        │
        │  Extracts 5 fields from the GenericRecord:
        │    time, temperature, windSpeed, windDirection, isDay
        │
        ▼  WeatherKafkaProducer
        │
Kafka topic: processed-weather-data  (JSON)
```

### Topics

| Topic | Format | Direction |
|---|---|---|
| `weather_input` | Avro (Schema Registry) | **consumed** |
| `processed-weather-data` | JSON | **produced** |

### Message key pattern

Messages published to `processed-weather-data` use a timestamped key:
```
PROCESSED_WEATHER_DATA-<epoch-millis>
```

### Processed payload example

```json
{
  "time": "2026-05-13T14:15:00Z",
  "temperature": 11.8,
  "windSpeed": 11.5,
  "windDirection": 248,
  "isDay": 1
}
```

Fields `interval` and `weathercode` from the source schema are intentionally dropped.
Null values in the source are forwarded as JSON `null`.

---

## Configuration

Key properties in `application.yml`:

| Property | Default | Description |
|---|---|---|
| `spring.kafka.bootstrap-servers` | `127.0.0.1:9092` | Kafka broker |
| `spring.kafka.consumer.properties.schema.registry.url` | `http://127.0.0.1:8081` | Confluent Schema Registry |
| `kafka.topic.name` | `weather_input` | Source topic |
| `kafka.topic.processed-name` | `processed-weather-data` | Output topic |
| `server.port` | `8090` | HTTP port |

---

## Running

```bash
./mvnw spring-boot:run
```

## Tests

```bash
./mvnw verify   # runs all unit tests + JaCoCo coverage gates (≥80% line / ≥70% branch)
```
