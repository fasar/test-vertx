package test.vertx.tsdb;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class TsDbRecord {
    String signalName;
    List<TsDbLabel> labels;
    Instant startInstant;
    Duration stepDuration;
    TsDbUnit unit;
    List<String> data;
}
