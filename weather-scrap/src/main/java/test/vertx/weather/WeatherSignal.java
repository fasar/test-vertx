package test.vertx.weather;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Value
@AllArgsConstructor
public class WeatherSignal {
    public String observationName; // The name of the Signal. Can be Temperature, Dew Point, Wind, Wind Speed, Wind Gust, Pressure, Condition
    public Instant startInstant;
    public Duration step;
    public List<String> data;
}
