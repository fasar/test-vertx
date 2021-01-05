package test.vertx.weather;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Value
@AllArgsConstructor
public class WeatherSignal {
    public String name;
    public Instant startInstant;
    public Duration step;
    public List<String> data;
}
