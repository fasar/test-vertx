package test.vertx.tsdb;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TsDbLabel {
    String key;
    String value;
}
